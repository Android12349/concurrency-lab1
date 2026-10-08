package org.labs;

import java.util.Arrays;

public final class OrderDesk {
    private final ServingRequest[] pendingRequests;
    private int remainingPortions;
    private int nextProgrammerId;
    private int pendingRequestCount;
    private int requestSearchStartIndex;
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
        pendingRequestCount++;
        notify();
    }

    public synchronized ServingTask takeNext() throws InterruptedException {
        while (true) {
            if (closed) {
                return null;
            }

            if (remainingPortions == 0) {
                if (pendingRequestCount > 0) {
                    ServingRequest request = takeAnyPendingRequest();
                    return new ServingTask(request, false, 0);
                }
            } else {
                ServingRequest request = pendingRequests[nextProgrammerId];
                if (request != null) {
                    pendingRequests[nextProgrammerId] = null;
                    pendingRequestCount--;
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
        pendingRequestCount = 0;
        notifyAll();
    }

    private ServingRequest takeAnyPendingRequest() {
        for (int offset = 0; offset < pendingRequests.length; offset++) {
            int index = (requestSearchStartIndex + offset) % pendingRequests.length;
            if (pendingRequests[index] != null) {
                ServingRequest request = pendingRequests[index];
                pendingRequests[index] = null;
                pendingRequestCount--;
                requestSearchStartIndex = (index + 1) % pendingRequests.length;
                return request;
            }
        }

        throw new IllegalStateException("Счётчик заказов не совпадает с содержимым очереди");
    }

    public record ServingTask(
            ServingRequest request,
            boolean portionAvailable,
            int remainingPortions
    ) {
    }
}
