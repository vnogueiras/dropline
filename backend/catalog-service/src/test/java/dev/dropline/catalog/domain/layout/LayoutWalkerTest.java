package dev.dropline.catalog.domain.layout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class LayoutWalkerTest {

    @Test
    void shouldCountSeatsInSeatBlock() {
        // Arrange: create a SeatBlock with 2 rows (10 + 12 seats)
        LayoutNode node = new SeatBlock("A", List.of(new SeatBlock.Row("a", 10), new SeatBlock.Row("b", 12)));
        // Act: call countSeats()
        LayoutWalker lw = new LayoutWalker();
        int res = lw.countSeats(node);
        // Assert: result should be 22
        assertEquals(22, res);
    }

    @Test
    void shouldCountSeatsInSection() {
        // Arrange
        LayoutNode child11 = new SeatBlock("A", List.of(new SeatBlock.Row("a", 10), new SeatBlock.Row("b", 12)));
        LayoutNode child12 = new SeatBlock("B", List.of(new SeatBlock.Row("a", 5)));
        LayoutNode child1 = new Section("Balcony", List.of(child11, child12));
        LayoutNode node = new Section("Main Hall", List.of(child1));
        // Act
        LayoutWalker lw = new LayoutWalker();
        int actual = lw.countSeats(node);
        // Assert
        assertEquals(27, actual);
    }

    @Test
    void shouldFlattenLayout() {
        // Arrange
        LayoutNode child11 = new SeatBlock("A", List.of(new SeatBlock.Row("a", 1), new SeatBlock.Row("b", 1)));
        LayoutNode child12 = new SeatBlock("B", List.of(new SeatBlock.Row("a", 2)));
        LayoutNode child1 = new Section("Balcony", List.of(child11, child12));
        LayoutNode node = new Section("Main Hall", List.of(child1));
        // Act
        LayoutWalker lw = new LayoutWalker();
        List<SeatPosition> actual = lw.flatten(node);
        // Assert
        List<SeatPosition> expected = List.of(
                new SeatPosition("Main Hall/Balcony/A", "a", 1),
                new SeatPosition("Main Hall/Balcony/A", "b", 1),
                new SeatPosition("Main Hall/Balcony/B", "a", 1),
                new SeatPosition("Main Hall/Balcony/B", "a", 2));
        assertEquals(expected, actual);
    }

    @Test
    void shouldFlattenLayoutWithEmptySection() {
        // Arrange
        LayoutNode child11 = new SeatBlock("A", List.of(new SeatBlock.Row("a", 1), new SeatBlock.Row("b", 1)));
        LayoutNode child12 = new SeatBlock("B", List.of());
        LayoutNode child1 = new Section("Balcony", List.of(child11, child12));
        LayoutNode node = new Section("Main Hall", List.of(child1));
        // Act
        LayoutWalker lw = new LayoutWalker();
        List<SeatPosition> actual = lw.flatten(node);
        // Assert
        List<SeatPosition> expected = List.of(
                new SeatPosition("Main Hall/Balcony/A", "a", 1), new SeatPosition("Main Hall/Balcony/A", "b", 1));
        assertEquals(expected, actual);
    }

    @Test
    void shouldFlattenLayoutWithMultipleNestedLevels() {
        // Arrange
        LayoutNode blockA = new SeatBlock("A", List.of(new SeatBlock.Row("a", 2), new SeatBlock.Row("b", 1)));
        LayoutNode blockB = new SeatBlock("B", List.of(new SeatBlock.Row("c", 2)));
        LayoutNode blockC = new SeatBlock("C", List.of(new SeatBlock.Row("d", 1)));

        LayoutNode eastWing = new Section("East Wing", List.of(blockA));
        LayoutNode westWing = new Section("West Wing", List.of(blockB));
        LayoutNode upperLevel = new Section("Upper Level", List.of(eastWing, westWing));
        LayoutNode floor = new Section("Floor", List.of(blockC));
        LayoutNode lowerLevel = new Section("Lower Level", List.of(floor));
        LayoutNode mainHall = new Section("Main Hall", List.of(upperLevel, lowerLevel));

        // Act
        LayoutWalker lw = new LayoutWalker();
        List<SeatPosition> actual = lw.flatten(mainHall);
        // Assert
        List<SeatPosition> expected = List.of(
                new SeatPosition("Main Hall/Upper Level/East Wing/A", "a", 1),
                new SeatPosition("Main Hall/Upper Level/East Wing/A", "a", 2),
                new SeatPosition("Main Hall/Upper Level/East Wing/A", "b", 1),
                new SeatPosition("Main Hall/Upper Level/West Wing/B", "c", 1),
                new SeatPosition("Main Hall/Upper Level/West Wing/B", "c", 2),
                new SeatPosition("Main Hall/Lower Level/Floor/C", "d", 1));

        assertEquals(expected, actual);
    }

    @Test
    void shouldAcceptLayoutWithDepthFive() {

        // Arrange
        LayoutNode seatBlock = new SeatBlock("A", List.of(new SeatBlock.Row("a", 10)));

        LayoutNode section4 = new Section("Section 4", List.of(seatBlock));

        LayoutNode section3 = new Section("Section 3", List.of(section4));

        LayoutNode section2 = new Section("Section 2", List.of(section3));

        LayoutNode root = new Section("Section 1", List.of(section2));

        // Act & Assert
        LayoutWalker lw = new LayoutWalker();
        assertDoesNotThrow(() -> lw.validateDepth(root));
    }

    @Test
    void shouldRejectLayoutWithDepthSix() {

        // Arrange
        LayoutNode seatBlock = new SeatBlock("A", List.of(new SeatBlock.Row("a", 10)));

        LayoutNode section5 = new Section("Section 5", List.of(seatBlock));

        LayoutNode section4 = new Section("Section 4", List.of(section5));

        LayoutNode section3 = new Section("Section 3", List.of(section4));

        LayoutNode section2 = new Section("Section 2", List.of(section3));

        LayoutNode root = new Section("Section 1", List.of(section2));

        // Act & Assert
        LayoutWalker lw = new LayoutWalker();
        assertThrows(IllegalArgumentException.class, () -> lw.validateDepth(root));
    }

    @Test
    void shouldAcceptSeatBlockAtRoot() {
        // Arrange
        LayoutNode seatBlock = new SeatBlock("A", List.of(new SeatBlock.Row("a", 10)));
        // Act & Assert
        LayoutWalker lw = new LayoutWalker();
        assertDoesNotThrow(() -> lw.validateDepth(seatBlock));
    }
}
