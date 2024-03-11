package searchengine.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.models.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PageUtil {

    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final FindLemmas findLemmas;



    public Page addPage(Site site, String pageUrl, String content, int statusCode) {
        Page page = new Page();
        page.setPath(pageUrl);
        page.setContent(content);
        page.setSiteId(site);
        page.setCode(statusCode);
        pageRepository.save(page);
        log.info("Страница " + page.getPath() + " добавлена в БД");
        return page;
    }

    public synchronized void findLemmasInPageText(Page page) {
        Document document = Jsoup.parse(page.getContent());
        String text = document.title() + " " + document.body().text();
        HashMap<String, Integer> lemmaList = findLemmas.getLemmasInMap(text);
        addLemmaAndIndex(page, lemmaList);
    }

    private void addLemmaAndIndex(Page page, HashMap<String, Integer> lemmas) {
        synchronized (lemmaRepository) {
            for (String s : lemmas.keySet()) {
                Index index = new Index();
                index.setPageId(page);
                Optional<Lemma> lemmaFromSql = lemmaRepository.findByLemmaAndSiteId(s, page.getSiteId());
                if (lemmaFromSql.isEmpty()) {
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
}
