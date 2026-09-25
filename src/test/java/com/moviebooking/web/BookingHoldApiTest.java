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

/**
 * Verifies the hold step: holding seats creates a CREATED booking, removes those seats
 * from availability, and blocks a second hold of an overlapping seat.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingHoldApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String showId;
    private String userId;

    @BeforeEach
    void setUpShowAndUser() throws Exception {
        String movieId = idOf(jsonPost("/api/movies", "{\"name\":\"Dune\",\"durationMinutes\":155}"));
        String theaterId = idOf(jsonPost("/api/theaters", "{\"name\":\"IMAX\",\"city\":\"Pune\"}"));
        String screenId = idOf(jsonPost("/api/theaters/" + theaterId + "/screens",
                "{\"name\":\"Audi 1\",\"seats\":["
                        + "{\"number\":\"A1\",\"category\":\"GOLD\"},"
                        + "{\"number\":\"A2\",\"category\":\"GOLD\"},"
                        + "{\"number\":\"A3\",\"category\":\"GOLD\"}]}"));
        showId = idOf(jsonPost("/api/shows", "{\"movieId\":\"" + movieId + "\",\"theaterId\":\"" + theaterId
                + "\",\"screenId\":\"" + screenId
                + "\",\"startTime\":\"2030-07-01T10:00:00Z\",\"durationMinutes\":155}"));
        userId = idOf(jsonPost("/api/users", "{\"name\":\"Ann\",\"email\":\"ann+" + System.nanoTime() + "@x.com\"}"));
    }

    @Test
    void holdCreatesBookingAndRemovesSeatsFromAvailability() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"" + showId + "\",\"userId\":\"" + userId
                                + "\",\"seatIds\":[\"A1\",\"A2\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.seatIds.length()").value(2));

        // A1/A2 held -> only A3 remains available.
        mockMvc.perform(get("/api/shows/" + showId + "/available-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("A3"));

        // Booking shows up in the user's history.
        mockMvc.perform(get("/api/bookings").param("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void secondHoldOfHeldSeatConflicts() throws Exception {
        hold("A1", "A2");

        String otherUser = idOf(jsonPost("/api/users",
                "{\"name\":\"Bob\",\"email\":\"bob+" + System.nanoTime() + "@x.com\"}"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"" + showId + "\",\"userId\":\"" + otherUser
                                + "\",\"seatIds\":[\"A2\",\"A3\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void holdingUnknownSeatReturns404() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"" + showId + "\",\"userId\":\"" + userId
                                + "\",\"seatIds\":[\"Z9\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateSeatIdsRejectedAs400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"" + showId + "\",\"userId\":\"" + userId
                                + "\",\"seatIds\":[\"A1\",\"A1\"]}"))
                .andExpect(status().isBadRequest());
    }

    private void hold(String... seats) throws Exception {
        String seatArray = "\"" + String.join("\",\"", seats) + "\"";
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"showId\":\"" + showId + "\",\"userId\":\"" + userId
                                + "\",\"seatIds\":[" + seatArray + "]}"))
                .andExpect(status().isCreated());
    }

    private String idOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder jsonPost(String url,
            String body) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
