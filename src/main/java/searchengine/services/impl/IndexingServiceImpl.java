package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.DataNotFoundException;
import searchengine.models.*;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.EntitiesService;
import searchengine.util.HtmlParserFork;
import searchengine.services.IndexingService;
import searchengine.util.StartThreadIndex;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final EntitiesService entitiesService;
    private Pattern pattern = Pattern.compile("(https?://)?([\\w-]+\\.[\\w-]+)[^\\s@]*$");
    private IndexingResponse indexingResponse;

    public IndexingResponse startIndexing() {
        if (isActiveIndexing()) {
            indexingResponse = new IndexingResponse(false, "Индексация уже запущена");
        } else {
            siteRepository.deleteAll();
            indexingResponse = new IndexingResponse(true);
            indexing();
        }
        return indexingResponse;
    }

    public IndexingResponse stopIndexing() {
        if (isActiveIndexing()) {
            HtmlParserFork.stop.set(true);
            indexingResponse = new IndexingResponse(true);
        } else indexingResponse = new IndexingResponse(false, "Индексация не запущена");
        return indexingResponse;

    }
    @Override
    public IndexingResponse indexPage(String url) {
        try {
            if (!isLink(url)) throw new DataNotFoundException("Некорректный URL");
            Connection.Response connection = entitiesService.getConnection(url);
            String content = connection.parse().html();
            if (connection.statusCode() != HttpStatus.OK.value()) {
                throw new BadRequestException("Контент недоступен. Код ответа страницы "
                        + connection.statusCode());
            }
            Site site;
            Optional<Page> pageOptional = pageRepository.findByPath(url);
            if (pageOptional.isPresent()) {
                site = pageOptional.get().getSiteId();
                pageRepository.delete(pageOptional.get());
                Page page = entitiesService.addPage(site, url, content, connection.statusCode());
                entitiesService.findLemmasInPageText(page);
                return new IndexingResponse(true, "Страница проиндексирована");
            }
            else {
                for (searchengine.config.Site siteCfg : sitesList.getSites()) {
                    if (url.contains(siteCfg.getUrl().replaceAll("(www.)?", ""))) {
                        site = entitiesService.updtaeOrAddSite(siteCfg, StatusEnum.INDEXED);
                        Page page = entitiesService.addPage(site, url, content, connection.statusCode());
                        entitiesService.findLemmasInPageText(page);
                        return new IndexingResponse(true, "Страница проиндексирована");
                    }
                }
            }
            return new IndexingResponse(false, "Страница за пределами проиндексированных сайтов");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isActiveIndexing() {
        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if(site.getStatus().name().equals("INDEXING")) return true;
        }
        return false;
    }

    public void indexing() {
        HtmlParserFork.stop.set(false);
        Thread thread = new Thread(() -> {
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (searchengine.config.Site siteCfg : sitesList.getSites()) {
                executorService.execute(new StartThreadIndex(entitiesService, siteRepository, siteCfg));
            }
            executorService.shutdown();
        });
        thread.start();
    }

    public boolean isLink(String url) {
        if (!url.isEmpty() || pattern.matcher(url).find()) return true;
        return false;
    }

}
