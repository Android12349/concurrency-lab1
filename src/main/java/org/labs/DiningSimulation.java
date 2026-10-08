package org.labs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public final class DiningSimulation {
    private final SimulationConfig config;

    public DiningSimulation(SimulationConfig config) {
        this.config = config;
    }

    public SimulationResult run() {
        long startedAt = System.nanoTime();
        SimulationLogger logger = new SimulationLogger(config.detailedLogging());
        OrderDesk orderDesk = new OrderDesk(
                config.programmerCount(),
                config.waiterCount(),
                config.totalPortions()
        );

        List<Spoon> spoons = createSpoons();
        List<Programmer> programmers = createProgrammers(spoons, orderDesk, logger);
        ExecutorService waiterExecutor = createVirtualExecutor("Официант-");
        ExecutorService programmerExecutor = createVirtualExecutor("Программист-");
        List<Future<?>> waiterTasks = new ArrayList<>();
        List<Future<?>> programmerTasks = new ArrayList<>();

        try {
            submitWaiters(waiterExecutor, orderDesk, logger, waiterTasks);
            submitProgrammers(programmerExecutor, programmers, programmerTasks);
            waitForAll(programmerTasks);
            orderDesk.close();
            waitForAll(waiterTasks);
        } finally {
            orderDesk.close();
            cancelAll(programmerTasks);
            cancelAll(waiterTasks);
            programmerExecutor.shutdownNow();
            waiterExecutor.shutdownNow();
            programmerExecutor.close();
            waiterExecutor.close();
        }

        long durationMs = (System.nanoTime() - startedAt) / 1000000;
        List<Integer> eatenPortions = programmers.stream()
                .map(Programmer::eatenPortions)
                .toList();

        return new SimulationResult(
                config.totalPortions(),
                orderDesk.remainingPortions(),
                eatenPortions,
                durationMs
        );
    }

    private List<Spoon> createSpoons() {
        List<Spoon> spoons = new ArrayList<>();
        for (int id = 0; id < config.programmerCount(); id++) {
            spoons.add(new Spoon(id));
        }
        return spoons;
    }

    private List<Programmer> createProgrammers(
            List<Spoon> spoons,
            OrderDesk orderDesk,
            SimulationLogger logger
    ) {
        List<Programmer> programmers = new ArrayList<>();

        for (int id = 0; id < config.programmerCount(); id++) {
            Spoon leftSpoon = spoons.get(id);
            Spoon rightSpoon = spoons.get((id + 1) % spoons.size());
            programmers.add(new Programmer(
                    id,
                    leftSpoon,
                    rightSpoon,
                    orderDesk,
                    config,
                    logger
            ));
        }

        return programmers;
    }

    private ExecutorService createVirtualExecutor(String threadNamePrefix) {
        ThreadFactory threadFactory = Thread.ofVirtual()
                .name(threadNamePrefix, 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }

    private void submitWaiters(
            ExecutorService executor,
            OrderDesk orderDesk,
            SimulationLogger logger,
            List<Future<?>> tasks
    ) {
        for (int id = 0; id < config.waiterCount(); id++) {
            tasks.add(executor.submit(new Waiter(id, orderDesk, logger)));
        }
    }

    private void submitProgrammers(
            ExecutorService executor,
            List<Programmer> programmers,
            List<Future<?>> tasks
    ) {
        for (Programmer programmer : programmers) {
            tasks.add(executor.submit(programmer));
        }
    }

    private void waitForAll(List<Future<?>> tasks) {
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание задач было прервано", exception);
            } catch (ExecutionException exception) {
                throw new IllegalStateException("Одна из задач завершилась с ошибкой", exception.getCause());
            }
        }
    }

    private void cancelAll(List<Future<?>> tasks) {
        tasks.forEach(task -> task.cancel(true));
    }
}
