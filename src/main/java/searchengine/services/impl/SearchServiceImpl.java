package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.DataSearch;
import searchengine.dto.searching.ErrorSearchingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.exceptions.BadRequestException;
import searchengine.exceptions.DataNotFoundException;
import searchengine.models.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;
import searchengine.util.FindLemmas;
import searchengine.util.Relevance;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final FindLemmas findLemmas;
    private int lemmaCount;
    private List<Page> pageList = new ArrayList<>();

    @Override
    public Object startSearch(String query, String site, Integer offset, Integer limit) {
        if (offset == null) offset = 0;
        SearchingResponse searchingResponse = new SearchingResponse();
        if (query.isEmpty()) throw new BadRequestException("Задан пустой поисковый запрос");
        List<String> lemmaList = lemmaListByFrequency(findLemmas.getLemmasFromQuery(query));
        if(lemmaList.isEmpty()) throw new DataNotFoundException("Леммы, по запросу '" + query + "' не найдены");
        getSearchPages(lemmaList, site);


        Relevance relevance = new Relevance(lemmaRepository, indexRepository);
        Map<Page, Float> map = relevance.getPagesRelevance(pageList, lemmaList);
        List<DataSearch> dataSearchList;

        if (limit != 0 && (offset != null || offset != 0)) {
            dataSearchList = getListByOffset(getSearchLemmas(map, lemmaList), limit, offset);
        } else {
            dataSearchList = getSearchLemmas(map, lemmaList);
        }

        searchingResponse.setResult(true);
        searchingResponse.setCount(getLemmaCount());
        searchingResponse.setData(dataSearchList);
        setLemmaCount(0);
        pageList.clear();

        return searchingResponse;
    }

    public void getSearchPages(List<String> lemmaList, String site) {
        for (String lemma: lemmaList) {
            List<Page> pList = new ArrayList<>(pageList);
            setLemmaCount(0);
            if (site == null) {
                if (getIndexedSites().isEmpty()) throw new DataNotFoundException("Нет проиндексированных сайтов");
                searchPages(lemma, getIndexedSites(), pList);
            } else {
                System.out.println(site);
                System.out.println("Внтри метода");
                Optional<Site> siteOptional = siteRepository.findByUrl(site);
                if (!siteOptional.isPresent()) throw new DataNotFoundException("Такой сайт не найден в БД");
                if (siteOptional.get().getStatus() != StatusEnum.INDEXED) throw new BadRequestException("Данный сайт не проиндексирован");
                searchPages(lemma, Collections.singletonList(siteOptional.get()), pList);
            }
        }
    }

    public void searchPages(String query, List<Site> indexedSites, List<Page> pages) {
        List<Page> pList = new ArrayList<>();
        for (Site site : indexedSites) {
            Optional<Lemma> lemmaOptional = lemmaRepository.findByLemmaAndSiteId(query, site);
            if (lemmaOptional.isPresent()) {
                Lemma lemma = lemmaOptional.get();
                List<Index> indexList = indexRepository.findByLemmaId(lemma);
                for (Index index : indexList) {
                    Page page = index.getPageId();
                    if (pages.isEmpty()) pageList.add(page);
                    else pList.add(page);
                }
            }
        }
        if (!pages.isEmpty()) pageList.retainAll(pList);
    }

    public List<DataSearch> getSearchLemmas(Map<Page, Float> pageList, List<String> lemmaList) {
        List<DataSearch> dsList = new ArrayList<>();
        for (Page p: pageList.keySet()) {
            for (String s: lemmaList) {
                dsList.addAll(search(s, p.getSiteId(), p, pageList.get(p)));
            }
        }
        return dsList;
    }

    public List<DataSearch> search(String query, Site site, Page page, float relevance) {
        List<DataSearch> dataSearchList = new ArrayList<>();
        Lemma lemma = lemmaRepository.findByLemmaAndSiteId(query, site).get();
        Index index = indexRepository.findByPageIdAndLemmaId(page, lemma).get();
        setLemmaCount((int) (getLemmaCount() + index.getRank()));
        String uri = page.getPath().replaceAll(site.getUrl(), "");
        List<String> text = findLemmas.getTextFromPage(query, page);
        for (int i = 0; i < index.getRank(); i++) {
            dataSearchList.add(new DataSearch(site.getUrl(),
                    site.getName(),
                    uri, getTitle(page),
                    text.get(i), relevance));
        }
        return dataSearchList;
    }

    public List<String> lemmaListByFrequency(Set<String> setList) {
        HashMap<String, Integer> map = new HashMap<>();
        for (String s: setList) {
            List<Lemma> listListFromSql = lemmaRepository.findByLemma(s);
            for (Lemma lemma: listListFromSql) {
                if (map.containsKey(lemma.getLemma())) {
                    map.put(lemma.getLemma(),
                            map.get(lemma.getLemma()) + lemma.getFrequency());
                } else map.put(lemma.getLemma(), lemma.getFrequency());
            }
        }
        map.values().removeIf(value -> value > 30);
        List<String> resultList = new ArrayList<>(map.keySet());
        resultList.sort(Comparator.comparingInt(map::get));
        return resultList;
    }

    public List<Site> getIndexedSites() {
        return siteRepository.findAll()
                .stream()
                .filter(site ->  site.getStatus().equals(StatusEnum.INDEXED))
                .collect(Collectors.toList());
    }

    public String getTitle(Page page) {
        Document document = Jsoup.parse(page.getContent());
        return document.title();
    }

    public List<DataSearch> getListByOffset(List<DataSearch> dsList, int limit, int offset) {
        return dsList.subList(limit * (offset - 1), dsList.size());
    }
    public int getLemmaCount() { return lemmaCount; }

    public void setLemmaCount(int lemmaCount) {
        this.lemmaCount = lemmaCount;
    }
}
