package booking.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import booking.app.dto.booking.BookingRequestDto;
import booking.app.dto.booking.BookingResponseDto;
import booking.app.dto.booking.BookingUpdateRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
class BookingControllerTest {
    private static final DateTimeFormatter PATTERN_OF_DATE =
            DateTimeFormatter.ofPattern("yyyy, MM, dd");
    private static MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private LocalDate date = LocalDate.now();

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
            callSqlQueryFromFile(connection, "add-two-user-to-users-table.sql");
            callSqlQueryFromFile(connection, "add-two-booking-to-bookings-table.sql");

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
            callSqlQueryFromFile(connection, "delete-all-from-bookings-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-accommodations-"
                    + "amenities-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-amenities-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-accommodations-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-address-table.sql");
            callSqlQueryFromFile(connection, "delete-all-from-users-table.sql");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Sql(scripts = "classpath:database/booking/delete-booking-after-create-method.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Create booking with valid data, should return BookingResponseDto")
    void createBooking_WithValidData_ShouldReturnBookingDto() throws Exception {
        //Given
        BookingRequestDto requestDto = new BookingRequestDto();
        requestDto.setCheckInDateYearMonthDay(date.plusDays(2).format(PATTERN_OF_DATE));
        requestDto.setAccommodationId(1L);
        requestDto.setDaysOfStay(2);

        BookingResponseDto expected = createResponseDto(
                4L,
                requestDto.getCheckInDateYearMonthDay(),
                requestDto.getDaysOfStay(),
                1L,
                1L,
                "PENDING");

        //When
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        MvcResult result = mockMvc.perform(
                post("/bookings")
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        //Then
        BookingResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), BookingResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Get all by valid user id and valid status,"
            + " should return list BookingResponseDto")
    void getAllByUserIdAndStatus_ByValidUserIdAndValidStatus_ShouldReturnListBookingDto()
            throws Exception {
        //Given
        String date = "2024, 01, 23";
        BookingResponseDto booking = createResponseDto(
                2L,
                date,
                2,
                2L,
                2L,
                "CONFIRMED"
        );
        List<BookingResponseDto> expected = List.of(booking);

        //When
        MvcResult result = mockMvc.perform(
                get("/bookings")
                        .param("user_id", "2")
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<BookingResponseDto> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<BookingResponseDto>>() {}
        );
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get all my bookings, should return all exists bookings")
    void getAllMyBookings_ShouldReturnAllExistsBookings() throws Exception {
        //Given
        String date = "2024, 01, 21";
        BookingResponseDto booking = createResponseDto(
                1L,
                date,
                2,
                1L,
                1L,
                "CONFIRMED"
        );
        List<BookingResponseDto> expected = List.of(booking);

        //When
        MvcResult result = mockMvc.perform(
                        get("/bookings/me")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        List<BookingResponseDto> actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                new TypeReference<List<BookingResponseDto>>() {}
        );
        assertEquals(expected.size(), actual.size());
        assertTrue(EqualsBuilder.reflectionEquals(expected.get(0), actual.get(0)));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Get by id by valid id, should return BookingResponseDto")
    void getById_ByValidId_ShouldReturnBookingDto() throws Exception {
        //Given
        String date = "2024, 01, 21";
        BookingResponseDto expected = createResponseDto(
                1L,
                date,
                2,
                1L,
                1L,
                "CONFIRMED"
        );

        //When
        MvcResult result = mockMvc.perform(
                get("/bookings/{id}", expected.id())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        BookingResponseDto actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), BookingResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @Sql(scripts = "classpath:database/booking/restore-booking-to-last-state-after-"
            + "update-method.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    @WithMockUser(username = "customer@example.com", roles = "CUSTOMER")
    @DisplayName("Update by id by valid id and valid update data, should return nothing")
    void updateById_ByValidIdAndValidData_ShouldNothingReturn() throws Exception {
        //Given
        BookingUpdateRequestDto requestDto = new BookingUpdateRequestDto();
        requestDto.setCheckInDateYearMonthDay(date.plusDays(4).format(PATTERN_OF_DATE));
        requestDto.setStatus("PENDING");
        requestDto.setDaysOfStay(2);

        BookingResponseDto expected = createResponseDto(
                1L,
                requestDto.getCheckInDateYearMonthDay(),
                requestDto.getDaysOfStay(),
                1L,
                1L,
                "PENDING"
        );

        //When
        String jsonRequest = objectMapper.writeValueAsString(requestDto);
        mockMvc.perform(
                patch("/bookings/{id}", 1)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult resultGet = mockMvc.perform(
                get("/bookings/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //Then
        BookingResponseDto actual = objectMapper.readValue(
                resultGet.getResponse().getContentAsString(), BookingResponseDto.class
        );
        assertTrue(EqualsBuilder.reflectionEquals(expected, actual));
    }

    @Test
    @Sql(scripts = "classpath:database/booking/add-booking-before-delete-method.sql")
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    @DisplayName("Delete by id by valid id, should return nothing")
    void deleteById_ByValidId_ShouldReturnNothing() throws Exception {
        //When
        mockMvc.perform(
                delete("/bookings/{id}", 3))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    private BookingResponseDto createResponseDto(
            Long id, String date, int dayOfStay, Long accommodationId, Long userId, String status
    ) {
        LocalDateTime checkInDate = checkAndParseCheckInDateToLocalDateTime(date);
        LocalDateTime checkOutDate = checkInDate.plusDays(dayOfStay).minusSeconds(1);

        return new BookingResponseDto(
                id,
                checkInDate,
                checkOutDate,
                accommodationId,
                userId,
                status
        );
    }

    private LocalDateTime checkAndParseCheckInDateToLocalDateTime(String date) {
        LocalDate localDate = LocalDate.parse(date, PATTERN_OF_DATE);
        return localDate.atTime(12, 0, 0);
    }

    private static void callSqlQueryFromFile(Connection connection, String fileName) {
        ScriptUtils.executeSqlScript(
                connection,
                new ClassPathResource("database/booking/" + fileName)
        );
    }
}
