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
 * Exercises the setup APIs end to end against a real MongoDB, acting as an ADMIN
 * principal: create a movie, a theater with a screen and seats, then a show, and read its
 * available seats. Also covers the validation, not-found, and conflict error paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSetupApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminId;

    @BeforeEach
    void createAdmin() throws Exception {
        adminId = idOf(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Admin\",\"email\":\"admin+" + System.nanoTime()
                        + "@x.com\",\"role\":\"ADMIN\"}"));
    }

    @Test
    void createsFullSetupAndReadsAvailableSeats() throws Exception {
        String movieId = idOf(asAdmin(post("/api/movies"))
                .content("{\"name\":\"Inception\",\"durationMinutes\":148}"));

        String theaterId = idOf(asAdmin(post("/api/theaters"))
                .content("{\"name\":\"PVR\",\"city\":\"Pune\"}"));

        String screenId = idOf(asAdmin(post("/api/theaters/" + theaterId + "/screens"))
                .content("""
                        {
                          "name": "Screen 1",
                          "seats": [
                            {"number": "A1", "row": "A", "category": "GOLD"},
                            {"number": "A2", "row": "A", "category": "GOLD"}
                          ]
                        }
                        """));

        String showId = idOf(asAdmin(post("/api/shows"))
                .content("{\"movieId\":\"" + movieId + "\",\"theaterId\":\"" + theaterId
                        + "\",\"screenId\":\"" + screenId
                        + "\",\"startTime\":\"2030-01-01T10:00:00Z\",\"durationMinutes\":148}"));

        mockMvc.perform(asAdmin(get("/api/shows/" + showId + "/available-seats")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsInvalidMovie() throws Exception {
        mockMvc.perform(asAdmin(post("/api/movies"))
                        .content("{\"name\":\"\",\"durationMinutes\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void returns404ForUnknownMovie() throws Exception {
        mockMvc.perform(asAdmin(get("/api/movies/does-not-exist")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateSeatOnScreen() throws Exception {
        String theaterId = idOf(asAdmin(post("/api/theaters"))
                .content("{\"name\":\"INOX\",\"city\":\"Pune\"}"));

        String screenId = idOf(asAdmin(post("/api/theaters/" + theaterId + "/screens"))
                .content("{\"name\":\"Screen 1\",\"seats\":[{\"number\":\"A1\",\"category\":\"SILVER\"}]}"));

        mockMvc.perform(asAdmin(post("/api/theaters/" + theaterId + "/screens/" + screenId + "/seats"))
                        .content("{\"seats\":[{\"number\":\"A1\",\"category\":\"SILVER\"}]}"))
                .andExpect(status().isConflict());
    }

    private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
        return builder.header("X-User-Id", adminId).contentType(MediaType.APPLICATION_JSON);
    }

    private String idOf(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
