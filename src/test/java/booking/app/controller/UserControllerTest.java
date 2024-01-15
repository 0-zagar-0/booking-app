package booking.app.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import booking.app.dto.user.UserResponseDto;
import booking.app.dto.user.UserUpdateProfileInformationDto;
import booking.app.dto.user.UserUpdateRoleDto;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {
    protected static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

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
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get user profile by authenticated user should return UserResponseDto")
    void getUserProfile_ByAuthenticatedUser_ShouldReturnUserDto() throws Exception {
        //Given
        UserResponseDto expected = createUserResponseDto();

        //When
        MvcResult result = getResultFromGetRequest(status().isOk());

        //Then
        UserResponseDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @WithMockUser(username = "user", roles = "CUSTOMER")
    @DisplayName("Get user profile by unauthenticated user should return exception")
    void getUserProfile_ByUnauthenticatedUser_ShouldReturnException() throws Exception {
        //Given
        String expected = "Can't find user by user email: user";

        //When
        MvcResult result = getResultFromGetRequest(status().isNotFound());

        //Then
        assertTrue(result.getResponse().getContentAsString().contains(expected));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    @Sql(scripts = "classpath:database/users/update-user-role-from-users-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Update user role with valid data should return status OK")
    void updateUserRole_WithValidData_ShouldReturnStatusOk() throws Exception {
        //Given
        Long id = 1L;
        UserUpdateRoleDto updateRoleDto = new UserUpdateRoleDto("ROLE_MANAGER");

        //When
        String jsonRequest = objectMapper.writeValueAsString(updateRoleDto);
        mockMvc.perform(
                put("/users/{id}/role", id)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    @DisplayName("Update user role with invalid data should return exception")
    void updateUserRole_WithInvalidId_ShouldReturnException() throws Exception {
        //Given
        Long id = 999L;
        UserUpdateRoleDto updateRoleDto = new UserUpdateRoleDto("ROLE_MANAGER");

        //When
        String jsonRequest = objectMapper.writeValueAsString(updateRoleDto);
        mockMvc.perform(
                        put("/users/{id}/role", id)
                                .content(jsonRequest)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @Sql(scripts = "classpath:database/users/restore-user-from-users-table.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Update user profile with valid data should return UserResponseDto")
    void updateUserProfile_WithValidData_ShouldReturnUserDto() throws Exception {
        //Given
        UserUpdateProfileInformationDto updateRequest = new UserUpdateProfileInformationDto(
                "update@example.com",
                "User=123456789",
                "Update",
                "Update"
        );
        UserResponseDto expected = new UserResponseDto(
                1L,
                updateRequest.email(),
                updateRequest.firstName(),
                updateRequest.lastName()
        );

        //When
        String jsonRequest = objectMapper.writeValueAsString(updateRequest);
        MvcResult result = mockMvc.perform(
                put("/users/me")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        UserResponseDto actual = objectMapper.readValue(result.getResponse().getContentAsString(),
                UserResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    private MvcResult getResultFromGetRequest(ResultMatcher matcher) throws Exception {
        return mockMvc.perform(
                get("/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(matcher)
                .andReturn();
    }

    private UserResponseDto createUserResponseDto() {
        return new UserResponseDto(
                1L,
                "customer@example.com",
                "Bob",
                "Alison"
        );
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("database/users/" + fileName)
        );
    }
}
