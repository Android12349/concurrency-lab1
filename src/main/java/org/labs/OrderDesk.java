package org.labs;

import java.util.Arrays;

public final class OrderDesk {
    private final ServingRequest[] pendingRequests;
    private int remainingPortions;
    private int nextProgrammerId;
    private boolean closed;

    public OrderDesk(int programmerCount, int totalPortions) {
        this.pendingRequests = new ServingRequest[programmerCount];
        this.remainingPortions = totalPortions;
    }

    public synchronized void submit(ServingRequest request) {
        if (closed) {
            request.complete(false);
            return;
        }
        if (pendingRequests[request.programmerId()] != null) {
            throw new IllegalStateException("У программиста уже есть необработанный заказ");
        }

        pendingRequests[request.programmerId()] = request;
        notifyAll();
    }

    public synchronized ServingTask takeNext() throws InterruptedException {
        while (true) {
            if (closed) {
                return null;
            }

            if (remainingPortions == 0) {
                ServingRequest request = takeAnyPendingRequest();
                if (request != null) {
                    return new ServingTask(request, false, 0);
                }
            } else {
                ServingRequest request = pendingRequests[nextProgrammerId];
                if (request != null) {
                    pendingRequests[nextProgrammerId] = null;
                    remainingPortions--;
                    nextProgrammerId = (nextProgrammerId + 1) % pendingRequests.length;
                    return new ServingTask(request, true, remainingPortions);
                }
            }

            wait();
        }
    }

    public synchronized int remainingPortions() {
        return remainingPortions;
    }

    public synchronized void close() {
        closed = true;
        Arrays.stream(pendingRequests)
                .filter(request -> request != null)
                .forEach(request -> request.complete(false));
        Arrays.fill(pendingRequests, null);
        notifyAll();
    }

    private ServingRequest takeAnyPendingRequest() {
        for (int index = 0; index < pendingRequests.length; index++) {
            if (pendingRequests[index] != null) {
                ServingRequest request = pendingRequests[index];
                pendingRequests[index] = null;
                return request;
            }
        }
        return null;
    }

    public record ServingTask(
            ServingRequest request,
            boolean portionAvailable,
            int remainingPortions
    ) {
    }
}
