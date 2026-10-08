package org.labs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class OrderDesk {
    private static final ServingRequest SHUTDOWN_SIGNAL = new ServingRequest(-1);

    private final BlockingQueue<ServingRequest> pendingRequests = new LinkedBlockingQueue<>();
    private final Set<Integer> programmersWithPendingRequest = ConcurrentHashMap.newKeySet();
    private final AtomicInteger remainingPortions;
    private final AtomicIntegerArray remainingQuotaByProgrammer;
    private final int waiterCount;
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final Lock operationLock = lifecycleLock.readLock();
    private final Lock closeLock = lifecycleLock.writeLock();

    private boolean closed;

    public OrderDesk(int programmerCount, int waiterCount, int totalPortions) {
        this.waiterCount = waiterCount;
        this.remainingPortions = new AtomicInteger(totalPortions);
        this.remainingQuotaByProgrammer = createFairQuotas(programmerCount, totalPortions);
    }

    public void submit(ServingRequest request) {
        validateProgrammerId(request.programmerId());

        operationLock.lock();
        try {
            if (closed) {
                request.complete(false);
                return;
            }
            if (!programmersWithPendingRequest.add(request.programmerId())) {
                throw new IllegalStateException("У программиста уже есть необработанный заказ");
            }

            pendingRequests.add(request);
        } finally {
            operationLock.unlock();
        }
    }

    public Optional<ServingTask> awaitNextTask() throws InterruptedException {
        ServingRequest request = pendingRequests.take();
        if (request == SHUTDOWN_SIGNAL) {
            return Optional.empty();
        }

        operationLock.lock();
        try {
            programmersWithPendingRequest.remove(request.programmerId());

            if (closed) {
                return Optional.of(new ServingTask(request, false, remainingPortions.get()));
            }

            int portionsLeft = reservePortion(request.programmerId());
            boolean portionAvailable = portionsLeft >= 0;
            int displayedRemainingPortions = portionAvailable
                    ? portionsLeft
                    : remainingPortions.get();
            return Optional.of(new ServingTask(
                    request,
                    portionAvailable,
                    displayedRemainingPortions
            ));
        } finally {
            operationLock.unlock();
        }
    }

    public int remainingPortions() {
        return remainingPortions.get();
    }

    public void close() {
        closeLock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;

            List<ServingRequest> abandonedRequests = new ArrayList<>();
            pendingRequests.drainTo(abandonedRequests);
            abandonedRequests.forEach(request -> request.complete(false));
            programmersWithPendingRequest.clear();

            for (int index = 0; index < waiterCount; index++) {
                pendingRequests.add(SHUTDOWN_SIGNAL);
            }
        } finally {
            closeLock.unlock();
        }
    }

    private int reservePortion(int programmerId) {
        while (true) {
            int currentQuota = remainingQuotaByProgrammer.get(programmerId);
            if (currentQuota == 0) {
                return -1;
            }

            if (remainingQuotaByProgrammer.compareAndSet(
                    programmerId,
                    currentQuota,
                    currentQuota - 1
            )) {
                int portionsLeft = remainingPortions.decrementAndGet();
                if (portionsLeft < 0) {
                    throw new IllegalStateException("Остаток еды не может быть отрицательным");
                }
                return portionsLeft;
            }
        }
    }

    private void validateProgrammerId(int programmerId) {
        if (programmerId < 0 || programmerId >= remainingQuotaByProgrammer.length()) {
            throw new IllegalArgumentException("Неизвестный идентификатор программиста: " + programmerId);
        }
    }

    private AtomicIntegerArray createFairQuotas(int programmerCount, int totalPortions) {
        AtomicIntegerArray quotas = new AtomicIntegerArray(programmerCount);
        int baseQuota = totalPortions / programmerCount;
        int extraPortions = totalPortions % programmerCount;

        for (int programmerId = 0; programmerId < programmerCount; programmerId++) {
            int quota = baseQuota + (programmerId < extraPortions ? 1 : 0);
            quotas.set(programmerId, quota);
        }

        return quotas;
    }

    public record ServingTask(
            ServingRequest request,
            boolean portionAvailable,
            int remainingPortions
    ) {
    }
}
