package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.models.Page;
import searchengine.models.Site;
import searchengine.services.EntitiesService;
import searchengine.services.impl.IndexingServiceImpl;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

public class HtmlParserFork extends RecursiveAction {
    private final EntitiesService entitiesService;
    private Site site;
    private String link;

    private AtomicBoolean stop;
//    public static HashSet<String> resultPagesSet = new LinkedHashSet<>();
//    public static AtomicBoolean stop = new AtomicBoolean();
    private FindLemmas findLemmas;

    public HtmlParserFork(EntitiesService entitiesService, Site site, String link) {
        this.entitiesService = entitiesService;
        this.site = site;
        this.link = link;
        this.stop = IndexingServiceImpl.getStop();
    }

    @Override
    protected void compute() {
        try {
//            if (StartThreadIndex.resultPagesSet.size() > 20) stop.set(true);
            if (entitiesService.isLink(link) && stop.get() == false) {
                List<String> linksList = parseLinks(link);
                List<HtmlParserFork> tasks = new ArrayList<>();
                if (!linksList.isEmpty()) {
                    for (String link : linksList) {
                        HtmlParserFork task = new HtmlParserFork(entitiesService, site, link);
                        task.fork();
                        tasks.add(task);
                    }
                }
                joinTasks(tasks);
            }
        } catch (SocketTimeoutException ex) {
            System.out.println(ex + link);
        } catch (InterruptedException e) {
            System.out.println(e + link);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> parseLinks(String URL) throws IOException, InterruptedException {
        List<String> links = new ArrayList<>();
        Connection.Response connection = entitiesService.getConnection(URL);
        Document document = connection.parse();
        if (checkPage(URL, document.html(), connection.statusCode())) return Collections.emptyList();
        Elements elements = document.select("a");
        elements.forEach(element -> {
            String link = element.attr("href").replaceAll("/$", "");
            String linkWithStartUrl = link.startsWith("/") ? URL + link : link;
            if (!linkWithStartUrl.equals(site.getUrl()) && !StartThreadIndex.resultPagesSet.contains(linkWithStartUrl)
                    && (link.startsWith(site.getUrl()) || link.startsWith("/"))) {
                System.out.println("На странице " + URL + " обнаружена ссылка " + link);
                links.add(linkWithStartUrl);
            }
        });
        return links;
    }

    public boolean checkPage(String path, String content, int statusCode) {
        if (StartThreadIndex.resultPagesSet.contains(path)) return true;
        if (site.getUrl().equals(path)) return false;
        if (statusCode != HttpStatus.OK.value()) {
            StartThreadIndex.resultPagesSet.add(path);
            entitiesService.addPage(site, path, content, statusCode);
            return true;
        }
        StartThreadIndex.resultPagesSet.add(path);
        Page page = entitiesService.addPage(site, path, content, statusCode);
        findLemmas(page);
        return false;
    }

    public synchronized void findLemmas(Page page) {
        findLemmas = new FindLemmas();
        Document document = Jsoup.parse(page.getContent());
        String text = document.title() + " " + document.body().text();
        HashMap<String, Integer> lemmaList = findLemmas.getLemmasInMap(text);
        entitiesService.addLemmaAndIndex(page, lemmaList);
    }

    public void joinTasks(List<HtmlParserFork> list) {
        try {
            list.forEach(HtmlParserFork::join);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }
}
