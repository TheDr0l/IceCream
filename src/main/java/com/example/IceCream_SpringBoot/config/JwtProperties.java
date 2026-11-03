package com.example.IceCream_SpringBoot.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del token JWT definidas en application.properties con el prefijo
 * {@code app.jwt}.
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private static final int MIN_SECRET_BYTES = 32;

    private String secret;
    private Duration expiration = Duration.ofHours(8);
    private String issuer = "icecream";
    private String cookieName = "icecream_token";
    private boolean cookieSecure = false;

    /**
     * Convierte el secreto configurado en la clave HMAC-SHA256 usada para firmar
     * y validar los tokens.
     */
    public SecretKey toSecretKey() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) debe estar definido y tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }
}
