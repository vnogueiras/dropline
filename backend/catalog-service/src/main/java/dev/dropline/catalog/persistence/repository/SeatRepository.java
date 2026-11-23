package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.Seat;
import dev.dropline.catalog.persistence.repository.projection.SeatCountByTierView;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Query(
            """
            SELECT
                s.tier.id AS tierId,
                COUNT(s) AS seatCount
            FROM Seat s
            WHERE s.event.id = :eventId
            GROUP BY s.tier.id
            """)
    List<SeatCountByTierView> countSeatsByTier(@Param("eventId") UUID eventId);
}
