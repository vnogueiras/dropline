package dev.dropline.catalog.api.mapper;

import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.persistence.repository.projection.EventSummaryView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventSummaryDto toDto(EventSummaryView view);
}
