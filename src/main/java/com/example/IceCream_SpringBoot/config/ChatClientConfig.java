package com.example.IceCream_SpringBoot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                // Para evitar repetir la configuración del sistema en cada llamada,
                // se puede establecer un sistema predeterminado en el constructor del
                // controlador.
                .defaultSystem("""
                            Eres asistente de soporte técnico interno.
                            Tu función es ayudar a los empleados con problemas relacionados con TI,
                            como restablecer contraseñas, desbloquear cuentas y responder preguntas
                            sobre políticas de TI. Si un usuario solicita ayuda con algo que no entra
                            dentro de estas responsabilidades, responde amablemente e infórmale que solo
                            puedes brindar soporte técnico dentro de tu ámbito de competencia.
                        """)
                // También se puede establecer un mensaje de usuario predeterminado para
                // pruebas rápidas, aunque en un caso real probablemente se omitiría o se
                // establecería a algo más genérico.
                .defaultUser("Como me puedes ayudar")
                .build();
    }

}

