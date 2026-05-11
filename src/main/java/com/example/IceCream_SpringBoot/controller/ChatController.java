package com.example.IceCream_SpringBoot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(@Qualifier("chatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Value("classpath:/promptTemplates/userPromptTemplate.st")
    Resource userPromptTemplate;

    // Endpoint existente
    @GetMapping("/email")
    public String emailResponse(@RequestParam("customerName") String customerName,
            @RequestParam("customerMessage") String customerMessage) {
        return chatClient
                .prompt()
                .system("""
                        Eres un asistente profesional de atención al cliente que ayuda a redactar correos
                        electrónicos para mejorar la productividad del equipo de atención al cliente.
                        """)
                .user(promptTemplateSpec -> promptTemplateSpec.text(userPromptTemplate)
                        .param("customerName", customerName)
                        .param("customerMessage", customerMessage))
                .call().content();
    }

    // Nuevo endpoint para el chatbot de la pagina principal
    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return chatClient
                .prompt()
                .user(request.message())
                .call()
                .content();
    }

    public record ChatRequest(String message) {}
}