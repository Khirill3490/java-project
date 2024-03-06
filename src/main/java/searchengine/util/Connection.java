package searchengine.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import searchengine.repository.PageRepository;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class Connection {

    private final PageRepository pageRepository;

    public org.jsoup.Connection.Response getConnection(String url) {
        org.jsoup.Connection.Response connection;
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
            } catch (IOException | InterruptedException e) {
                log.error("Подключение по URL " + url + " не выполнено. " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return connection;
    }
}
