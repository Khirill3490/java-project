package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.*;

@Component
public class FindLemmas {
    private LuceneMorphology luceneMorphology;
    private static RussianLuceneMorphology russianMorph;


    public FindLemmas() {
    }

    static {
        try {
            russianMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Integer> getLemmasInMap(String htmlContent) {
        String[] words = htmlContent
                .toLowerCase().replaceAll("[^а-я\\s]", " ")
                .trim().split("\\s+");

        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (isWord(word) == false) continue;
            List<String> normalForms = russianMorph.getNormalForms(word);
            String normalWordForm = normalForms.get(0);
            if (lemmas.containsKey(normalWordForm)) {
                lemmas.put(normalWordForm, lemmas.get(normalWordForm) + 1);
            } else lemmas.put(normalWordForm, 1);
        }
        return lemmas;
    }

    public List<String> getLemmasInArray(String word) {
        if (word.isEmpty()) return Collections.emptyList();
        List<String> lemmas = new ArrayList<>();
        if (isWord(word)) {
            List<String> normalForms = russianMorph.getNormalForms(word);
            lemmas.addAll(normalForms);
        }

        return lemmas;
    }

    private boolean isWord(String word) {
        List<String> list = russianMorph.getMorphInfo(word);
        if (list.get(0).contains("МЕЖД") || list.get(0).contains("ПРЕДЛ") || list.get(0).contains("СОЮЗ")
                || list.get(0).contains("ЧАСТ") || list.get(0).contains("МС") || list.get(0).length() < 4) return false;
        return true;
    }

    public List<String> getTextFromPage(String query, Page page) {
        Document document = Jsoup.parse(page.getContent());
        String contentText = document.title() + " " + document.body().text();
        List<Integer> lemmaIndex = findLemmaIndexInText(query, contentText);
        Collections.sort(lemmaIndex);
        return new ArrayList<>(getWordsFromContent(contentText, lemmaIndex));
    }

    public List<Integer> findLemmaIndexInText(String lemma, String content) {
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] elements = content.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s");
        FindLemmas findLemmas = new FindLemmas();
        int index = 0;
        for (String el : elements) {
            String s = el.replaceAll("\\p{Punct}|[0-9]|@|©|◄|»|«|—|-|№|…", "")
                    .replaceAll("[^а-я\\s]", "");
            List<String> lemmas = findLemmas.getLemmasInArray(s);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    lemmaIndexList.add(index);
                }
            }
            index += el.length() + 1;
        }
        return lemmaIndexList;
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end).replaceAll("[^А-я]", "");
        int firstPoint;
        int lastPoint;
        if (content.lastIndexOf(" ", start - 10) != -1) {
            firstPoint = content.lastIndexOf(" ", start - 10);
        } else firstPoint = start;
        if (content.indexOf(" ", end + 10) != -1) {
            lastPoint = content.indexOf(" ", end + 10);
        } else lastPoint = content.indexOf(" ", end);
        String words = content.substring(firstPoint, lastPoint);
        String text = words.replaceAll(word, "<b>" + word + "</b>");
        return "..." + text + "...";
    }

    public Set<String> getLemmasFromQuery(String query) {
        Set<String> lemmaList = new HashSet<>();
        FindLemmas findLemmas = new FindLemmas();
        String[] words = query.trim().toLowerCase().split("\\s+");
        for (String s: words) {
            List<String> lemmas = findLemmas.getLemmasInArray(s);
            if (lemmas.isEmpty()) continue;
            for (String lemma: lemmas) {
                lemmaList.add(lemma);
            }
        }
        return lemmaList;
    }

}
