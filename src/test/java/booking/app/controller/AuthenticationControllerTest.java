package booking.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import booking.app.dto.user.UserLoginRequestDto;
import booking.app.dto.user.UserRegisterRequestDto;
import booking.app.dto.user.UserResponseDto;
import booking.app.exception.EntityNotFoundException;
import booking.app.security.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationControllerTest {
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @BeforeAll
    static void setUp(@Autowired DataSource dataSource,
                      @Autowired WebApplicationContext webApplicationContext
    ) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        teardown(dataSource);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            callSqlQueryFromFile(connection, "add-two-user-to-users-table.sql");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void afterAll(@Autowired DataSource dataSource) {
        teardown(dataSource);
    }

    @SneakyThrows
    static void teardown(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            callSqlQueryFromFile(connection, "delete-all-from-users-table.sql");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Sql(scripts = "classpath:database/authentication/delete-user-with-id-3-from-users-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Register user with valid data should return UserResponseDto")
    void register_WithValidData_ShouldReturnUserDto() throws Exception {
        //Given
        UserRegisterRequestDto requestDto = new UserRegisterRequestDto();
        requestDto.setEmail("new@example.com");
        requestDto.setPassword("User=123456789");
        requestDto.setRepeatPassword("User=123456789");
        requestDto.setFirstName("New");
        requestDto.setLastName("New");

        UserResponseDto expected = new UserResponseDto(
                3L,
                requestDto.getEmail(),
                requestDto.getFirstName(),
                requestDto.getLastName()
        );

        //When
        String requestJson = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(
                post("/auth/register")
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        UserResponseDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @DisplayName("Login user with exists user should return JwtToken")
    void login_WithExistsUser_ShouldReturnJwtToken() throws Exception {
        //Given
        UserLoginRequestDto requestDto = new UserLoginRequestDto();
        requestDto.setEmail("customer@example.com");
        requestDto.setPassword("User=123456789");

        //When
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(
                post("/auth/login")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        String expected = ".*\"token\":\".*\\..*\\..*\".*";
        String actual = result.getResponse().getContentAsString();
        assertTrue(actual.matches(expected));
    }

    @Test
    @DisplayName("Login user with non exists user should return exception")
    void login_WithNonExistsUser_ShouldReturnException() throws Exception {
        //Given
        UserLoginRequestDto requestDto = new UserLoginRequestDto();
        requestDto.setEmail("nonexist@example.com");
        requestDto.setPassword("User=123456789");

        //When
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        Exception exception = assertThrows(
                EntityNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(requestDto.getEmail())
        );

        //Then
        String expected = "Can't find user by email: " + requestDto.getEmail();
        String actual = result.getResponse().getContentAsString();
        assertTrue(actual.contains(expected));
        assertEquals(expected, exception.getMessage());
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("database/users/" + fileName)
        );
    }
}
