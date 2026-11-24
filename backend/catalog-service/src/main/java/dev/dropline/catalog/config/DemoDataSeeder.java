package dev.dropline.catalog.config;

import dev.dropline.catalog.domain.entity.*;
import dev.dropline.catalog.domain.layout.*;
import dev.dropline.catalog.persistence.repository.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Component
@Profile({"local", "demo"})
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final PriceTierRepository priceTierRepository;
    private final SeatRepository seatRepository;

    private final LayoutWalker layoutWalker = new LayoutWalker();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (eventRepository.count() > 0) {
            log.info("Demo data already exists. Skipping seeding.");
            return;
        }

        log.info("Seeding demo data...");

        List<VenueSeed> venues = createVenues();
        venueRepository.saveAll(venues.stream().map(VenueSeed::venue).toList());

        List<Seat> allSeats = new ArrayList<>();

        for (VenueSeed venueSeed : venues) {
            for (EventSeed seed : eventsFor(venueSeed.venue())) {
                Event event = createEvent(venueSeed.venue(), seed);
                eventRepository.save(event);

                List<PriceTier> tiers = createPriceTiers(event);
                priceTierRepository.saveAll(tiers);

                Map<String, PriceTier> tiersByName = Map.of(
                        "VIP", tiers.get(0),
                        "Premium", tiers.get(1),
                        "Standard", tiers.get(2));

                for (SeatPosition position : layoutWalker.flatten(venueSeed.layout())) {
                    PriceTier tier = tierFor(position.sectionPath(), tiersByName);

                    allSeats.add(
                            new Seat(event, position.sectionPath(), position.rowLabel(), position.seatNumber(), tier));
                }
            }
        }

        seatRepository.saveAll(allSeats);

        log.info("Demo data seeded: {} venues, 12 events, {} seats", venues.size(), allSeats.size());
    }

    private List<VenueSeed> createVenues() {
        return List.of(
                new VenueSeed(
                        new Venue("Ziggo Dome", "Amsterdam", "Europe/Amsterdam", toJson(ziggoLayout())), ziggoLayout()),
                new VenueSeed(
                        new Venue("Palau Sant Jordi", "Barcelona", "Europe/Madrid", toJson(barcelonaLayout())),
                        barcelonaLayout()),
                new VenueSeed(
                        new Venue("Akvárium Klub", "Budapest", "Europe/Budapest", toJson(budapestLayout())),
                        budapestLayout()));
    }

    private LayoutNode ziggoLayout() {
        return new Section(
                "Main Hall",
                List.of(
                        new Section("Floor", List.of(block("VIP", 2, 10), block("Premium", 4, 15))),
                        new Section("Balcony", List.of(block("Standard", 4, 20)))));
    }

    private LayoutNode barcelonaLayout() {
        return new Section(
                "Arena",
                List.of(
                        new Section("Lower Level", List.of(block("VIP", 2, 8), block("Premium", 3, 12))),
                        new Section("Upper Level", List.of(block("Standard", 4, 15)))));
    }

    private LayoutNode budapestLayout() {
        return new Section(
                "Club",
                List.of(
                        new Section("Ground Floor", List.of(block("VIP", 2, 6), block("Premium", 3, 10))),
                        new Section("Gallery", List.of(block("Standard", 3, 12)))));
    }

    private SeatBlock block(String name, int rows, int seatsPerRow) {
        List<SeatBlock.Row> rowList = java.util.stream.IntStream.range(0, rows)
                .mapToObj(i -> new SeatBlock.Row(String.valueOf((char) ('A' + i)), seatsPerRow))
                .toList();

        return new SeatBlock(name, rowList);
    }

    private List<EventSeed> eventsFor(Venue venue) {
        String city = venue.getCity();

        return switch (city) {
            case "Amsterdam" ->
                List.of(
                        new EventSeed("Electronic Nights", 7, EventStatus.ON_SALE),
                        new EventSeed("Summer Live", 21, EventStatus.PUBLISHED),
                        new EventSeed("Indie Sessions", 42, EventStatus.SOLD_OUT),
                        new EventSeed("The Grand Show", 75, EventStatus.ON_SALE));
            case "Barcelona" ->
                List.of(
                        new EventSeed("Barcelona Music Festival", 10, EventStatus.ON_SALE),
                        new EventSeed("Latin Beats", 28, EventStatus.PUBLISHED),
                        new EventSeed("Rock Arena", 50, EventStatus.SOLD_OUT),
                        new EventSeed("Symphonic Night", 82, EventStatus.ON_SALE));
            default ->
                List.of(
                        new EventSeed("Budapest Live", 14, EventStatus.PUBLISHED),
                        new EventSeed("Danube Sounds", 35, EventStatus.ON_SALE),
                        new EventSeed("Underground Sessions", 60, EventStatus.SOLD_OUT),
                        new EventSeed("Winter Lights", 88, EventStatus.PUBLISHED));
        };
    }

    private Event createEvent(Venue venue, EventSeed seed) {
        ZonedDateTime localStart = ZonedDateTime.now(ZoneId.of(venue.getTimezone()))
                .plusDays(seed.daysFromNow())
                .withHour(19)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        Instant startsAt = localStart.toInstant();

        Event event = new Event(
                venue,
                seed.title(),
                "Experience " + seed.title() + " at " + venue.getName() + ".",
                "/images/events/" + slug(seed.title()) + ".jpg",
                startsAt,
                startsAt.plus(Duration.ofHours(3)),
                startsAt.minus(Duration.ofDays(30)));

        event.changeStatus(seed.status());

        return event;
    }

    private List<PriceTier> createPriceTiers(Event event) {
        return List.of(
                new PriceTier(event, "VIP", 12000, "EUR"),
                new PriceTier(event, "Premium", 8500, "EUR"),
                new PriceTier(event, "Standard", 5000, "EUR"));
    }

    private PriceTier tierFor(String sectionPath, Map<String, PriceTier> tiers) {
        if (sectionPath.contains("/VIP")) {
            return tiers.get("VIP");
        }

        if (sectionPath.contains("/Premium")) {
            return tiers.get("Premium");
        }

        return tiers.get("Standard");
    }

    private String slug(String title) {
        return title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private JsonNode toJson(LayoutNode node) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(
                "name",
                switch (node) {
                    case Section section -> section.name();
                    case SeatBlock seatBlock -> seatBlock.name();
                });

        switch (node) {
            case Section section -> {
                ArrayNode children = json.putArray("children");
                section.children().forEach(child -> children.add(toJson(child)));
            }
            case SeatBlock seatBlock -> {
                ArrayNode rows = json.putArray("rows");

                seatBlock.rows().forEach(row -> {
                    ObjectNode rowJson = rows.addObject();
                    rowJson.put("label", row.label());
                    rowJson.put("seatCount", row.seatCount());
                });
            }
        }

        return json;
    }

    private record VenueSeed(Venue venue, LayoutNode layout) {}

    private record EventSeed(String title, int daysFromNow, EventStatus status) {}
}
