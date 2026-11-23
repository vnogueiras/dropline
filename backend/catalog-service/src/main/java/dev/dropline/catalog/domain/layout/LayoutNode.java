package dev.dropline.catalog.domain.layout;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({@JsonSubTypes.Type(Section.class), @JsonSubTypes.Type(SeatBlock.class)})
public sealed interface LayoutNode permits Section, SeatBlock {}
