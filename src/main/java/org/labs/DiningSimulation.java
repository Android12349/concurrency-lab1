package org.labs;

import java.util.ArrayList;
import java.util.List;

public final class DiningSimulation {
    private final SimulationConfig config;

    public DiningSimulation(SimulationConfig config) {
        this.config = config;
    }

    public SimulationResult run() {
        long startedAt = System.nanoTime();
        SimulationLogger logger = new SimulationLogger(config.detailedLogging());
        OrderDesk orderDesk = new OrderDesk(config.programmerCount(), config.totalPortions());

        List<Spoon> spoons = createSpoons();
        List<Programmer> programmers = createProgrammers(spoons, orderDesk, logger);
        List<Thread> waiterThreads = createWaiterThreads(orderDesk, logger);
        List<Thread> programmerThreads = createProgrammerThreads(programmers);

        waiterThreads.forEach(Thread::start);
        programmerThreads.forEach(Thread::start);

        joinAll(programmerThreads);
        orderDesk.close();
        joinAll(waiterThreads);

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

    private List<Thread> createWaiterThreads(OrderDesk orderDesk, SimulationLogger logger) {
        List<Thread> threads = new ArrayList<>();
        for (int id = 0; id < config.waiterCount(); id++) {
            threads.add(new Thread(new Waiter(id, orderDesk, logger), "Официант-" + id));
        }
        return threads;
    }

    private List<Thread> createProgrammerThreads(List<Programmer> programmers) {
        List<Thread> threads = new ArrayList<>();
        for (int id = 0; id < programmers.size(); id++) {
            threads.add(new Thread(programmers.get(id), "Программист-" + id));
        }
        return threads;
    }

    private void joinAll(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException exception) {
                threads.forEach(Thread::interrupt);
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Ожидание потоков было прервано", exception);
            }
        }
    }
}
