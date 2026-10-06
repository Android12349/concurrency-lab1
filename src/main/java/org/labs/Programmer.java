package org.labs;

import java.util.concurrent.ThreadLocalRandom;

public final class Programmer implements Runnable {
    private final int id;
    private final Spoon firstSpoon;
    private final Spoon secondSpoon;
    private final OrderDesk orderDesk;
    private final SimulationConfig config;
    private final SimulationLogger logger;

    private int eatenPortions;

    public Programmer(
            int id,
            Spoon leftSpoon,
            Spoon rightSpoon,
            OrderDesk orderDesk,
            SimulationConfig config,
            SimulationLogger logger
    ) {
        this.id = id;
        this.orderDesk = orderDesk;
        this.config = config;
        this.logger = logger;

        if (leftSpoon.id() < rightSpoon.id()) {
            this.firstSpoon = leftSpoon;
            this.secondSpoon = rightSpoon;
        } else {
            this.firstSpoon = rightSpoon;
            this.secondSpoon = leftSpoon;
        }
    }

    @Override
    public void run() {
        logger.log("Программист " + id + " пришёл на обед");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                ServingRequest request = new ServingRequest(id);
                logger.log("Программист " + id + " попросил новую порцию");
                orderDesk.submit(request);

                if (!request.waitForResult()) {
                    break;
                }

                eat();
                discussTeachers();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.log("Программист " + id + " был прерван");
        } finally {
            logger.log("Программист " + id + " закончил обед. Съедено: " + eatenPortions);
        }
    }

    public int eatenPortions() {
        return eatenPortions;
    }

    private void eat() throws InterruptedException {
        firstSpoon.take();
        logger.log("Программист " + id + " взял ложку " + firstSpoon.id());

        try {
            secondSpoon.take();
            logger.log("Программист " + id + " взял ложку " + secondSpoon.id());

            try {
                logger.log("Программист " + id + " ест");
                sleepForRandomDelay();
                eatenPortions++;
            } finally {
                secondSpoon.putBack();
                logger.log("Программист " + id + " положил ложку " + secondSpoon.id());
            }
        } finally {
            firstSpoon.putBack();
            logger.log("Программист " + id + " положил ложку " + firstSpoon.id());
        }
    }

    private void discussTeachers() throws InterruptedException {
        logger.log("Программист " + id + " обсуждает преподавателей");
        sleepForRandomDelay();
    }

    private void sleepForRandomDelay() throws InterruptedException {
        int minDelay = config.minActionDelayMs();
        int maxDelay = config.maxActionDelayMs();

        if (maxDelay == 0) {
            return;
        }

        int delay = minDelay == maxDelay ? minDelay : ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
        Thread.sleep(delay);
    }
}
