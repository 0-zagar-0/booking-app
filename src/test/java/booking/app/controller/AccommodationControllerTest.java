package booking.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import booking.app.dto.accommodation.AccommodationFullInfoResponseDto;
import booking.app.dto.accommodation.AccommodationIncompleteInfoResponseDto;
import booking.app.dto.accommodation.AccommodationRequestDto;
import booking.app.dto.accommodation.AccommodationUpdateRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccommodationControllerTest {
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
            callSqlQueryFromFile(connection, "add-two-address-to-address-table.sql");
            callSqlQueryFromFile(connection, "add-two-amenities-to-amenities-table.sql");
            callSqlQueryFromFile(connection, "add-two-accommodation-to-accommodation-"
                    + "table.sql");
            callSqlQueryFromFile(connection, "add-accommodation-id-and-amenity-id-to-"
                    + "accommodation-amenities-table.sql");
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
            callSqlQueryFromFile(connection, "delete-all-from-accommodations-"
                    + "amenities-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-amenities-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-accommodations-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-address-table.sql");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    @Sql(scripts = {"classpath:database/accommodation/delete-accommodation-id-and-amenities-id-"
            + "from-accommodations-amenities-table.sql",
            "classpath:database/accommodation/delete-accommodation-with-id-three-from-"
                    + "accommodations-table.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Create accommodation with valid data, should return AccommodationFullInfoDto")
    void createAccommodation_WithValidData_ShouldReturnAccommodationFullDto() throws Exception {
        //Given
        AccommodationRequestDto requestDto = new AccommodationRequestDto();
        requestDto.setType("HOUSE");
        requestDto.setAddress("City, Street, 12");
        requestDto.setAmenities(Set.of("PARKING", "WIFI"));
        requestDto.setSize("TWO_BEDROOM");
        requestDto.setDailyRate(BigDecimal.valueOf(100));
        requestDto.setAvailability(1);

        AccommodationFullInfoResponseDto expected = new AccommodationFullInfoResponseDto(
                4L,
                requestDto.getType(),
                requestDto.getAddress(),
                requestDto.getSize(),
                requestDto.getAmenities(),
                requestDto.getDailyRate(),
                requestDto.getAvailability()
        );

        //When
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(
                post("/accommodations")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        AccommodationFullInfoResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccommodationFullInfoResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }
    
    @Test
    @DisplayName("Get all with exists two accommodation, should return list accommodations")
    void getAll_WithExistsTwoAccommodation_ShouldReturnListAccommodations() throws Exception {
        //Given
        AccommodationIncompleteInfoResponseDto expected =
                new AccommodationIncompleteInfoResponseDto(
                        1L,
                        "APARTMENT",
                        "City, Street, 21"
                );
        //When
        MvcResult result = mockMvc.perform(
                get("/accommodations/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        //Then
        List<AccommodationIncompleteInfoResponseDto> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<AccommodationIncompleteInfoResponseDto>>() {}
        );
        assertEquals(actual.size(), 2);
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual.get(0)));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    @DisplayName("Get by id by valid id, should return AccommodationFullDto")
    void getById_ByValidId_ShouldReturnAccommodationFullDto() throws Exception {
        //Given
        AccommodationFullInfoResponseDto expected = new AccommodationFullInfoResponseDto(
                2L,
                "APARTMENT",
                "City, Street, 18",
                "TWO_BEDROOM",
                Set.of("PARKING", "WIFI"),
                BigDecimal.valueOf(150.00),
                2
        );

        //When
        MvcResult result = mockMvc.perform(
                get("/accommodations/{id}", 2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        AccommodationFullInfoResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), AccommodationFullInfoResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual, "dailyRate"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    @Sql(scripts = {"classpath:database/accommodation/restore-accommodation-to-last-state-"
            + "after-update-test.sql",
            "classpath:database/accommodation/delete-accommodation-id-and-amenity-id-from-"
                    + "accommodations-amenities-table-after-update-test.sql",
            "classpath:database/accommodation/delete-amenity-big-window-after-update-test.sql"},
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @DisplayName("Update by id with valid data, should return status OK")
    void updateById_WithValidData_ShouldReturnStatusOk() throws Exception {
        //Given
        AccommodationUpdateRequestDto updateRequest = new AccommodationUpdateRequestDto();
        updateRequest.setAvailability(3);
        updateRequest.setAmenities(Set.of("BIG WINDOW"));
        updateRequest.setDailyRate(BigDecimal.valueOf(200.00));

        AccommodationFullInfoResponseDto expected = new AccommodationFullInfoResponseDto(
                2L,
                "APARTMENT",
                "City, Street, 18",
                "TWO_BEDROOM",
                Set.of("BIG WINDOW"),
                BigDecimal.valueOf(200.00),
                3
        );

        //When
        String requestJson = objectMapper.writeValueAsString(updateRequest);
        mockMvc.perform(
                patch("/accommodations/{id}", 2)
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult resultGet = mockMvc.perform(
                get("/accommodations/{id}", 2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        AccommodationFullInfoResponseDto actual = objectMapper.readValue(
                resultGet.getResponse().getContentAsString(), AccommodationFullInfoResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual, "dailyRate"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    @Sql(scripts = "classpath:database/accommodation/add-accommodation-for-delete-test.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("Delete by id with valid id, should return status NO CONTENT")
    void deleteById_WithValidId_ShouldReturnStatusNoContent() throws Exception {
        //Given
        Long id = 3L;

        //When
        mockMvc.perform(
                delete("/accommodations/{id}", id))
                .andExpect(status().isNoContent())
                .andReturn();

        MvcResult resultGetAll = mockMvc.perform(
                get("/accommodations/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<AccommodationIncompleteInfoResponseDto> actual = objectMapper.readValue(
                resultGetAll.getResponse().getContentAsByteArray(),
                new TypeReference<List<AccommodationIncompleteInfoResponseDto>>() {}
        );
        assertEquals(actual.size(), 2);
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("database/accommodation/" + fileName)
        );
    }
}
