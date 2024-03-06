package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    List<Index> findByLemmaId(Lemma lemma);

    Optional<Index> findByPageIdAndLemmaId(Page page, Lemma lemma);

}
