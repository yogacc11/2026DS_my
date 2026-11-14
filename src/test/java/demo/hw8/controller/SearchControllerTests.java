package demo.hw8.controller;

import demo.hw8.model.SearchResult;
import demo.hw8.service.TavilySearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerTests {
    private TavilySearchService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(TavilySearchService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SearchController(service)).build();
    }

    @Test
    void rejectsMissingBlankAndLongInputWithoutCallingTavily() throws Exception {
        mvc.perform(get("/api/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/search").param("q", "   "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/api/search").param("q", "a".repeat(201)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void trimsQueryAndReturnsResults() throws Exception {
        when(service.search("資料結構")).thenReturn(List.of(new SearchResult("標題", "https://example.org", "摘要")));
        mvc.perform(get("/api/search").param("q", " 資料結構 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("資料結構"))
                .andExpect(jsonPath("$.results[0].title").value("標題"));
    }

    @Test
    void returnsActionableServiceError() throws Exception {
        when(service.search("Java")).thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "連線逾時"));
        mvc.perform(get("/api/search").param("q", "Java"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.message").value("連線逾時"));
    }
}
