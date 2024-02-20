package searchengine.util;

import lombok.RequiredArgsConstructor;
import searchengine.models.Site;
import searchengine.models.StatusEnum;
import searchengine.repository.SiteRepository;
import searchengine.services.EntitiesService;
import searchengine.services.impl.IndexingServiceImpl;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
@RequiredArgsConstructor
public class StartThreadIndex implements Runnable {

    private final EntitiesService entitiesService;
    private final SiteRepository siteRepository;
    private final searchengine.config.Site siteCfg;

    public static HashSet<String> resultPagesSet = new LinkedHashSet<>();

    @Override
    public void run() {
        ForkJoinPool pool = new ForkJoinPool();
        Site site = getSiteModel(siteCfg, StatusEnum.INDEXING);
        HtmlParserFork htmlParserFork = new HtmlParserFork(entitiesService, site, site.getUrl());
        pool.invoke(htmlParserFork);
        htmlParserFork.join();
        resultPagesSet.clear();
//            HtmlParserFork.resultPagesSet.clear();
        getSiteModel(siteCfg, StatusEnum.INDEXED);
    }

    public Site getSiteModel(searchengine.config.Site siteConfig, StatusEnum statusEnum) {
        Optional<Site> siteOptional = siteRepository.findByName(siteConfig.getName());
        Site site = new Site();
        if (!siteOptional.isPresent()) {
            site.setName(siteConfig.getName());
            site.setUrl(siteConfig.getUrl().replaceAll("(www.)?", ""));
            site.setStatus(statusEnum);
            site.setLastError("");
            site.setStatusTime(new Date());
            siteRepository.save(site);
        } else {
            Site siteFromSql = siteOptional.get();
            siteFromSql.setStatus(statusEnum);
            siteRepository.save(siteFromSql);
        }
        return site;
    }
}
