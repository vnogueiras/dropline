package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.Venue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, UUID> {}
