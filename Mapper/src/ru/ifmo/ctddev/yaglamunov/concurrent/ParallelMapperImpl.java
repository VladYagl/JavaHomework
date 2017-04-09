package ru.ifmo.ctddev.yaglamunov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Implementation for {@code ParallelMapper}.
 */
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

    /**
     * Creates an instance of {@link ParallelMapper}.
     * Creates {@code threadsNumber} threads, which will be used in {@code map}.
     * @param threadsNumber number of threads to create.
     */
    public ParallelMapperImpl(int threadsNumber) {
        this.threads = new ArrayList<>();

        for (int i = 0; i < threadsNumber; i++) {
            threads.add(new Thread(new Worker()));
            threads.get(i).start();
        }
    }

    /**
     * Applies function on each element of list and returns list of results.
     * @param function function to apply.
     * @param args arguments list.
     * @param <T> type of arguments.
     * @param <R> type of results.
     * @return list of results of function applications.
     * @throws InterruptedException wait for threads interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, final List<? extends T> args) throws InterruptedException {
        ArrayList<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        int[] readyCount = {0};

        for (int i = 0; i < args.size(); ++i) {
            final int position = i;

            synchronized (taskQueue) {
                taskQueue.add(() -> {
                    result.set(position, function.apply(args.get(position)));

                    synchronized (readyCount) {
                        readyCount[0]++;
                        if (readyCount[0] == args.size()) {
                            readyCount.notify();
                        }
                    }
                });
                taskQueue.notify();
            }
        }

        synchronized (readyCount) {
            while (readyCount[0] < args.size()) {
                readyCount.wait();
            }
        }

        return result;
    }

    /**
     * Closes all threads.
     * @throws InterruptedException if main thread is interrupted during {@code Thread.join()} call.
     */
    @Override
    public void close() throws InterruptedException {
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
