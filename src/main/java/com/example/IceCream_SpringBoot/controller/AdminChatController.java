package com.example.IceCream_SpringBoot.controller;

import com.example.IceCream_SpringBoot.dto.AdminChatRequest;
import com.example.IceCream_SpringBoot.dto.AdminChatResponse;
import com.example.IceCream_SpringBoot.service.AdminChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminChatController {

    private final AdminChatService adminChatService;

    @Autowired
    public AdminChatController(AdminChatService adminChatService) {
        this.adminChatService = adminChatService;
    }

    /**
     * Endpoint exclusivo para administradores para chatear con el asistente de MongoDB.
     * Este endpoint requiere rol ADMIN (según SecurityConfig.java .anyRequest().hasRole("ADMIN")).
     */
    @PostMapping("/chat")
    public AdminChatResponse chatAdmin(@RequestBody AdminChatRequest request) {
        if (request == null || request.message() == null || request.message().trim().isEmpty()) {
            return new AdminChatResponse(
                    "Por favor escribe una consulta válida.",
                    "Ninguna",
                    true
            );
        }
        return adminChatService.procesarMensajeAdmin(request.message());
    }
}
