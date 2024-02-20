package searchengine.services.impl;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.models.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.EntitiesService;
import searchengine.util.FindLemmas;
import searchengine.util.HtmlParserFork;
import searchengine.services.IndexingService;
import searchengine.util.StartThreadIndex;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Getter
    private static AtomicBoolean stop = new AtomicBoolean(false);


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
            stop.set(true);
            indexingResponse = new IndexingResponse(true);
        } else indexingResponse = new IndexingResponse(false, "Индексация не запущена");
        return indexingResponse;

    }
    @Override
    public IndexingResponse indexPage(String url) {
        try {
            if (!isLink(url)) return new IndexingResponse(false, "Некорректно введен URL");
            Connection.Response connection = entitiesService.getConnection(url);
            String content = connection.parse().html();
            if (connection.statusCode() != HttpStatus.OK.value()) {
                return new IndexingResponse(false, "Контент недоступен. Код ответа страницы "
                        + connection.statusCode());
            }
            Optional<Page> pageOptional = pageRepository.findByPath(url);
            for (searchengine.config.Site siteCfg: sitesList.getSites()) {
                Optional<Site> siteOptional = siteRepository.findByName(siteCfg.getName());
                Site site;
                if (siteOptional.isEmpty()) site = entitiesService.addSite(siteCfg, StatusEnum.INDEXED);
                else site = siteOptional.get();
                if (url.contains(site.getUrl()) && pageOptional.isEmpty()) {
                    entitiesService.addPage(site, url, content, connection.statusCode());
                    entitiesService.parseLink(site, url);
                    return new IndexingResponse(true);
                } else if (url.contains(site.getUrl()) && pageOptional.isPresent()) {
                    pageRepository.delete(pageOptional.get());
                    entitiesService.addPage(site, url, content, connection.statusCode());
                    entitiesService.parseLink(site, url);
                    return new IndexingResponse(true);
                }
            }
            return new IndexingResponse(false, "Страница за пределами сайта");
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
        for (searchengine.config.Site siteCfg: sitesList.getSites()) {
            executorService.execute(new StartThreadIndex(entitiesService, siteRepository, siteCfg));
//            ForkJoinPool pool = new ForkJoinPool();
//            Site site = getSiteModel(siteCfg, StatusEnum.INDEXING);
//            HtmlParserFork htmlParserFork = new HtmlParserFork(entitiesService, site, site.getUrl());
//            pool.invoke(htmlParserFork);
//            htmlParserFork.join();
//            HtmlParserFork.resultPagesSet.clear();
////            HtmlParserFork.resultPagesSet.clear();
//            getSiteModel(siteCfg, StatusEnum.INDEXED);
//            HtmlParserFork.stop.set(false);
        }
        executorService.shutdown();
        setStop(false);
    }

    public boolean isLink(String url) {
        if (!url.isEmpty() || pattern.matcher(url).find()) return true;
        return false;
    }

    public static void setStop(boolean a) {
        stop.set(a);
    }

}
