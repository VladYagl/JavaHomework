package ru.ifmo.ctddev.yaglamunov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threads;
    private final Queue<Runnable> taskQueue = new LinkedList<>();

    private class Worker implements Runnable {

        @Override
        public void run() {
            while (true) {
                Runnable task;

                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            if (!Thread.interrupted()) {
                                taskQueue.wait();
                            } else {
                                return;
                            }
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    task = taskQueue.remove();
                }

                task.run();
            }
        }

    }

    private final Object readyLock = new Object();
    private volatile int readyCount = 0;

    public ParallelMapperImpl(int threadsNumber) {
        this.threads = new ArrayList<>();

        for (int i = 0; i < threadsNumber; i++) {
            threads.add(new Thread(new Worker()));
            threads.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> args) throws InterruptedException {
        ArrayList<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));

        for (int i = 0; i < args.size(); ++i) {
            final int position = i;

            synchronized (taskQueue) {
                taskQueue.add(() -> {
                    result.set(position, function.apply(args.get(position)));

                    synchronized (readyLock) {
                        readyCount++;
                        if (readyCount == args.size()) {
                            readyLock.notify();
                        }
                    }
                });
                taskQueue.notify();
            }
        }

        synchronized (readyLock) {
            while (readyCount < args.size()) {
                readyLock.wait();
            }
        }

        return result;
    }

    @Override
    public void close() throws InterruptedException {
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
