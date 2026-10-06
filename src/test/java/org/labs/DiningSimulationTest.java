package org.labs;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiningSimulationTest {
    @Test
    void consumesAllFoodAndDistributesItFairly() {
        SimulationConfig config = new SimulationConfig(7, 2, 1_003, 0, 0, false);

        SimulationResult result = assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> new DiningSimulation(config).run()
        );

        assertEquals(1003, result.totalEaten());
        assertEquals(0, result.remainingPortions());
        assertTrue(result.differenceBetweenMostAndLeastFed() <= 1);
    }

    @Test
    void worksWhenThereIsNoFood() {
        SimulationConfig config = new SimulationConfig(3, 1, 0, 0, 0, false);

        SimulationResult result = assertTimeoutPreemptively(
                Duration.ofSeconds(2),
                () -> new DiningSimulation(config).run()
        );

        assertEquals(0, result.totalEaten());
        assertEquals(0, result.differenceBetweenMostAndLeastFed());
    }

    @Test
    void worksWithMoreWaitersThanProgrammers() {
        SimulationConfig config = new SimulationConfig(3, 5, 31, 0, 0, false);

        SimulationResult result = assertTimeoutPreemptively(
                Duration.ofSeconds(3),
                () -> new DiningSimulation(config).run()
        );

        assertEquals(31, result.totalEaten());
        assertTrue(result.differenceBetweenMostAndLeastFed() <= 1);
    }
}
