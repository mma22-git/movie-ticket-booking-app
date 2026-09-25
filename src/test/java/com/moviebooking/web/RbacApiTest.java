package com.moviebooking.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Focused checks on the role-authorization interceptor: missing principal, wrong role,
 * and correct role, plus that a public endpoint needs no principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RbacApiTest {

    private static final String MOVIE_BODY = "{\"name\":\"X\",\"durationMinutes\":100}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminId;
    private String customerId;

    @BeforeEach
    void setUp() throws Exception {
        adminId = idOf(user("Admin", "ADMIN"));
        customerId = idOf(user("Cust", "CUSTOMER"));
    }

    @Test
    void adminEndpointWithoutPrincipalIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/movies").contentType(MediaType.APPLICATION_JSON).content(MOVIE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownPrincipalIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/movies").header("X-User-Id", "nobody")
                        .contentType(MediaType.APPLICATION_JSON).content(MOVIE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotUseAdminEndpoint() throws Exception {
        mockMvc.perform(post("/api/movies").header("X-User-Id", customerId)
                        .contentType(MediaType.APPLICATION_JSON).content(MOVIE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminEndpoint() throws Exception {
        mockMvc.perform(post("/api/movies").header("X-User-Id", adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(MOVIE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void publicEndpointNeedsNoPrincipal() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder user(String name, String role) {
        return post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"email\":\"" + name.toLowerCase() + "+"
                        + System.nanoTime() + "@x.com\",\"role\":\"" + role + "\"}");
    }

    private String idOf(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }
}
