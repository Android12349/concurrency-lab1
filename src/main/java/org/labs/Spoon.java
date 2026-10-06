package org.labs;

import java.util.concurrent.locks.ReentrantLock;

public final class Spoon {
    private final int id;
    private final ReentrantLock lock = new ReentrantLock(true);

    public Spoon(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public void take() throws InterruptedException {
        lock.lockInterruptibly();
    }

    public void putBack() {
        lock.unlock();
    }
}
