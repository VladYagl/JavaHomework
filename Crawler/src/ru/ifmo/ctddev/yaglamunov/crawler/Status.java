package ru.ifmo.ctddev.yaglamunov.crawler;

class Status {
    private int tasks = 0;

    synchronized void addTask() {
        tasks++;
        this.notify();
    }

    synchronized void finishTask() {
        tasks--;
        this.notify();
    }

    synchronized void waitFinish() throws InterruptedException {
        while (tasks > 0) {
            wait();
        }
    }
}
