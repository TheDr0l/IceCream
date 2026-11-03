package com.example.IceCream_SpringBoot.config;

import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.Cookie;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * Rutas que no deben intentar autenticarse con el token. Si una cookie
     * expirada se procesara en /login, el navegador quedaria atrapado en un
     * bucle de redirecciones.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/", "/homePage", "/login", "/error", "/favicon.ico",
            "/css/**", "/img/**", "/js/**",
            "/api/email", "/api/chat", "/api/auth/login", "/api/auth/logout");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        SecretKey secretKey = jwtProperties.toSecretKey();
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder.withSecretKey(jwtProperties.toSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Acepta el token desde la cabecera Authorization: Bearer (clientes API) o
     * desde la cookie HttpOnly usada por las paginas Thymeleaf.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver(JwtProperties jwtProperties) {
        DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();

        return request -> {
            if (isPublicPath(request.getRequestURI())) {
                return null;
            }

            String token = headerResolver.resolve(request);
            if (StringUtils.hasText(token)) {
                return token;
            }

            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if (jwtProperties.getCookieName().equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
            return null;
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /**
     * El claim roles ya contiene los valores ROLE_ADMIN / ROLE_USER, por eso no
     * se anade prefijo adicional.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            BearerTokenResolver bearerTokenResolver,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                // Sin formulario no hay sesion: cada peticion se autentica con su JWT.
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // publico (misma lista usada para no procesar tokens en estas rutas)
                        .requestMatchers(PUBLIC_PATHS.toArray(String[]::new)).permitAll()
                        // cualquier usuario autenticado
                        .requestMatchers("/api/auth/me").authenticated()
                        // rutas de ventas: solo USER
                        .requestMatchers("/venderHelados", "/venderHelados/**", "/buscarCliente").hasRole("USER")
                        // todo lo demas: ADMIN
                        .anyRequest().hasRole("ADMIN"))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
