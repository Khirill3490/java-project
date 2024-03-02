package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {


//    @Query(value = "SELECT * from indexes WHERE indexes.lemma_id IN :lemmas and indexes.page_id IN :pages", nativeQuery = true)
//    List<Index> findByPageIdAndLemmaId(@Param("lemmas")List<Lemma> lemmas,
//                                       @Param("pages") List<Page> pages);

    List<Index> findByLemmaId(Lemma lemma);

    Optional<Index> findByPageIdAndLemmaId(Page page, Lemma lemma);

}
