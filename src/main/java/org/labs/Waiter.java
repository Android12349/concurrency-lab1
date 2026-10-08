package org.labs;

import java.util.Optional;

public final class Waiter implements Runnable {
    private final int id;
    private final OrderDesk orderDesk;
    private final SimulationLogger logger;

    public Waiter(int id, OrderDesk orderDesk, SimulationLogger logger) {
        this.id = id;
        this.orderDesk = orderDesk;
        this.logger = logger;
    }

    @Override
    public void run() {
        logger.log("Официант " + id + " начал работу");

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Optional<OrderDesk.ServingTask> nextTask = orderDesk.awaitNextTask();
                if (nextTask.isEmpty()) {
                    break;
                }
                OrderDesk.ServingTask task = nextTask.get();

                if (task.portionAvailable()) {
                    logger.log("Официант " + id + " принёс порцию программисту " + task.request().programmerId() + ". Осталось порций: " + task.remainingPortions());
                } else {
                    logger.log("Официант " + id + " сообщил программисту " + task.request().programmerId() + ", что еда закончилась");
                }

                task.request().complete(task.portionAvailable());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            logger.log("Официант " + id + " закончил работу");
        }
    }
}
