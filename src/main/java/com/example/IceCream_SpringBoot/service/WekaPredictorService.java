package com.example.IceCream_SpringBoot.service;

import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.*;

import java.io.InputStream;
import java.util.ArrayList;

@Service
public class WekaPredictorService {

    private final Classifier modelo;
    private final Instances estructura;

    public WekaPredictorService() throws Exception {
        // Cargar modelo entrenado
        InputStream modelStream = getClass().getResourceAsStream("/wekaModels/helados_j48.model");
        modelo = (Classifier) weka.core.SerializationHelper.read(modelStream);

        // === Definir los mismos atributos que en el ARFF y en el ORDEN del entrenamiento ===
        // Orden del entrenamiento: sabor, tipo, precioUnitario, cantidad, edadCliente, metodoPago

        ArrayList<Attribute> atributos = new ArrayList<>();

        // 0. Clase objetivo: sabor (ÍNDICE 0)
        ArrayList<String> sabores = new ArrayList<>();
        sabores.add("mango");
        sabores.add("vainilla");
        sabores.add("coco");
        sabores.add("fresa");
        sabores.add("maracuya");
        sabores.add("mora");
        sabores.add("menta");
        sabores.add("cafe");
        sabores.add("chocolate");
        sabores.add("caramelo");
        sabores.add("nutella");
        sabores.add("limon");
        sabores.add("durazno");
        atributos.add(new Attribute("sabor", sabores));

        // 1. Atributo: tipo (ÍNDICE 1)
        ArrayList<String> tipos = new ArrayList<>();
        tipos.add("paleta");
        tipos.add("vaso");
        tipos.add("cono");
        tipos.add("galleta");
        atributos.add(new Attribute("tipo", tipos));

        // 2. Atributo: precioUnitario (ÍNDICE 2)
        atributos.add(new Attribute("precioUnitario"));

        // 3. Atributo: cantidad (ÍNDICE 3)
        atributos.add(new Attribute("cantidad"));

        // 4. Atributo: edadCliente (ÍNDICE 4)
        atributos.add(new Attribute("edadCliente"));

        // 5. Atributo: metodoPago (ÍNDICE 5)
        ArrayList<String> metodos = new ArrayList<>();
        metodos.add("efectivo");
        metodos.add("tarjeta");
        atributos.add(new Attribute("metodoPago", metodos));

        // Crear estructura vacía
        estructura = new Instances("HeladoPredictor", atributos, 0);
        // Establecer el índice de la clase objetivo en 0
        estructura.setClassIndex(0); 
    }

    /**
     * Predice el sabor de helado basado en los parámetros de entrada.
     * * @param tipo El tipo de helado ("paleta", "vaso", "cono", "galleta").
     * @param precioUnitario El precio del helado.
     * @param cantidad La cantidad de helados.
     * @param edadCliente La edad del cliente.
     * @param metodoPago El método de pago ("efectivo", "tarjeta").
     * @return El sabor de helado predicho por el modelo.
     * @throws Exception Si ocurre un error durante la clasificación.
     */
    public String predecirSabor(String tipo, double precioUnitario, double cantidad,
                                double edadCliente, String metodoPago) throws Exception {

        // La instancia debe tener 6 posiciones (el total de atributos, incluyendo la clase)
        Instance instancia = new DenseInstance(6);
        instancia.setDataset(estructura);

        // NOTA: El índice 0 (sabor) se deja sin establecer, ya que es el valor que se va a predecir.

        // Asignar los valores de entrada a los índices correctos:
        instancia.setValue(1, tipo);
        instancia.setValue(2, precioUnitario);
        instancia.setValue(3, cantidad);
        instancia.setValue(4, edadCliente);
        instancia.setValue(5, metodoPago);

        double resultado = modelo.classifyInstance(instancia);
        
        // Retornar el valor nominal de la clase predicha (sabor)
        return estructura.classAttribute().value((int) resultado);
    }
}