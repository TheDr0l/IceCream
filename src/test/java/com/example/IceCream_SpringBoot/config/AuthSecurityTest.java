package com.example.IceCream_SpringBoot.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.IceCream_SpringBoot.controller.AuthApiController;
import com.example.IceCream_SpringBoot.service.JwtService;

import jakarta.servlet.http.Cookie;

@WebMvcTest(controllers = AuthApiController.class)
@Import({ SecurityConfig.class, JwtAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtService.class, AuthApiController.class, AuthSecurityTest.TestConfig.class })
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-de-pruebas-con-mas-de-32-bytes-123456",
        "app.jwt.expiration=PT1H",
        "app.jwt.issuer=icecream-test",
        "app.jwt.cookie-name=icecream_token",
        "app.jwt.cookie-secure=false"
})
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginDeAdminDevuelveTokenEnCookieYRedireccion() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"admin","password":"admin123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("icecream_token"))
                .andExpect(cookie().httpOnly("icecream_token", true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.redirect").value("/home"));
    }

    @Test
    void loginDeVendedorRedirigeAlPuntoDeVenta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"vendedor","password":"vendedor123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.redirect").value("/venderHelados"));
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"admin","password":"incorrecta"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciales invalidas"));
    }

    @Test
    void loginSinDatosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"","password":""}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elTokenFuncionaComoBearer() throws Exception {
        String token = loginYObtenerToken("admin", "admin123");

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void elTokenFuncionaDesdeLaCookie() throws Exception {
        String token = loginYObtenerToken("admin", "admin123");

        mockMvc.perform(get("/api/auth/me")
                .cookie(new Cookie("icecream_token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void sinTokenLaApiDevuelve401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void unVendedorNoPuedeEntrarAUnaRutaDeAdmin() throws Exception {
        String token = loginYObtenerToken("vendedor", "vendedor123");

        mockMvc.perform(get("/home")
                .cookie(new Cookie("icecream_token", token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdminNoPuedeEntrarAlPuntoDeVentaDeVendedores() throws Exception {
        String token = loginYObtenerToken("admin", "admin123");

        mockMvc.perform(get("/venderHelados")
                .cookie(new Cookie("icecream_token", token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaCookieInvalidaNoBloqueaElLogin() throws Exception {
        mockMvc.perform(get("/login")
                .cookie(new Cookie("icecream_token", "token-invalido")))
                .andExpect(status().isOk());
    }

    private String loginYObtenerToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("icecream_token").getValue();
    }

    @SpringBootConfiguration
    static class TestApplication {
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        UserDetailsService userDetailsService() {
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin").password(encoder.encode("admin123")).roles("ADMIN").build(),
                    User.withUsername("vendedor").password(encoder.encode("vendedor123")).roles("USER").build());
        }

        @Bean
        TestEndpoints testEndpoints() {
            return new TestEndpoints();
        }
    }

    @RestController
    static class TestEndpoints {

        @GetMapping("/home")
        String home() {
            return "home";
        }

        @GetMapping("/venderHelados")
        String venderHelados() {
            return "venderHelados";
        }

        @GetMapping("/login")
        String login() {
            return "login";
        }
    }
}
