package com.example.IceCream_SpringBoot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.example.IceCream_SpringBoot.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-prueba-con-mas-de-32-bytes-abcdef";

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpiration(Duration.ofHours(8));
        properties.setIssuer("icecream-test");

        SecretKey secretKey = properties.toSecretKey();
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        jwtService = new JwtService(jwtEncoder, properties);
    }

    @Test
    void generaTokenConSujetoRolesEmisorYExpiracion() {
        String token = jwtService.generateToken("admin", List.of("ROLE_ADMIN"));

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("icecream-test");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void creaCookieHttpOnlyConNombreYDuracionConfigurados() {
        ResponseCookie cookie = jwtService.createAuthCookie("token-de-prueba");

        assertThat(cookie.getName()).isEqualTo("icecream_token");
        assertThat(cookie.getValue()).isEqualTo("token-de-prueba");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void borraLaCookieConMaxAgeCero() {
        ResponseCookie cookie = jwtService.clearAuthCookie();

        assertThat(cookie.getName()).isEqualTo("icecream_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    void rechazaSecretosDemasiadoCortos() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("corto");

        assertThatThrownBy(properties::toSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
