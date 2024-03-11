package searchengine.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.models.Page;
import searchengine.models.Site;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class HtmlParserFork extends RecursiveAction {
    private final PageUtil pageUtil;
    private final searchengine.util.Connection connection;
    private final Site site;
    private final String link;


    private final Pattern pattern = Pattern.compile("(https?://)?([\\w-]+\\.[\\w-]+)[^\\s@]*$");

    public static AtomicBoolean stop = new AtomicBoolean(false);


    @Override
    protected void compute() {
        try {
            List<HtmlParserFork> tasks = new ArrayList<>();
            if (!stop.get()) {
                if (isLink(link)) {
                    List<String> linksList = parseLinks(link);
                    if (!linksList.isEmpty()) {
                        for (String link : linksList) {
                            HtmlParserFork task = new HtmlParserFork(pageUtil, connection, site, link);
                            task.fork();
                            tasks.add(task);
                        }
                    }
                    joinTasks(tasks);
                }
            } else joinTasks(tasks);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public List<String> parseLinks(String url) throws IOException {
        List<String> links = new ArrayList<>();
        Connection.Response connect = connection.getConnection(url);
        Document document = connect.parse();
        if (checkPage(url, document.html(), connect.statusCode())) {
            return Collections.emptyList();
        }
        Elements elements = document.select("a");
        elements.forEach(element -> {
            String link = element.attr("href").replaceAll("/$", "");
            String linkWithStartUrl = link.startsWith("/") ? url + link : link;
            if (!linkWithStartUrl.equals(site.getUrl())
                    && !StartThreadIndex.resultPagesSet.contains(linkWithStartUrl)
                    && (link.startsWith(site.getUrl()) || link.startsWith("/"))) {
                links.add(linkWithStartUrl);
            }
        });
        return links;
    }

    public boolean checkPage(String path, String content, int statusCode) {
        if (StartThreadIndex.resultPagesSet.contains(path)) {
            return true;
        }
        if (site.getUrl().equals(path)) {
            return false;
        }
        if (statusCode != HttpStatus.OK.value()) {
            StartThreadIndex.resultPagesSet.add(path);
            pageUtil.addPage(site, path, content, statusCode);
            return true;
        }
        StartThreadIndex.resultPagesSet.add(path);
        Page page = pageUtil.addPage(site, path, content, statusCode);
        if (!stop.get()) {
            pageUtil.findLemmasInPageText(page);
        }
        return false;
    }

    public boolean isLink(String link) {

        return !link.contains(".pdf") && !link.contains(".jpg")
                && !link.contains("%") && !link.contains("#")
                && !link.contains(".png") && !link.isEmpty()
                && pattern.matcher(link).find() && !link.contains("mailto:");
    }

    public void joinTasks(List<HtmlParserFork> list) {
        try {
            list.forEach(HtmlParserFork::join);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }
}
