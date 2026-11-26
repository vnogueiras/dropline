package dev.dropline.catalog.domain.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

public class LayoutNodeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeSectionContainingSeatBlock() throws Exception {
        String json = """
            {
                "name": "Main Hall",
                "children": [
                    {
                        "name": "Section A",
                        "rows": [
                            {
                                "label": "A",
                                "seatCount": 10
                            },
                            {
                                "label": "B",
                                "seatCount": 12
                            }
                        ]
                    }
                ]
            }
            """;

        LayoutNode node = objectMapper.readValue(json, LayoutNode.class);

        assertInstanceOf(Section.class, node);

        Section section = (Section) node;

        assertEquals("Main Hall", section.name());
        assertEquals(1, section.children().size());

        LayoutNode child = section.children().getFirst();

        assertInstanceOf(SeatBlock.class, child);

        SeatBlock seatBlock = (SeatBlock) child;

        assertEquals("Section A", seatBlock.name());
        assertEquals(2, seatBlock.rows().size());

        assertEquals("A", seatBlock.rows().getFirst().label());
        assertEquals(10, seatBlock.rows().getFirst().seatCount());

        assertEquals("B", seatBlock.rows().getLast().label());
        assertEquals(12, seatBlock.rows().getLast().seatCount());
    }
}
