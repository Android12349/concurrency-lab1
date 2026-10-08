package org.labs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDeskTest {
    @Test
    void waitsUntilRequestAppears() throws Exception {
        OrderDesk orderDesk = new OrderDesk(1, 1, 2);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<OrderDesk.ServingTask> waitingTask = executor.submit(
                    () -> orderDesk.awaitNextTask().orElseThrow()
            );

            Thread.sleep(50);
            assertFalse(waitingTask.isDone());

            ServingRequest request = new ServingRequest(0);
            orderDesk.submit(request);

            OrderDesk.ServingTask task = waitingTask.get(1, TimeUnit.SECONDS);
            assertSame(request, task.request());
            assertTrue(task.portionAvailable());
        } finally {
            orderDesk.close();
        }
    }

    @Test
    void takesRequestsInArrivalOrder() throws Exception {
        OrderDesk orderDesk = new OrderDesk(2, 1, 2);
        ServingRequest firstRequest = new ServingRequest(1);
        ServingRequest secondRequest = new ServingRequest(0);
        orderDesk.submit(firstRequest);
        orderDesk.submit(secondRequest);

        OrderDesk.ServingTask firstTask = orderDesk.awaitNextTask().orElseThrow();
        OrderDesk.ServingTask secondTask = orderDesk.awaitNextTask().orElseThrow();

        assertSame(firstRequest, firstTask.request());
        assertSame(secondRequest, secondTask.request());
        orderDesk.close();
    }

    @Test
    void rejectsPendingRequestWhenFoodIsOver() throws Exception {
        OrderDesk orderDesk = new OrderDesk(2, 1, 0);
        ServingRequest request = new ServingRequest(1);
        orderDesk.submit(request);

        OrderDesk.ServingTask task = orderDesk.awaitNextTask().orElseThrow();

        assertSame(request, task.request());
        assertFalse(task.portionAvailable());
        orderDesk.close();
    }

    @Test
    void allocatesPortionsAtomicallyToConcurrentWaiters() throws Exception {
        int totalRequests = 10000;
        int totalPortions = 2500;
        OrderDesk orderDesk = new OrderDesk(totalRequests, 100, totalPortions);

        for (int programmerId = 0; programmerId < totalRequests; programmerId++) {
            orderDesk.submit(new ServingRequest(programmerId));
        }

        List<Future<OrderDesk.ServingTask>> tasks = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < totalRequests; index++) {
                tasks.add(executor.submit(() -> orderDesk.awaitNextTask().orElseThrow()));
            }

            long successfulTasks = 0;
            for (Future<OrderDesk.ServingTask> task : tasks) {
                if (task.get(5, TimeUnit.SECONDS).portionAvailable()) {
                    successfulTasks++;
                }
            }

            assertEquals(totalPortions, successfulTasks);
            assertEquals(0, orderDesk.remainingPortions());
        } finally {
            orderDesk.close();
        }
    }

    @Test
    void returnsStopSignalWhenDeskIsClosed() throws Exception {
        OrderDesk orderDesk = new OrderDesk(1, 1, 2);

        orderDesk.close();

        assertTrue(orderDesk.awaitNextTask().isEmpty());
    }
}
