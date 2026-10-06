package org.labs;

public class Main {
    public static void main(String[] args) {
        SimulationConfig config = ConsoleInput.readConfig();
        DiningSimulation simulation = new DiningSimulation(config);

        SimulationResult result = simulation.run();
        result.printSummary();
    }
}
