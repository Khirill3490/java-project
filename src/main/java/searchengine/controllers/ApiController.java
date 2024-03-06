package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse indexingResponse = indexingService.startIndexing();
        return ResponseEntity.ok(indexingResponse);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse indexingResponse = indexingService.stopIndexing();
        return ResponseEntity.ok(indexingResponse);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam("url") String url) {
        IndexingResponse indexingResponse = indexingService.indexPage(url);
        return ResponseEntity.ok(indexingResponse);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search(@RequestParam(value = "query", required = false) String query,
                                                    @RequestParam(value = "site", required = false) String site,
                                                    @RequestParam(value = "offset", required = false) Integer offset,
                                                    @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(searchService.startSearch(query, site, offset, limit));
    }

}
