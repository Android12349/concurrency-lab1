package org.labs;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDeskTest {
    @Test
    void waitsUntilRequestForNextProgrammerAppears() throws Exception {
        OrderDesk orderDesk = new OrderDesk(2, 2);
        ServingRequest secondProgrammerRequest = new ServingRequest(1);
        orderDesk.submit(secondProgrammerRequest);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<OrderDesk.ServingTask> waitingTask = executor.submit(
                    () -> orderDesk.awaitNextTask().orElseThrow()
            );

            Thread.sleep(50);
            assertFalse(waitingTask.isDone());

            ServingRequest firstProgrammerRequest = new ServingRequest(0);
            orderDesk.submit(firstProgrammerRequest);

            OrderDesk.ServingTask task = waitingTask.get(1, TimeUnit.SECONDS);
            assertSame(firstProgrammerRequest, task.request());
            assertTrue(task.portionAvailable());
        } finally {
            orderDesk.close();
        }
    }

    @Test
    void rejectsPendingRequestWhenFoodIsOver() throws Exception {
        OrderDesk orderDesk = new OrderDesk(2, 0);
        ServingRequest request = new ServingRequest(1);
        orderDesk.submit(request);

        OrderDesk.ServingTask task = orderDesk.awaitNextTask().orElseThrow();

        assertSame(request, task.request());
        assertFalse(task.portionAvailable());
        orderDesk.close();
    }

    @Test
    void returnsStopSignalWhenDeskIsClosed() throws Exception {
        OrderDesk orderDesk = new OrderDesk(2, 2);

        orderDesk.close();

        assertTrue(orderDesk.awaitNextTask().isEmpty());
    }
}
