package ru.ifmo.ctddev.yaglamunov.crawler;

/**
 * Contains how many tasks are needed to finish request.
 */
class Status {
    /**
     * Number of unfinished tasks
     */
    private int tasks = 0;

    /**
     * Increases number of unfinished tasks
     */
    synchronized void addTask() {
        tasks++;
    }

    /**
     * Decreases number of unfinished tasks
     */
    synchronized void finishTask() {
        tasks--;
        this.notify();
    }

    /**
     * Waits until there are some unfinished tasks
     *
     * @throws InterruptedException if wait throws
     */
    synchronized void waitFinish() throws InterruptedException {
        while (tasks > 0) {
            wait();
        }
    }
}
