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
 * Verifies step two: paying for a held booking confirms it, persists one booked-seat row
 * per seat, and frees the hold — plus the ownership, status, and payment guards.
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
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        String movieId = idOf(jsonPost("/api/movies", "{\"name\":\"Tenet\",\"durationMinutes\":150}"));
        String theaterId = idOf(jsonPost("/api/theaters", "{\"name\":\"PVR\",\"city\":\"Pune\"}"));
        String screenId = idOf(jsonPost("/api/theaters/" + theaterId + "/screens",
                "{\"name\":\"Audi 1\",\"seats\":[{\"number\":\"A1\",\"category\":\"GOLD\"},"
                        + "{\"number\":\"A2\",\"category\":\"GOLD\"}]}"));
        showId = idOf(jsonPost("/api/shows", "{\"movieId\":\"" + movieId + "\",\"theaterId\":\"" + theaterId
                + "\",\"screenId\":\"" + screenId
                + "\",\"startTime\":\"2030-08-01T10:00:00Z\",\"durationMinutes\":150}"));
        userId = idOf(jsonPost("/api/users", "{\"name\":\"Ann\",\"email\":\"ann+" + System.nanoTime() + "@x.com\"}"));
    }

    @Test
    void payConfirmsBookingPersistsSeatsAndFreesHold() throws Exception {
        String bookingId = idOf(jsonPost("/api/bookings",
                "{\"showId\":\"" + showId + "\",\"userId\":\"" + userId + "\",\"seatIds\":[\"A1\",\"A2\"]}"));

        mockMvc.perform(jsonPost("/api/bookings/" + bookingId + "/payment",
                        "{\"userId\":\"" + userId + "\",\"paymentMethod\":\"MOCK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // One booked-seat row per seat is now persisted.
        assertThat(bookedSeatRepository.findByShowId(showId)).hasSize(2);

        // Seats stay unavailable after confirmation (now via the persisted rows, not the hold).
        mockMvc.perform(get("/api/shows/" + showId + "/available-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void confirmingTwiceIsRejected() throws Exception {
        String bookingId = idOf(jsonPost("/api/bookings",
                "{\"showId\":\"" + showId + "\",\"userId\":\"" + userId + "\",\"seatIds\":[\"A1\"]}"));

        mockMvc.perform(jsonPost("/api/bookings/" + bookingId + "/payment", "{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isOk());

        // Already CONFIRMED -> not awaiting confirmation.
        mockMvc.perform(jsonPost("/api/bookings/" + bookingId + "/payment", "{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void payingForAnotherUsersBookingIsForbidden() throws Exception {
        String bookingId = idOf(jsonPost("/api/bookings",
                "{\"showId\":\"" + showId + "\",\"userId\":\"" + userId + "\",\"seatIds\":[\"A1\"]}"));
        String otherUser = idOf(jsonPost("/api/users",
                "{\"name\":\"Bob\",\"email\":\"bob+" + System.nanoTime() + "@x.com\"}"));

        mockMvc.perform(jsonPost("/api/bookings/" + bookingId + "/payment", "{\"userId\":\"" + otherUser + "\"}"))
                .andExpect(status().isForbidden());
    }

    private String idOf(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    private MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
