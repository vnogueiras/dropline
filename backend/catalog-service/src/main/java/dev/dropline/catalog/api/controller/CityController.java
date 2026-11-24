package dev.dropline.catalog.api.controller;

import dev.dropline.catalog.service.EventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final EventService eventService;

    @GetMapping()
    public List<String> findCities() {
        return eventService.findCities();
    }
}
