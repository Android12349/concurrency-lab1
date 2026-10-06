package org.labs;

import java.util.concurrent.CompletableFuture;

public final class ServingRequest {
    private final int programmerId;
    private final CompletableFuture<Boolean> result = new CompletableFuture<>();

    public ServingRequest(int programmerId) {
        this.programmerId = programmerId;
    }

    public int programmerId() {
        return programmerId;
    }

    public boolean waitForResult() throws InterruptedException {
        try {
            return result.get();
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("Официант не смог обработать заказ", exception.getCause());
        }
    }

    public void complete(boolean portionReceived) {
        result.complete(portionReceived);
    }
}
