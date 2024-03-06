package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.searching.SearchingResponse;

@Service
public interface SearchService {
    SearchingResponse startSearch(String query, String site, Integer offset, Integer limit);
}
