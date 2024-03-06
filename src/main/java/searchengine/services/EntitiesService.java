package searchengine.services;

import searchengine.models.Page;
import searchengine.models.Site;

import java.util.HashMap;

public interface EntitiesService {

    Page addPage(Site site, String pageUrl, String content, int i);

    void addLemmaAndIndex(Page page, HashMap<String, Integer> lemmas);

    void findLemmasInPageText(Page page);

}
