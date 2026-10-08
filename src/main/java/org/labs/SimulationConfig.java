package org.labs;

public record SimulationConfig(
        int programmerCount,
        int waiterCount,
        int totalPortions,
        int minActionDelayMs,
        int maxActionDelayMs,
        boolean detailedLogging
) {
    public SimulationConfig {
        if (programmerCount < 2) {
            throw new IllegalArgumentException("Количество программистов должно быть не меньше 2");
        }
        if (waiterCount < 1) {
            throw new IllegalArgumentException("Количество официантов должно быть не меньше 1");
        }
        if (totalPortions < 0) {
            throw new IllegalArgumentException("Количество порций не может быть отрицательным");
        }
        if (minActionDelayMs < 0 || maxActionDelayMs < minActionDelayMs) {
            throw new IllegalArgumentException("Некорректный диапазон задержек");
        }
    }
}
