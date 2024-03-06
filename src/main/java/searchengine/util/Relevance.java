package searchengine.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import searchengine.exceptions.DataNotFoundException;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Relevance {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Getter
    @Setter
    private float maxRank = 0;

    public Map<Page, Float> getPagesRelevance(List<Page> pageList, List<String> lemmaList) {
        Map<Page, Float> map = new HashMap<>();
        for (Page p: pageList) {
            float a = 0.00f;
            for (String s : lemmaList) {
                Lemma lemma = lemmaRepository.findByLemmaAndSiteId(s, p.getSiteId()).orElseThrow(() -> new
                        DataNotFoundException("Лемма не найдена"));
                Index index = indexRepository.findByPageIdAndLemmaId(p, lemma).orElseThrow(() -> new
                        DataNotFoundException("Индекс не найден"));
                a = a + index.getRank();
            }
            if (a > maxRank) maxRank = a;
            map.put(p, a);
        }
        map.replaceAll((k, v) -> v / maxRank);
        return map.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

}
