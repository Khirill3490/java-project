package searchengine.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "site_id", referencedColumnName = "id", columnDefinition = "INT", nullable = false)
    private Site siteId;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(columnDefinition = "INT", nullable = false)
    private Integer frequency;

//    @Transient
//    private Page page;


//    id INT NOT NULL AUTO_INCREMENT;
//● site_id INT NOT NULL — ID веб-сайта из таблицы site;
//● lemma VARCHAR(255) NOT NULL — нормальная форма слова (лемма);
//● frequency INT NOT NULL — количество страниц, на которых слово
//    встречается хотя бы один раз. Максимальное значение не может
//    превышать общее количество слов на сайте.
}
