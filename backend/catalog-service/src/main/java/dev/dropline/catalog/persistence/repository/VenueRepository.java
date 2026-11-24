package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.Venue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VenueRepository extends JpaRepository<Venue, UUID> {

    @Query("SELECT DISTINCT v.city FROM Venue v ORDER BY v.city ASC")
    List<String> findDistinctCities();
}
