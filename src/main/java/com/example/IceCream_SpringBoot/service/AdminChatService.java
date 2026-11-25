package com.example.IceCream_SpringBoot.service;

import com.example.IceCream_SpringBoot.dto.AdminChatResponse;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminChatService {

    private final ChatClient chatClient;
    private final MongoTemplate mongoTemplate;

    // Colecciones válidas permitidas para consulta
    private static final Set<String> ALLOWED_COLLECTIONS = Set.of(
            "ventas",
            "helados",
            "heladosEliminados",
            "cliente"
    );

    @Autowired
    public AdminChatService(OpenAiChatModel openAiChatModel, MongoTemplate mongoTemplate) {
        // Creamos un ChatClient dedicado para el administrador
        this.chatClient = ChatClient.builder(openAiChatModel).build();
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Procesa la pregunta del administrador de forma segura y devuelve la respuesta.
     */
    public AdminChatResponse procesarMensajeAdmin(String userMessage) {
        // 1. Guardrail semántico inicial sobre la entrada del usuario
        if (!esEntradaSegura(userMessage)) {
            return new AdminChatResponse(
                    "No tengo permisos para modificar datos. Solo puedo realizar consultas.",
                    "BLOQUEADO POR POLÍTICA DE SEGURIDAD (Intento de modificación detectado en la entrada)",
                    false
            );
        }

        try {
            // 2. Traducir la pregunta en una consulta estructurada de MongoDB en formato JSON
            String jsonQueryString = generarConsultaEstructurada(userMessage);
            String cleanedJson = cleanJsonString(jsonQueryString);

            // Intentar parsear el JSON generado
            Document parsedResponse;
            try {
                parsedResponse = Document.parse(cleanedJson);
            } catch (Exception e) {
                // Fallback en caso de que el LLM devuelva un JSON mal estructurado
                return new AdminChatResponse(
                        "Lo siento, tuve un problema al procesar y estructurar la consulta a la base de datos. ¿Podrías replantear la pregunta de otra manera?",
                        "ERROR DE PARSEO: " + cleanedJson,
                        true
                );
            }

            boolean safe = parsedResponse.getBoolean("safe", true);
            boolean requiresDb = parsedResponse.getBoolean("requiresDb", false);
            String rejectionReason = parsedResponse.getString("rejectionReason");

            // Si el LLM detectó que es una solicitud no permitida (de escritura)
            if (!safe) {
                return new AdminChatResponse(
                        rejectionReason != null ? rejectionReason : "No tengo permisos para modificar datos. Solo puedo realizar consultas.",
                        "BLOQUEADO POR POLÍTICA DE SEGURIDAD (Rechazado por el analizador LLM)",
                        false
                );
            }

            // Si es una conversación general que no requiere base de datos
            if (!requiresDb) {
                String respuestaDirecta = chatClient.prompt()
                        .system("Eres el asistente administrativo inteligente de la heladería 'Ice Cream'. Responde al saludo o pregunta de forma amable, profesional y en español.")
                        .user(userMessage)
                        .call()
                        .content();
                return new AdminChatResponse(respuestaDirecta, "Ninguna (Conversación general)", true);
            }

            // Validar parámetros del query
            String collection = parsedResponse.getString("collection");
            String operation = parsedResponse.getString("operation");

            if (collection == null || !ALLOWED_COLLECTIONS.contains(collection.trim())) {
                return new AdminChatResponse(
                        "Lo siento, no tengo permisos para acceder a la colección solicitada o la colección no existe.",
                        "BLOQUEADO: Colección no permitida (" + collection + ")",
                        false
                );
            }

            // 3. Ejecutar la consulta de solo lectura de forma segura en MongoDB
            String queryDetails = "";
            List<Document> queryResults = new ArrayList<>();

            if ("listCollections".equalsIgnoreCase(operation)) {
                queryDetails = "db.getCollectionNames()";
                for (String name : mongoTemplate.getCollectionNames()) {
                    if (ALLOWED_COLLECTIONS.contains(name)) {
                        queryResults.add(new Document("name", name));
                    }
                }
            } else if ("count".equalsIgnoreCase(operation)) {
                Document queryFilter = (Document) parsedResponse.get("query");
                if (queryFilter == null) queryFilter = new Document();
                
                // Doble chequeo de seguridad en los filtros
                if (!esFiltroSeguro(queryFilter.toJson())) {
                    return new AdminChatResponse(
                            "No tengo permisos para modificar datos. Solo puedo realizar consultas.",
                            "BLOQUEADO: Filtro de consulta sospechoso detectado",
                            false
                    );
                }

                queryDetails = "db." + collection + ".countDocuments(" + queryFilter.toJson() + ")";
                long count = mongoTemplate.getCollection(collection).countDocuments(queryFilter);
                queryResults.add(new Document("count", count));
            } else if ("find".equalsIgnoreCase(operation)) {
                Document queryFilter = (Document) parsedResponse.get("query");
                if (queryFilter == null) queryFilter = new Document();
                
                Document projection = (Document) parsedResponse.get("projection");
                Document sort = (Document) parsedResponse.get("sort");
                Integer limit = parsedResponse.getInteger("limit");

                // Doble chequeo de seguridad
                if (!esFiltroSeguro(queryFilter.toJson()) || (projection != null && !esFiltroSeguro(projection.toJson())) || (sort != null && !esFiltroSeguro(sort.toJson()))) {
                    return new AdminChatResponse(
                            "No tengo permisos para modificar datos. Solo puedo realizar consultas.",
                            "BLOQUEADO: Filtro, proyección o sort sospechoso detectado",
                            false
                    );
                }

                // Generar detalle de consulta para mostrar
                StringBuilder detailsBuilder = new StringBuilder("db.").append(collection).append(".find(").append(queryFilter.toJson()).append(")");
                if (projection != null) detailsBuilder.append(".project(").append(projection.toJson()).append(")");
                if (sort != null) detailsBuilder.append(".sort(").append(sort.toJson()).append(")");
                if (limit != null && limit > 0) {
                    detailsBuilder.append(".limit(").append(Math.min(limit, 100)).append(")");
                } else {
                    detailsBuilder.append(".limit(100)");
                }
                queryDetails = detailsBuilder.toString();

                // Ejecución
                var findIterable = mongoTemplate.getCollection(collection).find(queryFilter);
                if (projection != null) findIterable.projection(projection);
                if (sort != null) findIterable.sort(sort);
                
                int finalLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 100;
                findIterable.limit(finalLimit);
                findIterable.into(queryResults);
            } else if ("aggregate".equalsIgnoreCase(operation)) {
                List<?> rawPipeline = parsedResponse.getList("pipeline", Object.class);
                List<Document> pipelineStages = new ArrayList<>();
                if (rawPipeline != null) {
                    for (Object stageObj : rawPipeline) {
                        if (stageObj instanceof Document) {
                            pipelineStages.add((Document) stageObj);
                        } else if (stageObj instanceof String) {
                            pipelineStages.add(Document.parse((String) stageObj));
                        }
                    }
                }

                // Doble chequeo de seguridad en las etapas de agregación
                for (Document stage : pipelineStages) {
                    String stageJson = stage.toJson();
                    if (!esFiltroSeguro(stageJson) || stageJson.contains("$out") || stageJson.contains("$merge")) {
                        return new AdminChatResponse(
                                "No tengo permisos para modificar datos. Solo puedo realizar consultas.",
                                "BLOQUEADO: Etapa de agregación no permitida o sospechosa ($out, $merge u operador prohibido)",
                                false
                        );
                    }
                }

                String pipelineJson = "[" + pipelineStages.stream().map(Document::toJson).collect(Collectors.joining(", ")) + "]";
                queryDetails = "db." + collection + ".aggregate(" + pipelineJson + ")";

                // Ejecución
                MongoCollection<Document> mongoCollection = mongoTemplate.getCollection(collection);
                mongoCollection.aggregate(pipelineStages).into(queryResults);
            } else {
                return new AdminChatResponse(
                        "Operación no soportada. Solo puedo realizar consultas de lectura.",
                        "BLOQUEADO: Operación no permitida (" + operation + ")",
                        false
                );
            }

            // 4. Traducir los resultados obtenidos a lenguaje natural (sintetizar la respuesta)
            String finalNaturalLanguageResponse = sintetizarRespuestaFinal(userMessage, queryDetails, queryResults);

            return new AdminChatResponse(finalNaturalLanguageResponse, queryDetails, true);

        } catch (Exception e) {
            e.printStackTrace();
            return new AdminChatResponse(
                    "Lo siento, ocurrió un problema técnico al consultar la base de datos de MongoDB Atlas. Asegúrate de estructurar tu pregunta de forma simple.",
                    "ERROR DE EJECUCIÓN: " + e.getMessage(),
                    true
            );
        }
    }

    /**
     * Llama al LLM para traducir la pregunta del administrador a una estructura JSON.
     */
    private String generarConsultaEstructurada(String userMessage) {
        String systemInstruction = """
                Tu tarea es traducir la pregunta de un administrador de heladería en una consulta estructurada de MongoDB de SOLO LECTURA.
                
                Las colecciones disponibles son:
                1. 'ventas': contiene los registros de facturas de ventas. Estructura:
                   - id (String)
                   - helados (Lista de ItemVenta: { helado: HeladoDocument, cantidad: int, precioUnitario: double, subTotal: double })
                   - vendedor (String, nombre del administrador/usuario que vendió)
                   - cliente (Objeto Cliente: { nombre: String, cedula: String, Email: String, telefono: String, fechaNacimiento: LocalDate })
                   - metodoPago (String, e.g. 'tarjeta', 'efectivo')
                   - total (double)
                   - fechaVenta (Date/LocalDateTime, e.g. '2026-05-21T14:12:08')
                2. 'helados': contiene el inventario de helados en stock. Estructura:
                   - id (String)
                   - nombre (String)
                   - sabor (String)
                   - tipo (String, cono o vaso)
                   - precio (double)
                   - unidades (int)
                   - ubicacion (String, 'almacen' o 'heladeria')
                3. 'heladosEliminados': contiene helados que fueron dados de baja.
                4. 'cliente': contiene datos de clientes registrados.
                   - nombre (String), cedula (String), Email (String), telefono (String), fechaNacimiento (Date)
                
                Reglas Críticas de Seguridad:
                - Si el usuario te pide editar, borrar, vaciar, remover, actualizar, insertar, inyectar o modificar datos (ej: 'eliminar ventas', 'borrar helados', 'actualizar stock', 'modificar precio'), DEBES marcar safe: false y devolver la razón.
                - NUNCA uses los operadores de agregación '$out' ni '$merge', ya que escriben en base de datos.
                
                Reglas de Formato de Respuesta:
                DEBES responder ÚNICAMENTE con el objeto JSON. Está terminantemente PROHIBIDO incluir cualquier tipo de texto introductorio, explicación conversacional, comentarios o código alternativo fuera del JSON principal. Tu salida debe ser directamente parseable por un lector de JSON sin ningún texto alrededor.
                
                Esquema JSON Exacto a Retornar:
                {
                  "safe": true,
                  "requiresDb": true,
                  "collection": "ventas" | "helados" | "heladosEliminados" | "cliente",
                  "operation": "find" | "aggregate" | "count" | "listCollections",
                  "query": {}, // Filtro para find o count. En MongoDB, si es find vacío usa {}.
                  "projection": {}, // Opcional, e.g. {"nombre": 1, "precio": 1}
                  "sort": {}, // Opcional, e.g. {"total": -1}
                  "limit": 10, // Opcional, máximo 100
                  "pipeline": [], // Requerido si operation es 'aggregate'. Array de etapas de agregación en JSON (e.g. [{"$match":...}, {"$group":...}]).
                  "rejectionReason": null
                }
                
                Si es una pregunta general o saludo que no requiere base de datos:
                {
                  "safe": true,
                  "requiresDb": false,
                  "collection": null,
                  "operation": null,
                  "query": null,
                  "projection": null,
                  "sort": null,
                  "limit": 0,
                  "pipeline": null,
                  "rejectionReason": null
                }
                
                Si la solicitud es peligrosa o de escritura:
                {
                  "safe": false,
                  "requiresDb": false,
                  "collection": null,
                  "operation": null,
                  "query": null,
                  "projection": null,
                  "sort": null,
                  "limit": 0,
                  "pipeline": null,
                  "rejectionReason": "No tengo permisos para modificar datos. Solo puedo realizar consultas."
                }
                """;

        return chatClient.prompt()
                .system(systemInstruction)
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Llama al LLM para formular la respuesta en lenguaje natural basándose en los resultados reales de la base de datos.
     */
    private String sintetizarRespuestaFinal(String userQuestion, String queryDetails, List<Document> queryResults) {
        // Limitar la cantidad de resultados enviados al prompt para evitar desbordar tokens
        String truncatedResults = queryResults.stream()
                .limit(20)
                .map(Document::toJson)
                .collect(Collectors.joining(",\n"));
        if (queryResults.size() > 20) {
            truncatedResults += "\n... (y " + (queryResults.size() - 20) + " resultados más)";
        }

        String systemInstruction = """
                Eres el asistente administrativo inteligente de la heladería 'Ice Cream'. Tu rol es responder la pregunta del administrador de forma profesional, clara y concisa en español, basándote en los datos reales obtenidos de la base de datos de MongoDB.
                
                Reglas Importantes:
                - Genera una respuesta amigable, estructurada y muy clara dirigida al administrador.
                - Presenta los datos de forma legible. Si hay listas de helados o ventas, usa viñetas, listas ordenadas, negritas o emojis para que la lectura sea atractiva y premium.
                - Si los resultados están vacíos, indícalo de manera amable.
                - NUNCA inventes datos. Limítate estrictamente a lo que muestran los resultados reales de la base de datos.
                - Si hay totales monetarios, formátalos con signo de peso (e.g. $15,000 COP).
                - Responde siempre en español.
                """;

        String userPrompt = String.format(
                "Pregunta del Administrador: %s\n\nConsulta ejecutada en MongoDB Atlas: %s\n\nResultados obtenidos de la DB:\n[\n%s\n]",
                userQuestion, queryDetails, truncatedResults
        );

        return chatClient.prompt()
                .system(systemInstruction)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * Valida que la entrada del usuario no contenga palabras clave sospechosas de escritura.
     */
    private boolean esEntradaSegura(String text) {
        if (text == null) return true;
        String lower = text.toLowerCase();
        
        // Bloquear palabras sospechosas
        String[] forbidden = {
                "drop", "delete", "update", "insert", "save", "remove", 
                "createindex", "dropindex", "reindex", "renamecollection", "eval",
                "eliminar", "borrar", "actualizar", "insertar", "crear", "modificar", 
                "guardar", "$out", "$merge", "db.ventas.remove", "db.helados.remove"
        };
        
        for (String word : forbidden) {
            if (lower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valida que los filtros, proyecciones u ordenamientos JSON no contengan comandos peligrosos.
     */
    private boolean esFiltroSeguro(String json) {
        if (json == null) return true;
        String lower = json.toLowerCase();
        
        // Bloquear palabras sospechosas en JSON
        String[] forbidden = {
                "eval", "system", "$out", "$merge", "drop", "delete", "insert", "update"
        };
        
        for (String word : forbidden) {
            if (lower.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extrae y limpia de forma extremadamente robusta el objeto JSON del texto retornado por el LLM.
     * Soporta bloques markdown (```json ... ```) o texto conversacional que envuelva al JSON.
     */
    private String cleanJsonString(String text) {
        if (text == null) return "";
        text = text.trim();

        // 1. Extraer bloques markdown ```json ... ``` prioritariamente
        int jsonBlockStart = text.indexOf("```json");
        if (jsonBlockStart != -1) {
            int jsonBlockEnd = text.indexOf("```", jsonBlockStart + 7);
            if (jsonBlockEnd != -1) {
                return text.substring(jsonBlockStart + 7, jsonBlockEnd).trim();
            }
        }

        // 2. Extraer bloques markdown ``` ... ``` genéricos
        int blockStart = text.indexOf("```");
        if (blockStart != -1) {
            int blockEnd = text.indexOf("```", blockStart + 3);
            if (blockEnd != -1) {
                return text.substring(blockStart + 3, blockEnd).trim();
            }
        }

        // 3. Buscar el primer '{' y el último '}' para extraer únicamente el objeto JSON,
        // descartando cualquier texto conversacional o explicativo del LLM antes o después del objeto.
        int firstBrace = text.indexOf("{");
        int lastBrace = text.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim();
        }

        return text;
    }
}
