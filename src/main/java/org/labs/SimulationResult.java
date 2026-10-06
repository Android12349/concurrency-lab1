package org.labs;

import java.util.List;

public record SimulationResult(
        int initialPortions,
        int remainingPortions,
        List<Integer> eatenByProgrammer,
        long durationMs
) {
    public int totalEaten() {
        return eatenByProgrammer.stream().mapToInt(Integer::intValue).sum();
    }

    public int differenceBetweenMostAndLeastFed() {
        if (eatenByProgrammer.isEmpty()) {
            return 0;
        }

        int minimum = eatenByProgrammer.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maximum = eatenByProgrammer.stream().mapToInt(Integer::intValue).max().orElse(0);
        return maximum - minimum;
    }

    public void printSummary() {
        System.out.println();
        System.out.println("=== Обед завершён ===");
        for (int id = 0; id < eatenByProgrammer.size(); id++) {
            System.out.printf("Программист %d съел порций: %d%n", id, eatenByProgrammer.get(id));
        }
        System.out.println("Всего было порций: " + initialPortions);
        System.out.println("Всего съедено: " + totalEaten());
        System.out.println("Осталось порций: " + remainingPortions);
        System.out.println("Максимальная разница: " + differenceBetweenMostAndLeastFed());
        System.out.println("Время выполнения: " + durationMs + " мс");
    }
}
