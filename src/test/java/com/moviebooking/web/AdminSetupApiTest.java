package com.moviebooking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises the setup APIs end to end against a real MongoDB: create a movie, a theater
 * with a screen and seats, then a show, and read its available seats. Also covers the
 * validation, not-found, and conflict error paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSetupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsFullSetupAndReadsAvailableSeats() throws Exception {
        String movieId = idOf(mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Inception\",\"durationMinutes\":148}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Inception"))
                .andReturn());

        MvcResult theaterResult = mockMvc.perform(post("/api/theaters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PVR\",\"city\":\"Pune\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String theaterId = idOf(theaterResult);

        String screenId = idOf(mockMvc.perform(post("/api/theaters/" + theaterId + "/screens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Screen 1",
                                  "seats": [
                                    {"number": "A1", "row": "A", "category": "GOLD"},
                                    {"number": "A2", "row": "A", "category": "GOLD"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andReturn());

        String showId = idOf(mockMvc.perform(post("/api/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":\"" + movieId + "\",\"theaterId\":\"" + theaterId
                                + "\",\"screenId\":\"" + screenId
                                + "\",\"startTime\":\"2030-01-01T10:00:00Z\",\"durationMinutes\":148}"))
                .andExpect(status().isCreated())
                .andReturn());

        // No bookings yet, so both seats are available.
        mockMvc.perform(get("/api/shows/" + showId + "/available-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsInvalidMovie() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"durationMinutes\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void returns404ForUnknownMovie() throws Exception {
        mockMvc.perform(get("/api/movies/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateSeatOnScreen() throws Exception {
        String theaterId = idOf(mockMvc.perform(post("/api/theaters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"INOX\",\"city\":\"Pune\"}"))
                .andReturn());

        String screenId = idOf(mockMvc.perform(post("/api/theaters/" + theaterId + "/screens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Screen 1\",\"seats\":[{\"number\":\"A1\",\"category\":\"SILVER\"}]}"))
                .andReturn());

        mockMvc.perform(post("/api/theaters/" + theaterId + "/screens/" + screenId + "/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seats\":[{\"number\":\"A1\",\"category\":\"SILVER\"}]}"))
                .andExpect(status().isConflict());
    }

    private String idOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
