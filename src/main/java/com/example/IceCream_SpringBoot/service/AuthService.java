package com.example.IceCream_SpringBoot.service;

import com.example.IceCream_SpringBoot.model.User;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private Set<User> usuariosRegistrados = new HashSet<>();

    public boolean usuarioExiste(String usuario) {
        return usuariosRegistrados.stream().anyMatch(user -> user.getUsuario().equals(usuario));
    }

    public boolean registrarNuevoUsuario(String usuario, String contrasena, String pin) {
        if (usuarioExiste(usuario)) {
            return false;
        }
        if (!pin.equals("admin123")) {
            return false;
        }

        User user = new User(usuario, contrasena);
        usuariosRegistrados.add(user);
        return true;
    }

    public boolean validarUsuario(String usuario, String contrasena) {
        return usuariosRegistrados.stream().anyMatch(user -> user.getUsuario().equals(usuario) &&
                user.getContrasena().equals(contrasena));
    }
}
