package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.models.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.EntitiesService;
import searchengine.util.FindLemmas;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EntitiesServiceImpl implements EntitiesService {

    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final FindLemmas findLemmas;

    public static HashSet<String> pageSet = new LinkedHashSet<>();
    private Pattern pattern = Pattern.compile("(https?://)?([\\w-]+\\.[\\w-]+)[^\\s@]*$");


    @Override
    public Site updtaeOrAddSite(searchengine.config.Site siteCfg, StatusEnum statusEnum) {
        Site site;
        Optional<Site> siteOptional  = siteRepository.findByName(siteCfg.getName());
        if (siteOptional.isPresent()) {
            site = siteOptional.get();
            site.setStatus(statusEnum);
            site.setStatusTime(new Date());
            siteRepository.save(site);
            return site;
        }
        site = new Site();
        site.setName(siteCfg.getName());
        site.setUrl(siteCfg.getUrl().replaceAll("(www.)?", ""));
        site.setLastError("");
        site.setStatus(statusEnum);
        site.setStatusTime(new Date());
        siteRepository.save(site);
        return site;
    }

    @Override
    public Page addPage(Site site, String pageUrl, String content, int statusCode) {
        Page page = new Page();
        page.setPath(pageUrl);
        page.setContent(content);
        page.setSiteId(site);
        page.setCode(statusCode);
        pageRepository.save(page);
        return page;
    }

    public void addLemmaAndIndex(Page page, HashMap<String, Integer> lemmas) {
        synchronized (lemmaRepository) {
            for (String s : lemmas.keySet()) {
                Index index = new Index();
                index.setPageId(page);
                Optional<Lemma> lemmaFromSql = lemmaRepository.findByLemmaAndSiteId(s, page.getSiteId());
//                Проверить логику работы поиска леммы лист или опшнл
                if (!lemmaFromSql.isPresent()) {
                    Lemma lemma = new Lemma();
                    lemma.setLemma(s);
                    lemma.setFrequency(lemmas.get(s));
                    lemma.setSiteId(page.getSiteId());
                    lemmaRepository.save(lemma);
                    index.setLemmaId(lemma);
                    index.setRank(Float.valueOf(lemma.getFrequency()));
                } else {
                    Lemma lemmaObj = lemmaFromSql.get();
                    lemmaObj.setFrequency(lemmaObj.getFrequency() + lemmas.get(s));
                    lemmaRepository.save(lemmaObj);
                    index.setLemmaId(lemmaObj);
                    index.setRank(Float.valueOf(lemmas.get(s)));
                }
                indexRepository.saveAndFlush(index);
            }
        }
    }

    @Override
    public boolean isLink(String link) {
//        Исправить проверку на ссылку
        if (link.contains(".pdf") || link.contains(".jpg")
                || link.contains("%") || link.contains("#")
                || link.contains(".png") || link.isEmpty()
                || !pattern.matcher(link).find() || link.contains("mailto:")) return false;
        return true;
    }

    @Override
    public void parseLink(Site site, String pageUrl) {
        pageSet.add(pageUrl);
        try {
            Connection.Response connection = getConnection(pageUrl);
            Document document = connection.parse();
            Elements elements = document.select("a");
            for (Element element: elements) {
                try {
                    String link = element.attr("href").replaceAll("$/$", "");
                    if (pageSet.contains(link) || !isLink(link)) continue;
                    Connection.Response connection1 = getConnection(link);
                    if (connection1.statusCode() != HttpStatus.OK.value()) continue;
                    Document document1 = connection1.parse();
                    if (!link.equals(site.getUrl()) &&
                            (link.startsWith(site.getUrl()) || link.startsWith("/"))) {
                        pageSet.add(link);
                        Page page = addPage(site, link, document1.html(), connection1.statusCode());
                        FindLemmas findLemmas = new FindLemmas();
                        String htmlContent = page.getContent();
                        HashMap<String, Integer> lemmaList = findLemmas.getLemmasInMap(htmlContent);
                        addLemmaAndIndex(page, lemmaList);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Завершено");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void findLemmasInPageText(Page page) {
        Document document = Jsoup.parse(page.getContent());
        String text = document.title() + " " + document.body().text();
        HashMap<String, Integer> lemmaList = findLemmas.getLemmasInMap(text);
        addLemmaAndIndex(page, lemmaList);
    }

    @Override
    public Connection.Response getConnection(String url) {
        Connection.Response connection;
        synchronized (pageRepository) {
            try {
                Thread.sleep(600);
                connection = Jsoup.connect(url)
                        .ignoreHttpErrors(true)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                                " (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36")
                        .referrer("http://www.google.com")
                        .timeout(60000)
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }
}
