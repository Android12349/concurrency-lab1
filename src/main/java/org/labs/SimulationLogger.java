package org.labs;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class SimulationLogger {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final boolean enabled;

    public SimulationLogger(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized void log(String message) {
        if (!enabled) {
            return;
        }

        System.out.printf("[%s] [%s] %s%n",
                LocalTime.now().format(TIME_FORMAT),
                Thread.currentThread().getName(),
                message);
    }
}
