package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.models.Lemma;
import searchengine.models.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSiteId(String lemma, Site site);


    List<Lemma> findByLemma(String lemma);

    List<Lemma> findLemmaByLemmaAndSiteId(String lemma, Site site);

    Integer countBySiteId(Site site);
}