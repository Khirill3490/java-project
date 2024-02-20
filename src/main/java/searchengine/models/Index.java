package searchengine.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "indexes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "page_id", referencedColumnName = "id", columnDefinition = "INT", nullable = false)
    private Page pageId;

    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", columnDefinition = "INT")
    private Lemma lemmaId;


    @Column(columnDefinition = "Decimal", nullable = false, name = "ranks")
    private Float rank;

//    index — поисковый индекс
//● id INT NOT NULL AUTO_INCREMENT;
//● page_id INT NOT NULL — идентификатор страницы;
//● lemma_id INT NOT NULL — идентификатор леммы;
//● rank FLOAT NOT NULL — количество данной леммы для данной
//    страницы.

}
