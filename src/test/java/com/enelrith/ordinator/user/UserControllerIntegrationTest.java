package com.enelrith.ordinator.user;

import com.enelrith.ordinator.TestcontainersConfiguration;
import com.enelrith.ordinator.user.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {
    private static final String USERS_URI = "/api/users";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void createUser_returnsCreated() throws Exception {
        var request = new CreateUserRequest("test@email.com", "testPassword", "Lor'themar", "Ibn-La'Ahad");

        mockMvc.perform(post(USERS_URI)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()));
    }

    @Test
    void createUsers_withExistingEmail_returnsConflict() throws Exception {
        var request = new CreateUserRequest("test@email.com", "testPassword", "First", "Last");
        var user = UserMapper.toEntity(request, "hashedPassword");

        userRepository.save(user);

        mockMvc.perform(post(USERS_URI)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "test@email.com, testPassword, -John, test",
            "test@email.com, testPassword, John-, test",
            "test@email.com, testPassword, Jo--hn, test",
            "test@email.com, testPassword, 'John, test",
            "test@email.com, testPassword, John', test",
            "test@email.com, testPassword, John$, test",
    }, quoteCharacter = '"')
    void createUsers_withInvalidName_returnsBadRequest(String email, String rawPassword, String firstName, String lastName) throws Exception {
        var request = new CreateUserRequest(email, rawPassword, firstName, lastName);

        mockMvc.perform(post(USERS_URI)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
