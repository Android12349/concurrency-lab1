package org.labs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationConfigTest {
    @Test
    void acceptsValidConfiguration() {
        assertDoesNotThrow(() -> new SimulationConfig(7, 2, 100, 0, 10, false));
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationConfig(1, 2, 100, 0, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationConfig(7, 0, 100, 0, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationConfig(7, 2, -1, 0, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationConfig(7, 2, 100, 10, 5, false));
    }
}
