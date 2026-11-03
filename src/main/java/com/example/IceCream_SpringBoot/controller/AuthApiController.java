package com.example.IceCream_SpringBoot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.IceCream_SpringBoot.service.JwtService;

/**
 * API de autenticacion basada en JWT. El token se entrega en una cookie
 * HttpOnly para las paginas Thymeleaf y tambien se puede enviar como
 * Authorization: Bearer desde otros clientes.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthApiController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.username())
                || !StringUtils.hasText(request.password())) {
            return problem(HttpStatus.BAD_REQUEST, "Datos incompletos",
                    "Usuario y contraseña son obligatorios");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));

            String token = jwtService.generateToken(authentication);
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            String redirect = roles.contains("ROLE_ADMIN") ? "/home" : "/venderHelados";

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtService.createAuthCookie(token).toString())
                    .body(new LoginResponse(authentication.getName(), roles, redirect));

        } catch (AuthenticationException ex) {
            return problem(HttpStatus.UNAUTHORIZED, "Credenciales invalidas",
                    "Usuario o contraseña incorrectos");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearAuthCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return problem(HttpStatus.UNAUTHORIZED, "No autenticado", "No hay un token valido en la peticion");
        }
        List<String> roles = jwt.getClaimAsStringList("roles");
        return ResponseEntity.ok(Map.of(
                "username", jwt.getSubject(),
                "roles", roles == null ? List.of() : roles));
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }

    public record LoginRequest(String username, String password) {
    }

    public record LoginResponse(String username, List<String> roles, String redirect) {
    }
}
