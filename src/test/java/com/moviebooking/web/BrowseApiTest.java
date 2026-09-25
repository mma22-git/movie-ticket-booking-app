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

import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.TheaterRepository;

/**
 * Walks the customer browse flow: search a movie by name, find the theaters screening
 * it, then list its showtimes at a chosen theater.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BrowseApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowRepository showRepository;

    @BeforeEach
    void clean() {
        // Isolate this flow from data left by other tests so counts are deterministic.
        showRepository.deleteAll();
        movieRepository.deleteAll();
        theaterRepository.deleteAll();
    }

    @Test
    void searchThenFindTheatersThenListShowtimes() throws Exception {
        String movieId = idOf(mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"The Dark Knight\",\"durationMinutes\":152}"))
                .andReturn());

        String theaterId = idOf(mockMvc.perform(post("/api/theaters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cinepolis\",\"city\":\"Mumbai\"}"))
                .andReturn());

        String screenId = idOf(mockMvc.perform(post("/api/theaters/" + theaterId + "/screens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Audi 1\",\"seats\":[{\"number\":\"A1\",\"category\":\"GOLD\"}]}"))
                .andReturn());

        mockMvc.perform(post("/api/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":\"" + movieId + "\",\"theaterId\":\"" + theaterId
                                + "\",\"screenId\":\"" + screenId
                                + "\",\"startTime\":\"2030-06-01T14:00:00Z\",\"durationMinutes\":152}"))
                .andExpect(status().isCreated());

        // 1. Search by partial, case-insensitive name.
        mockMvc.perform(get("/api/movies").param("name", "dark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("The Dark Knight"));

        // 2. Theaters screening the movie (filtered by city).
        mockMvc.perform(get("/api/movies/" + movieId + "/theaters").param("city", "Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cinepolis"))
                .andExpect(jsonPath("$[0].city").value("Mumbai"));

        // A non-matching city returns nothing.
        mockMvc.perform(get("/api/movies/" + movieId + "/theaters").param("city", "Delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 3. Showtimes for the movie at the theater.
        mockMvc.perform(get("/api/shows").param("movieId", movieId).param("theaterId", theaterId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].screenId").value(screenId));
    }

    private String idOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
