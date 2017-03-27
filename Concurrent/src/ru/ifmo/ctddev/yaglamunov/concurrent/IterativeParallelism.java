package ru.ifmo.ctddev.yaglamunov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class IterativeParallelism implements ListIP {

    private final SingleThreadFunctions master = new SingleThreadFunctions();

    @Override
    public String join(int threads, List<?> list) throws InterruptedException {
        return concurrentFunction(threads, list, master::join, data -> String.join("", data));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return concurrentFunction(threads, list, (Function<java.util.List<? extends T>, List<T>>) data -> master.filter(data, predicate), master::joinToList);
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return concurrentFunction(threads, list, (Function<java.util.List<? extends T>, List<U>>) data -> master.map(data, function), master::joinToList);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return concurrentFunction(threads, list, data -> master.maximum(data, comparator), data -> master.maximum(data, comparator));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return concurrentFunction(threads, list, data -> master.minimum(data, comparator), data -> master.minimum(data, comparator));
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return concurrentFunction(threads, list, data -> master.all(data, predicate), data -> master.all(data, Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return concurrentFunction(threads, list, data -> master.any(data, predicate), data -> master.any(data, Boolean::booleanValue));
    }

    private <T, S, U> U concurrentFunction(int threadsNumber, List<? extends T> list, Function<List<? extends T>, S> function, Function<List<? extends S>, U> resultFunction) throws InterruptedException {
        int size = list.size() / Math.min(threadsNumber, list.size());
        int count = (list.size() + size - 1) / size;
        List<Thread> threads = new ArrayList<>();
        List<S> results = new ArrayList<>(count);
        for (int j = 0; j < count; j++) {
            final List<? extends T> part = list.subList(j * size, Math.min(list.size(), (j + 1) * size));
            final int position = j;
            results.add(null);
            threads.add(new Thread(() -> results.set(position, function.apply(part))));
            threads.get(j).start();
        }
        for (int j = 0; j < count; j++) {
            threads.get(j).join();
        }
        return resultFunction.apply(results);
    }
}
