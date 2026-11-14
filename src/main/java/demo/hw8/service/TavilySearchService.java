package demo.hw8.service;

import demo.hw8.model.SearchResult;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TavilySearchService {
    private final boolean enabled;
    private final String apiKey;
    private final RestClient client;

    @Autowired
    public TavilySearchService(@Value("${tavily.enabled:false}") boolean enabled,
                              @Value("${tavily.apiKey:}") String apiKey) {
        this(enabled, apiKey, defaultClient());
    }

    // Package access allows tests to use a simulated HTTP server without API credits.
    TavilySearchService(boolean enabled, String apiKey, RestClient client) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.client = client;
    }

    private static RestClient defaultClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().baseUrl("https://api.tavily.com")
                .requestFactory(factory).build();
    }

    public List<SearchResult> search(String query) {
        if (!enabled || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "搜尋服務尚未啟用或未設定 API 金鑰，請聯絡網站管理者。");
        }
        try {
            TavilyResponse response = client.post().uri("/search")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", query, "max_results", 10, "search_depth", "basic"))
                    .retrieve().body(TavilyResponse.class);
            if (response == null || response.results() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "搜尋服務回傳格式異常，請稍後再試。");
            }
            return response.results().stream()
                    .filter(item -> item != null && item.title() != null && !item.title().isBlank())
                    .filter(item -> validUrl(item.url()))
                    .map(item -> new SearchResult(item.title(), item.url(),
                            item.content() == null ? "" : item.content()))
                    .limit(10).toList();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            String message = switch (status) {
                case 401, 403 -> "搜尋服務驗證失敗，請聯絡網站管理者檢查 API 金鑰。";
                case 429, 432, 433 -> "搜尋服務目前額度不足或請求過於頻繁，請稍後再試。";
                default -> "搜尋服務暫時無法使用，請稍後再試。";
            };
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "搜尋連線逾時或無法連線，請稍後再試。");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "搜尋服務回傳格式異常，請稍後再試。");
        }
    }

    private static boolean validUrl(String url) {
        try {
            URI uri = URI.create(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException | NullPointerException ex) {
            return false;
        }
    }

    public record TavilyResponse(List<SearchResult> results) {}
}
