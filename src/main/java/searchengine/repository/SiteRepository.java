package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.models.Site;
import searchengine.models.StatusEnum;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByName(String name);

    Optional<Site> findByUrl(String url);

    List<Site> findByStatus(StatusEnum status);
}