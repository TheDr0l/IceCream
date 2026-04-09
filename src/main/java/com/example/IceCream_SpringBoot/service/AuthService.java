package com.example.IceCream_SpringBoot.service;

import com.example.IceCream_SpringBoot.model.User;
import com.example.IceCream_SpringBoot.repository.UserRepository;

import jakarta.annotation.PostConstruct;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Inyectamos el encriptador de contraseñas

    public boolean usuarioExiste(String usuario) {
        return userRepository.findByUsername(usuario).isPresent();
    }

    public boolean registrarNuevoUsuario(String usuario, String contrasena, String passwordConfirmation, String role) {
        if (usuarioExiste(usuario)) {
            return false; // No permite registrar si el usuario ya existe
        }
        if (!contrasena.equals(passwordConfirmation)) {
            return false; // No permite registrar si las contraseñas no coinciden
        }

        // Encripta la contraseña antes de guardarla
        String hashedPassword = passwordEncoder.encode(contrasena);

        // Crea el usuario y lo guarda en la base de datos
        User user = new User(usuario, hashedPassword, role);
        userRepository.save(user);
        return true;
    }

    public User obtenerUsuarioPorNombre(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public List<User> obtenerTodosLosUsuarios() {
        return userRepository.findAll();
    }

    public boolean eliminarUsuarioPorId(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    //pruebas
    @PostConstruct
    public void crearAdminPorDefecto() {

        String adminUser = "admin";
        String adminPass = "admin";

        if (userRepository.findByUsername(adminUser).isEmpty()) {

            User admin = new User();
            admin.setUsername(adminUser);
            admin.setPassword(passwordEncoder.encode(adminPass));
            admin.setRoles(Set.of("ADMIN"));

            userRepository.save(admin);

            System.out.println("ADMIN creado");
        }
    }

}
