package demo.hw8.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TavilySearchServiceTests {
    private MockRestServiceServer server;
    private TavilySearchService service;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("https://api.tavily.com");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new TavilySearchService(true, "test-key", builder.build());
    }

    @Test
    void sendsAuthenticatedSearchAndPreservesDuplicateTitles() {
        server.expect(requestTo("https://api.tavily.com/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().json("""
                        {"query":"資料結構","max_results":10,"search_depth":"basic"}
                        """))
                .andRespond(withSuccess("""
                        {"results":[
                          {"title":"相同標題","url":"https://example.org/1","content":"摘要"},
                          {"title":"相同標題","url":"https://example.org/2"},
                          {"title":"危險連結","url":"javascript:alert(1)"},
                          {"title":"無效連結","url":"https:///missing-host"},
                          {"title":"","url":"https://example.org"}
                        ],"response_time":0.1}
                        """, MediaType.APPLICATION_JSON));
        var results = service.search("資料結構");
        assertEquals(2, results.size());
        assertEquals("摘要", results.getFirst().content());
        assertEquals("", results.get(1).content());
        server.verify();
    }

    @Test
    void emptyResultsAreSuccessful() {
        server.expect(requestTo("https://api.tavily.com/search"))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));
        assertTrue(service.search("unknown").isEmpty());
        server.verify();
    }

    @Test
    void missingKeyDoesNotCallApi() {
        var unconfigured = new TavilySearchService(true, "", RestClient.create());
        var error = assertThrows(ResponseStatusException.class, () -> unconfigured.search("Java"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void disabledServiceDoesNotCallApi() {
        var disabled = new TavilySearchService(false, "test-key", RestClient.create());
        assertThrows(ResponseStatusException.class, () -> disabled.search("Java"));
    }

    @Test
    void upstreamAuthenticationErrorDoesNotExposeResponseBody() {
        server.expect(requestTo("https://api.tavily.com/search"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("sensitive upstream detail"));
        var error = assertThrows(ResponseStatusException.class, () -> service.search("Java"));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        assertFalse(error.getReason().contains("sensitive"));
        server.verify();
    }

    @Test
    void malformedResponseIsNotShownAsNoResults() {
        server.expect(requestTo("https://api.tavily.com/search"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        var error = assertThrows(ResponseStatusException.class, () -> service.search("Java"));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
        server.verify();
    }
}
