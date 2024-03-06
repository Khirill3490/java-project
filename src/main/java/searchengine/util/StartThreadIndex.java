package searchengine.util;

import lombok.RequiredArgsConstructor;
import searchengine.models.Site;
import searchengine.models.StatusEnum;
import searchengine.repository.SiteRepository;
import searchengine.services.EntitiesService;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class StartThreadIndex implements Runnable {

    private final EntitiesService entitiesService;
    private final SiteRepository siteRepository;
    private final Connection connection;
    private final searchengine.config.Site siteCfg;


    public static HashSet<String> resultPagesSet = new LinkedHashSet<>();

    @Override
    public void run() {
        ForkJoinPool pool = new ForkJoinPool();
        Site site = getSiteModel(siteCfg, StatusEnum.INDEXING, "");
        HtmlParserFork htmlParserFork = new HtmlParserFork(entitiesService, connection, site, site.getUrl());
        pool.invoke(htmlParserFork);
        pool.shutdown();
        resultPagesSet.clear();
        if (!HtmlParserFork.stop.get()) {
            getSiteModel(siteCfg, StatusEnum.INDEXED, "");
        }
        else {
            getSiteModel(siteCfg, StatusEnum.FAILED, "Индексация остановлена пользователем");
        }
    }

    public Site getSiteModel(searchengine.config.Site siteConfig, StatusEnum statusEnum, String errMessage) {
        Optional<Site> siteOptional = siteRepository.findByName(siteConfig.getName());
        Site site = new Site();
        if (siteOptional.isEmpty()) {
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl().replaceAll("(www.)?", ""));
            site.setStatus(statusEnum);
            site.setLastError(errMessage);
            site.setStatusTime(new Date());
            siteRepository.save(site);
        } else {
            Site siteFromSql = siteOptional.get();
            siteFromSql.setStatus(statusEnum);
            siteFromSql.setLastError(errMessage);
            siteRepository.save(siteFromSql);
        }
        return site;
    }
}
