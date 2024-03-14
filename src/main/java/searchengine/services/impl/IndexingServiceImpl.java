package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import searchengine.util.HtmlParserFork;
import searchengine.services.IndexingService;
import searchengine.util.PageUtil;
import searchengine.util.StartThreadIndex;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sitesList;
    private final PageUtil pageUtil;
    private final searchengine.util.Connection connection;
    private final Pattern pattern = Pattern.compile("(https?://)?([\\w-]+\\.[\\w-]+)[^\\s@]*$");
    private IndexingResponse indexingResponse;

    public IndexingResponse startIndexing() {
        if (isActiveIndexing()) {
            indexingResponse = new IndexingResponse(false, "Индексация уже запущена");
        } else {
            siteRepository.deleteAll();
            indexingResponse = new IndexingResponse(true);
            log.info("Запуск индексации пользователем");
            indexing();
        }
        return indexingResponse;
    }

    public IndexingResponse stopIndexing() {
        if (isActiveIndexing()) {
            HtmlParserFork.stop.set(true);
            indexingResponse = new IndexingResponse(true);
            log.info("Остановка индексации пользователем");
        } else {
            indexingResponse = new IndexingResponse(false, "Индексация не запущена");
        }
        return indexingResponse;

    }
    @Override
    public IndexingResponse indexPage(String url) {
        try {
            if (!isLink(url)) {
                throw new DataNotFoundException("Некорректный URL");
            }
            Connection.Response connect = connection.getConnection(url);
            String content = connect.parse().html();
            if (connect.statusCode() != HttpStatus.OK.value()) {
                throw new BadRequestException("Контент недоступен. Код ответа страницы "
                        + connect.statusCode());
            }
            Site site;
            Optional<Page> pageOptional = pageRepository.findByPath(url);
            if (pageOptional.isPresent()) {
                site = pageOptional.get().getSiteId();
                pageRepository.delete(pageOptional.get());
                Page page = pageUtil.addPage(site, url, content, connect.statusCode());
                pageUtil.findLemmasInPageText(page);
                log.info("Страница " + page.getPath() + " проиндексирована");
                return new IndexingResponse(true, "Страница проиндексирована");
            }
            else {
                for (searchengine.config.Site siteCfg : sitesList.getSites()) {
                    if (url.contains(siteCfg.getUrl().replaceAll("(www.)?", ""))) {
                        site = updateOrAddSite(siteCfg, StatusEnum.INDEXED);
                        Page page = pageUtil.addPage(site, url, content, connect.statusCode());
                        pageUtil.findLemmasInPageText(page);
                        log.info("Страница " + page.getPath() + " проиндексирована");
                        return new IndexingResponse(true, "Страница проиндексирована");
                    }
                }
            }
            log.error("Ссылка " + url + " не проидексирована. За пределами сайта");
            return new IndexingResponse(false, "Страница за пределами проиндексированных сайтов");
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean isActiveIndexing() {
        List<Site> siteList = siteRepository.findAll();
        for (Site site : siteList) {
            if(site.getStatus().name().equals("INDEXING")) {
                return true;
            }
        }
        return false;
    }

    public void indexing() {
        HtmlParserFork.stop.set(false);
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (searchengine.config.Site siteCfg : sitesList.getSites()) {
            try {
                executorService.execute(new StartThreadIndex(pageUtil, siteRepository, connection, siteCfg));
            } catch (Exception e) {
                log.error("Ошибка при выполнении потока: " + e.getMessage());
                e.printStackTrace();
            }
        }
        executorService.shutdown();
    }

    public Site updateOrAddSite(searchengine.config.Site siteCfg, StatusEnum statusEnum) {
        Site site;
        Optional<Site> siteOptional  = siteRepository.findByName(siteCfg.getName());
        if (siteOptional.isPresent()) {
            site = siteOptional.get();
        } else  {
            site = new Site();
            site.setName(siteCfg.getName());
            site.setUrl(siteCfg.getUrl().replaceAll("(www.)?", ""));
            site.setLastError("");
        }
        site.setStatus(statusEnum);
        site.setStatusTime(new Date());
        siteRepository.save(site);
        log.info("Сайт " + site.getUrl() + " добавлен в БД");
        return site;
    }

    public boolean isLink(String url) {
        return !url.isEmpty() || pattern.matcher(url).find();
    }

}
