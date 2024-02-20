package searchengine.services;

import org.springframework.stereotype.Service;

@Service
public interface SearchService {

    Object startSearch(String query, String site, Integer offset, Integer limit);
}
