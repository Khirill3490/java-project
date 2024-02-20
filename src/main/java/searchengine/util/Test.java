package searchengine.util;


import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Test {

    public static LuceneMorphology luceneMorphology;

    static {
        try {
            luceneMorphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Test() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        float a = 2;
        float b = 3;
        float c = a / b;
        System.out.println(a + " | " + b + " | " + c + " | ");

//
//
//        Document document = Jsoup.connect("https://lenta.ru/rubrics/world")
//                .ignoreHttpErrors(true).userAgent("Opera").timeout(30000).get();
//        String text = document.title() + " " + document.body().text();
//        HashMap<String, Integer> lemmaList = findLemmas.getLemmasInMap(text);
////        lemmaList.keySet().forEach(System.out::println);
    }

//    private static boolean isWord(String word) {
//        List<String> list = luceneMorphology.getMorphInfo(word);
//        if (list.get(0).contains("МЕЖД") || list.get(0).contains("ПРЕДЛ")
//                || list.get(0).contains("СОЮЗ")) return false;
//        return true;
//    }
//        public static String clear(String content, String selector) {
//        StringBuilder html = new StringBuilder();
//        var doc = Jsoup.parse(content);
//        var elements = doc.select(selector);
//        for (Element el : elements) {
//            html.append(el.html());
//        }
//        return Jsoup.parse(html.toString()).text();
//    }

//    повторный — 1
//    появление — 1
//    постоянно — 1
//    позволять — 1
//    предположить — 1
//    северный — 1
//    район — 1
//    кавказ — 1
//    осетия — 1
//    леопард — 2
//    обитать — 1

}

