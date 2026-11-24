package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.PriceTier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceTierRepository extends JpaRepository<PriceTier, UUID> {}
