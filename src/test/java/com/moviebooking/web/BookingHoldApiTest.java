package com.moviebooking.web;

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

/**
 * Verifies the hold step: a customer holding seats creates a CREATED booking, removes
 * those seats from availability, and blocks a second hold of an overlapping seat.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingHoldApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String showId;
    private String customerId;
    private String adminId;

    @BeforeEach
    void setUp() throws Exception {
        adminId = idOf(user("Admin", "ADMIN"));
        customerId = idOf(user("Ann", "CUSTOMER"));

        String movieId = idOf(asAdmin(post("/api/movies")).content("{\"name\":\"Dune\",\"durationMinutes\":155}"));
        String theaterId = idOf(asAdmin(post("/api/theaters")).content("{\"name\":\"IMAX\",\"city\":\"Pune\"}"));
        String screenId = idOf(asAdmin(post("/api/theaters/" + theaterId + "/screens"))
                .content("{\"name\":\"Audi 1\",\"seats\":["
                        + "{\"number\":\"A1\",\"category\":\"GOLD\"},"
                        + "{\"number\":\"A2\",\"category\":\"GOLD\"},"
                        + "{\"number\":\"A3\",\"category\":\"GOLD\"}]}"));
        showId = idOf(asAdmin(post("/api/shows")).content("{\"movieId\":\"" + movieId + "\",\"theaterId\":\""
                + theaterId + "\",\"screenId\":\"" + screenId
                + "\",\"startTime\":\"2030-07-01T10:00:00Z\",\"durationMinutes\":155}"));
    }

    @Test
    void holdCreatesBookingAndRemovesSeatsFromAvailability() throws Exception {
        mockMvc.perform(asCustomer(post("/api/bookings"), customerId)
                        .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A1\",\"A2\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.seatIds.length()").value(2));

        mockMvc.perform(asCustomer(get("/api/shows/" + showId + "/available-seats"), customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("A3"));

        mockMvc.perform(asCustomer(get("/api/bookings"), customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void secondHoldOfHeldSeatConflicts() throws Exception {
        mockMvc.perform(asCustomer(post("/api/bookings"), customerId)
                        .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A1\",\"A2\"]}"))
                .andExpect(status().isCreated());

        String otherCustomer = idOf(user("Bob", "CUSTOMER"));
        mockMvc.perform(asCustomer(post("/api/bookings"), otherCustomer)
                        .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A2\",\"A3\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void holdingUnknownSeatReturns404() throws Exception {
        mockMvc.perform(asCustomer(post("/api/bookings"), customerId)
                        .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"Z9\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateSeatIdsRejectedAs400() throws Exception {
        mockMvc.perform(asCustomer(post("/api/bookings"), customerId)
                        .content("{\"showId\":\"" + showId + "\",\"seatIds\":[\"A1\",\"A1\"]}"))
                .andExpect(status().isBadRequest());
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
