package com.enelrith.ordinator.security;

import com.enelrith.ordinator.TestcontainersConfiguration;
import com.enelrith.ordinator.user.User;
import com.enelrith.ordinator.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {
    private static final String AUTH_URI = "/api/auth";
    private static final String EMAIL = "test@email.com";
    private static final String RAW_PASSWORD = "testPassword";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        var user = new User(EMAIL, passwordEncoder.encode(RAW_PASSWORD), "test", "test");
        userRepository.save(user);
    }

    @Test
    void login_returnsNoContent() throws Exception {
        mockMvc.perform(post(AUTH_URI + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", EMAIL)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().isNoContent())
                .andExpect(authenticated());
    }

    @ParameterizedTest
    @CsvSource({
            "test@email.com, anotherPassword",
            "anotherEmail@email.com, testPassword"
    })
    void login_withInvalidCredentials_returnsUnauthorized(String email, String password) throws Exception {
        var user = new User("anotherEmail@email.com", passwordEncoder.encode("anotherPassword"), "test", "test");
        userRepository.save(user);

        mockMvc.perform(post(AUTH_URI + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void login_withMissingCsrf_returnsForbidden() throws Exception {
        mockMvc.perform(post(AUTH_URI + "/login"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_returnsNoContent() throws Exception {
        var loginResult = mockMvc.perform(post(AUTH_URI + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", EMAIL)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().isNoContent())
                .andExpect(authenticated())
                .andReturn();

        var session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        var logoutResult = mockMvc.perform(post(AUTH_URI + "/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isNoContent())
                .andExpect(unauthenticated())
                .andReturn();

        var jSessionCookie = logoutResult.getResponse().getCookie("JSESSIONID");
        var csrfCookie = logoutResult.getResponse().getCookie("XSRF-TOKEN");

        assertNotNull(jSessionCookie);
        assertNotNull(csrfCookie);
        assertEquals(0, jSessionCookie.getMaxAge());
        assertEquals(0, csrfCookie.getMaxAge());
    }

    @Test
    void me_returnsUserDto() throws Exception {
        var loginResult = mockMvc.perform(post(AUTH_URI + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", EMAIL)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().isNoContent())
                .andExpect(authenticated())
                .andReturn();

        var session = (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(get(AUTH_URI + "/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void me_withUnauthenticatedUser_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(AUTH_URI + "/me"))
                .andExpect(status().isUnauthorized());
    }
}
