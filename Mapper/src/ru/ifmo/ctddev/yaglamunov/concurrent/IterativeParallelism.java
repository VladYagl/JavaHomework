package ru.ifmo.ctddev.yaglamunov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementation of class {@code ListIP}
 */
public class IterativeParallelism implements ListIP {
    private final ParallelMapper parallelMapper;

    /**
     * Objects with single thread functions
     */
    private final SingleThreadFunctions master = new SingleThreadFunctions();

    /**
     * Creates an instance of {@code IterativeParallelism}.
     */
    public IterativeParallelism() {
        parallelMapper = null;
    }

    /**
     * Creates an instance of {@link IterativeParallelism}.
     *
     * @param parallelMapper {@link ParallelMapper} to be used for concurrency.
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Concatenates list elements into a string.
     *
     * @param i    number of threads to use.
     * @param list the list to be joined.
     * @return Concatenated string representations of list elements.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        return concurrentFunction(i, list, master::join, data -> String.join("", data));
    }

    /**
     * Filters given list by predicate and returns filtered list.
     *
     * @param i         number of threads to use.
     * @param list      the list to be filtered.
     * @param predicate predicate to test list elements with.
     * @param <T>       type of elements in the list.
     * @return list consisting of elements of given list which satisfy the predicate.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return concurrentFunction(i, list, (Function<List<? extends T>, List<T>>) data -> master.filter(data, predicate), master::joinToList);
    }

    /**
     * Applies given function on every element of list, and creates list of results of function applications.
     *
     * @param i        number of threads to use.
     * @param list     the list to be mapped.
     * @param function function to apply on list elements.
     * @param <T>      type of elements of the initial list.
     * @param <U>      type of elements of the resulting list.
     * @return list of results of function applications.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return concurrentFunction(i, list, (Function<List<? extends T>, List<U>>) data -> master.map(data, function), master::joinToList);
    }

    /**
     * Finds maximum element in the list.
     *
     * @param i          number of threads to use.
     * @param list       the list to be searched.
     * @param comparator comparator to be used for searching.
     * @param <T>        type of elements in the list.
     * @return maximum of list, or {@code null} if list is empty.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return concurrentFunction(i, list, data -> master.maximum(data, comparator), data -> master.maximum(data, comparator));
    }

    /**
     * Finds minimum element in the list.
     *
     * @param i          number of threads to use.
     * @param list       the list to be searched.
     * @param comparator comparator to be used for searching.
     * @param <T>        type of elements in the list.
     * @return minimum of list, or {@code null} if list is empty.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return concurrentFunction(i, list, data -> master.minimum(data, comparator), data -> master.minimum(data, comparator));
    }

    /**
     * Tests if all list elements satisfy the predicate.
     *
     * @param i         number of threads to use.
     * @param list      the list to be tested.
     * @param predicate predicate to test with.
     * @param <T>       type of elements in the list.
     * @return {@code true} if all elements of the list satisfy predicate, {@code false} otherwise.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return concurrentFunction(i, list, data -> master.all(data, predicate), data -> master.all(data, Boolean::booleanValue));
    }

    /**
     * Tests if any list element satisfies the predicate.
     *
     * @param i         number of threads to use.
     * @param list      the list to be tested.
     * @param predicate predicate to test with.
     * @param <T>       type of elements in the list.
     * @return {@code true} if any element of the list satisfies predicate, {@code false} otherwise.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return concurrentFunction(i, list, data -> master.any(data, predicate), data -> master.any(data, Boolean::booleanValue));
    }

    /**
     * Applies given function on every element of list and reduces answer with another function
     *
     * @param i              number of threads to use.
     * @param list           the list to be mapped.
     * @param function       function to apply on list elements.
     * @param resultFunction function to reduce answer
     * @param <T>            type of elements of the initial list.
     * @param <U>            type of elements of the resulting list.
     * @return list of results of function applications.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    private <T, S, U> U concurrentFunction(int i, List<? extends T> list, Function<List<? extends T>, S> function, Function<List<? extends S>, U> resultFunction) throws InterruptedException {
        int partCount = Math.min(i, list.size());
        List<Thread> threads = new ArrayList<>();
        List<S> results = new ArrayList<>(partCount);
        List<List<? extends T>> parts = new ArrayList<>();

        double step = list.size() / (double) partCount;
        for (double to = step, from = 0; from < list.size(); to += step) {
            int ito = (int) Math.floor(to);
            parts.add(list.subList((int) from, Math.min(ito, list.size())));
            from = ito;
        }

        if (parallelMapper == null) {
            for (int j = 0; j < partCount; j++) {
                final int position = j;
                results.add(null);
                threads.add(new Thread(() -> results.set(position, function.apply(parts.get(position)))));
                threads.get(j).start();
            }
            for (int j = 0; j < partCount; j++) {
                threads.get(j).join();
            }
            return resultFunction.apply(results);
        } else {
            return resultFunction.apply(parallelMapper.map(function, parts));
        }
    }
}
