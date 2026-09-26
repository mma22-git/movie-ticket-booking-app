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

import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.TheaterRepository;

/**
 * Walks the customer browse flow: search a movie by name, find the theaters screening it,
 * then list its showtimes. Catalog is set up as ADMIN; browsing as a CUSTOMER.
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

    private String adminId;
    private String customerId;

    @BeforeEach
    void setUp() throws Exception {
        showRepository.deleteAll();
        movieRepository.deleteAll();
        theaterRepository.deleteAll();

        adminId = idOf(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Admin\",\"email\":\"admin+" + System.nanoTime() + "@x.com\",\"role\":\"ADMIN\"}"));
        customerId = idOf(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Cust\",\"email\":\"cust+" + System.nanoTime() + "@x.com\"}"));
    }

    @Test
    void searchThenFindTheatersThenListShowtimes() throws Exception {
        String movieId = idOf(asAdmin(post("/api/movies"))
                .content("{\"name\":\"The Dark Knight\",\"durationMinutes\":152}"));
        String theaterId = idOf(asAdmin(post("/api/theaters"))
                .content("{\"name\":\"Cinepolis\",\"city\":\"Mumbai\"}"));
        String screenId = idOf(asAdmin(post("/api/theaters/" + theaterId + "/screens"))
                .content("{\"name\":\"Audi 1\",\"seats\":[{\"number\":\"A1\",\"category\":\"GOLD\"}]}"));
        mockMvc.perform(asAdmin(post("/api/shows"))
                        .content("{\"movieId\":\"" + movieId + "\",\"theaterId\":\"" + theaterId
                                + "\",\"screenId\":\"" + screenId
                                + "\",\"startTime\":\"2030-06-01T14:00:00Z\",\"durationMinutes\":152}"))
                .andExpect(status().isCreated());

        // 1. Search by partial, case-insensitive name (as a customer).
        mockMvc.perform(get("/api/movies").param("name", "dark").header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("The Dark Knight"));

        // 2. Theaters screening the movie (filtered by city).
        mockMvc.perform(get("/api/movies/" + movieId + "/theaters").param("city", "Mumbai")
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cinepolis"));

        mockMvc.perform(get("/api/movies/" + movieId + "/theaters").param("city", "Delhi")
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 3. Showtimes for the movie at the theater.
        mockMvc.perform(get("/api/shows").param("movieId", movieId).param("theaterId", theaterId)
                        .header("X-User-Id", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].screenId").value(screenId));
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
