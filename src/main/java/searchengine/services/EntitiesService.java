package searchengine.services;

import org.jsoup.Connection;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.models.StatusEnum;

import java.util.HashMap;

public interface EntitiesService {
    Site addSite(searchengine.config.Site siteCfg, StatusEnum statusEnum);

    Page addPage(Site site, String pageUrl, String content, int i);

    void addLemmaAndIndex(Page page, HashMap<String, Integer> lemmas);

    boolean isLink(String link);

    void parseLink(Site site, String pageUrl);

    Connection.Response getConnection(String url);
}
