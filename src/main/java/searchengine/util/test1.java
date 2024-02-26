package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test1 {
    public static void main(String[] args) {
        try {
            // Подключение к веб-странице
            Document doc = Jsoup.connect("https://skillbox.ru/sale/main/").get();

            // Извлечение всех элементов с текстом
            Elements elementsWithText = doc.select("*[text()]");

            // Создание списка для хранения текстовых строк
            List<String> textLines = new ArrayList<>();

            // Итерация по элементам и добавление текста в список
            for (Element element : elementsWithText) {
                textLines.add(element.text());
            }

            System.out.println(textLines.size()); // Вывод количества текстовых строк
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
