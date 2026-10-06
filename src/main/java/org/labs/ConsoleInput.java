package org.labs;

import java.util.Scanner;

public final class ConsoleInput {
    private static final int DEFAULT_PROGRAMMERS = 7;
    private static final int DEFAULT_WAITERS = 2;
    private static final int DEFAULT_PORTIONS = 1000000;
    private static final int DEFAULT_MIN_DELAY_MS = 500;
    private static final int DEFAULT_MAX_DELAY_MS = 1000;

    private ConsoleInput() {
    }

    public static SimulationConfig readConfig() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Симуляция обеда программистов ===");
        System.out.println("Нажмите Enter, чтобы оставить значение по умолчанию.");

        int programmers = readInt(scanner, "Количество программистов", DEFAULT_PROGRAMMERS, 2);
        int waiters = readInt(scanner, "Количество официантов", DEFAULT_WAITERS, 1);
        int portions = readInt(scanner, "Количество порций", DEFAULT_PORTIONS, 0);
        boolean useDelays = readBoolean(scanner, "Использовать задержки 0.5-1 секунду?", true);
        boolean detailedLogging = readBoolean(scanner, "Показывать каждое действие?", portions <= 1000);

        int minDelay = useDelays ? DEFAULT_MIN_DELAY_MS : 0;
        int maxDelay = useDelays ? DEFAULT_MAX_DELAY_MS : 0;

        return new SimulationConfig(
                programmers,
                waiters,
                portions,
                minDelay,
                maxDelay,
                detailedLogging
        );
    }

    private static int readInt(Scanner scanner, String label, int defaultValue, int minimum) {
        while (true) {
            System.out.printf("%s [%d]: ", label, defaultValue);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                return defaultValue;
            }

            try {
                int value = Integer.parseInt(input);
                if (value >= minimum) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.printf("Введите целое число не меньше %d.%n", minimum);
        }
    }

    private static boolean readBoolean(Scanner scanner, String label, boolean defaultValue) {
        String defaultText = defaultValue ? "да" : "нет";

        while (true) {
            System.out.printf("%s [по умолчанию: %s] (да/нет): ", label, defaultText);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.isEmpty()) {
                return defaultValue;
            }
            if (input.equals("да") || input.equals("д") || input.equals("yes") || input.equals("y")) {
                return true;
            }
            if (input.equals("нет") || input.equals("н") || input.equals("no") || input.equals("n")) {
                return false;
            }

            System.out.println("Ответьте 'да' или 'нет'.");
        }
    }
}
