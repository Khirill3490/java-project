package searchengine.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(columnDefinition = "INT", nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;


//    page — проиндексированные страницы сайта
//● id INT NOT NULL AUTO_INCREMENT;
//● site_id INT NOT NULL — ID веб-сайта из таблицы site;
//● path TEXT NOT NULL — адрес страницы от корня сайта (должен
//                                                              начинаться со слэша, например: /news/372189/);
//2
//        ● code INT NOT NULL — код HTTP-ответа, полученный при запросе
//    страницы (например, 200, 404, 500 или другие);
//● content MEDIUMTEXT NOT NULL — контент страницы (HTML-код).

}
