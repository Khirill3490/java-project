package searchengine.models;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, columnDefinition = "INT")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    private StatusEnum status;

    @Column(name = "status_time", nullable = false, columnDefinition = "DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy hh:mm:ss")
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

//    @OneToMany(cascade = CascadeType.ALL)
//    private List<Site> siteList;

//    id INT NOT NULL AUTO_INCREMENT;
//● status ENUM('INDEXING', 'INDEXED', 'FAILED') NOT NULL — текущий
//    статус полной индексации сайта, отражающий готовность поискового
//    движка осуществлять поиск по сайту — индексация или переиндексация
//    в процессе, сайт полностью проиндексирован (готов к поиску) либо его не
//    удалось проиндексировать (сайт не готов к поиску и не будет до
//            устранения ошибок и перезапуска индексации);
//● status_time DATETIME NOT NULL — дата и время статуса (в случае
//статуса INDEXING дата и время должны обновляться регулярно при
//        добавлении каждой новой страницы в индекс);
//● last_error TEXT — текст ошибки индексации или NULL, если её не было;
//● url VARCHAR(255) NOT NULL — адрес главной страницы сайта;
//● name VARCHAR(255) NOT NULL — имя сайт
}
