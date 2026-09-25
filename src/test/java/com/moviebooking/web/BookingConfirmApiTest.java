package com.moviebooking.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.moviebooking.repository.BookedSeatRepository;

/**
 * Verifies step two: a customer paying for their held booking confirms it, persists one
 * booked-seat row per seat, and frees the hold — plus the status and ownership guards.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingConfirmApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookedSeatRepository bookedSeatRepository;

    private String showId;
    private String customerId;
    private String adminId;

    @BeforeEach
    void setUp() throws Exception {
        adminId = idOf(user("Admin", "ADMIN"));
        customerId = idOf(user("Ann", "CUSTOMER"));

        String movieId = idOf(asAdmin(post("/api/movies")).content("{\"name\":\"Tenet\",\"durationMinutes\":150}"));
        String theaterId = idOf(asAdmin(post("/api/theaters")).content("{\"name\":\"PVR\",\"city\":\"Pune\"}"));
        String screenId = idOf(asAdmin(post("/api/theaters/" + theaterId + "/screens"))
                .content("{\"name\":\"Audi 1\",\"seats\":[{\"number\":\"A1\",\"category\":\"GOLD\"},"
                        + "{\"number\":\"A2\",\"category\":\"GOLD\"}]}"));
        showId = idOf(asAdmin(post("/api/shows")).content("{\"movieId\":\"" + movieId + "\",\"theaterId\":\""
                + theaterId + "\",\"screenId\":\"" + screenId
                + "\",\"startTime\":\"2030-08-01T10:00:00Z\",\"durationMinutes\":150}"));
    }

    @Test
    void payConfirmsBookingPersistsSeatsAndFreesHold() throws Exception {
        String bookingId = idOf(asCustomer(post("/api/bookings"), customerId)
                .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A1\",\"A2\"]}"));

        mockMvc.perform(asCustomer(post("/api/bookings/" + bookingId + "/payment"), customerId)
                        .content("{\"paymentMethod\":\"MOCK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(bookedSeatRepository.findByShowId(showId)).hasSize(2);

        mockMvc.perform(asCustomer(get("/api/shows/" + showId + "/available-seats"), customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void confirmingTwiceIsRejected() throws Exception {
        String bookingId = idOf(asCustomer(post("/api/bookings"), customerId)
                .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A1\"]}"));

        mockMvc.perform(asCustomer(post("/api/bookings/" + bookingId + "/payment"), customerId)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(asCustomer(post("/api/bookings/" + bookingId + "/payment"), customerId)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void payingForAnotherUsersBookingIsForbidden() throws Exception {
        String bookingId = idOf(asCustomer(post("/api/bookings"), customerId)
                .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A1\"]}"));
        String otherCustomer = idOf(user("Bob", "CUSTOMER"));

        mockMvc.perform(asCustomer(post("/api/bookings/" + bookingId + "/payment"), otherCustomer)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder user(String name, String role) {
        return post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"email\":\"" + name.toLowerCase() + "+"
                        + System.nanoTime() + "@x.com\",\"role\":\"" + role + "\"}");
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        return builder.header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder asCustomer(MockHttpServletRequestBuilder builder, String customer) {
        return builder.header("X-User-Id", customer).contentType(MediaType.APPLICATION_JSON);
    }

    private String idOf(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
