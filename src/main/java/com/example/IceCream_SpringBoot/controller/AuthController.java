package com.example.IceCream_SpringBoot.controller;

import com.example.IceCream_SpringBoot.model.User;
import com.example.IceCream_SpringBoot.service.AuthService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
            Model model) {
        if (authService.usuarioExiste(username)) {
            model.addAttribute("error", "El usuario ya existe");
            return "redirect:/panelUsuariosInternos";
        }

        boolean success = authService.registrarNuevoUsuario(username, password, passwordConfirmation, role);

        if (success) {
            model.addAttribute("mensaje", "Usuario registrado correctamente.");
        } else {
            model.addAttribute("error", "Error al registrar el usuario.");
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
    public String eliminarUsuario(@RequestParam Long id, Model model) {
        boolean eliminado = authService.eliminarUsuarioPorId(id);

        if (eliminado) {
            model.addAttribute("mensaje", "Usuario eliminado correctamente.");
        } else {
            model.addAttribute("error", "Error al eliminar el usuario.");
        }

        // Recargamos la pagina para actualizar la tabla
        return "redirect:/panelUsuariosInternos";
    }

}
