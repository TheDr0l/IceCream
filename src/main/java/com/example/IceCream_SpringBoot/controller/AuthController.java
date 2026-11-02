package com.example.IceCream_SpringBoot.controller;

import com.example.IceCream_SpringBoot.model.User;
import com.example.IceCream_SpringBoot.service.AuthService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/")
    public String root() {
        return "redirect:/homePage";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "Login";
    }

    @GetMapping("/panelUsuariosInternos")
    public String register() {
        return "panelUsuariosInternos";
    }

    @PostMapping("/panelUsuariosInternos")
    public String registerUser(@RequestParam String username,
            @RequestParam String password,
            @RequestParam String passwordConfirmation,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        if (authService.usuarioExiste(username)) {
            redirectAttributes.addFlashAttribute("error", "El usuario ya existe");
            return "redirect:/panelUsuariosInternos";
        }

        boolean success = authService.registrarNuevoUsuario(username, password, passwordConfirmation, role);

        if (success) {
            redirectAttributes.addFlashAttribute("mensaje", "Usuario registrado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el usuario.");
        }

        return "redirect:/panelUsuariosInternos";
    }

    @GetMapping("/homePage")
    public String homePage() {
        return "homePage";
    }

    @GetMapping("/home")
    public String home() {
        return "index";
    }

    @GetMapping("/ventas")
    public String ventas() {
        return "VenderHelados";
    }

    // Listar todos los usuarios
    @GetMapping("/obtenerUsuarios")
    @ResponseBody
    public List<User> obtenerUsuarios() {
        return authService.obtenerTodosLosUsuarios();
    }

    // Eliminar usuario por ID
    @PostMapping("/eliminarUsuario")
    public String eliminarUsuario(@RequestParam String id, RedirectAttributes redirectAttributes) {
        boolean eliminado = authService.eliminarUsuarioPorId(id);

        if (eliminado) {
            redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el usuario.");
        }

        // Recargamos la pagina para actualizar la tabla
        return "redirect:/panelUsuariosInternos";
    }

}
