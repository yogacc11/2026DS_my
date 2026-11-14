package demo.hw8.controller;

import demo.hw8.model.SearchResult;
import demo.hw8.service.TavilySearchService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SearchController {
    private final TavilySearchService service;

    public SearchController(TavilySearchService service) {
        this.service = service;
    }

    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam(defaultValue = "") String q) {
        String query = q.strip();
        if (query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "請輸入搜尋關鍵字。");
        }
        if (query.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "搜尋關鍵字請勿超過 200 個字元。");
        }
        return new SearchResponse(query, service.search(query));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleSearchError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason() == null ? "搜尋失敗，請稍後再試。" : ex.getReason()));
    }

    public record SearchResponse(String query, List<SearchResult> results) {}
}
