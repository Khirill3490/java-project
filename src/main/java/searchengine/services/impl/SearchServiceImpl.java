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

    @Override
    public Object startSearch(String query, String site, Integer offset, Integer limit) {
        if (limit < 1) throw new BadRequestException("limit не может быть меньше 1");
        System.out.println(limit);
        SearchingResponse searchingResponse = new SearchingResponse();
        if (query.isEmpty()) throw new BadRequestException("Задан пустой поисковый запрос");
        List<String> stringLemmaList = lemmaListByFrequency(findLemmas.getLemmasFromQuery(query), site);
        if(stringLemmaList.isEmpty()) throw new DataNotFoundException("Леммы, по запросу '" + query + "' не найдены");
        List<Page> pages = getSearchPages(stringLemmaList);

        Relevance relevance = new Relevance(lemmaRepository, indexRepository);
        Map<Page, Float> map = relevance.getPagesRelevance(pages, stringLemmaList);
        List<DataSearch> dataSearchList = getListByOffset(getSearchLemmas(map, stringLemmaList), limit, offset);

        searchingResponse.setResult(true);
        searchingResponse.setCount(getLemmaCount());
        searchingResponse.setData(dataSearchList);
        setLemmaCount(0);

        return searchingResponse;
    }

    public List<Page> getSearchPages(List<String> list) {
        List<Page> pages = new ArrayList<>();
        for (String s: list) {
            List<Lemma> lemmaList = lemmaRepository.findByLemma(s);
            for (Lemma lemma: lemmaList) {
                List<Index> indexList = indexRepository.findByLemmaId(lemma);
                for (Index index: indexList) {
                    pages.add(index.getPageId());
                }
            }
        }
        return pages;
    }

    public List<DataSearch> getSearchLemmas(Map<Page, Float> pageList, List<String> lemmaList) {
        List<DataSearch> dsList = new ArrayList<>();
        for (Page page: pageList.keySet()) {
            for (String s: lemmaList) {
                dsList.addAll(search(s, page, pageList.get(page)));
            }
        }
        return dsList;
    }

    public List<DataSearch> search(String query, Page page, float relevance) {
        Site site = page.getSiteId();
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

    public List<String> lemmaListByFrequency(Set<String> setList, String siteUrl) {
        HashMap<String, Integer> map = new HashMap<>();
        List<String> lemmalist = new ArrayList<>();
        for (String s: setList) {
            List<Lemma> lemmaList;
            if (siteRepository.findByUrl(siteUrl).isPresent()) {
                Site site = siteRepository.findByUrl(siteUrl).get();
                lemmaList = lemmaRepository.findLemmaByLemmaAndSiteId(s, site);
            } else lemmaList = lemmaRepository.findByLemma(s);

            for (Lemma lemma: lemmaList) {
                if (map.containsKey(lemma.getLemma())) {
                    map.put(lemma.getLemma(),
                            map.get(lemma.getLemma()) + lemma.getFrequency());
                } else map.put(lemma.getLemma(), lemma.getFrequency());
            }
        }
        map.values().removeIf(value -> value > 30);
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparing(Map.Entry::getValue));
        for (Map.Entry<String, Integer> entry : list) {
            lemmalist.add(entry.getKey());
        }
        return lemmalist;
    }

    public String getTitle(Page page) {
        Document document = Jsoup.parse(page.getContent());
        return document.title();
    }

    public List<DataSearch> getListByOffset(List<DataSearch> dsList, int limit, int offset) {
        int result = 0;
        if (offset > 1) result = limit * (offset - 1);
        return dsList.subList(result >= dsList.size() ? 0 : result,
                (result + limit) > dsList.size() ? dsList.size() : result + limit);
    }
    public int getLemmaCount() { return lemmaCount; }

    public void setLemmaCount(int lemmaCount) {
        this.lemmaCount = lemmaCount;
    }
}
