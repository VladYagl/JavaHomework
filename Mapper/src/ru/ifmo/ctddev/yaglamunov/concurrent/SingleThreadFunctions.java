package ru.ifmo.ctddev.yaglamunov.concurrent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class SingleThreadFunctions {
    /**
     * Finds maximum element in the list.
     *
     * @param list       the list to be searched.
     * @param comparator comparator to be used for searching.
     * @param <T>        type of elements in the list.
     * @return maximum of list, or {@code null} if list is empty.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    <T> T maximum(List<? extends T> list, Comparator<? super T> comparator) {
        return Collections.max(list, comparator);
    }

    /**
     * Finds minimum element in the list.
     *
     * @param list       the list to be searched.
     * @param comparator comparator to be used for searching.
     * @param <T>        type of elements in the list.
     * @return minimum of list, or {@code null} if list is empty.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    <T> T minimum(List<? extends T> list, Comparator<? super T> comparator) {
        return Collections.min(list, comparator);
    }

    /**
     * Tests if all list elements satisfy the predicate.
     *
     * @param list      the list to be tested.
     * @param predicate predicate to test with.
     * @param <T>       type of elements in the list.
     * @return {@code true} if all elements of the list satisfy predicate, {@code false} otherwise.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    <T> boolean all(List<? extends T> list, Predicate<? super T> predicate) {
        return list.stream().allMatch(predicate);
    }

    /**
     * Tests if any list element satisfies the predicate.
     *
     * @param list      the list to be tested.
     * @param predicate predicate to test with.
     * @param <T>       type of elements in the list.
     * @return {@code true} if any element of the list satisfies predicate, {@code false} otherwise.
     * @throws InterruptedException if one of created threads was interrupted.
     */
    <T> boolean any(List<? extends T> list, Predicate<? super T> predicate) {
        return list.stream().anyMatch(predicate);
    }

    /**
     * Filters given list by predicate and returns filtered list.
     *
     * @param values    the list to be filtered.
     * @param predicate predicate to test list elements with.
     * @param <T>       type of elements in the list.
     * @return list consisting of elements of given list which satisfy the predicate.
     */
    <T> List<T> filter(List<? extends T> values, Predicate<? super T> predicate) {
        return values.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * Applies given function on every element of list, and creates list of results of function applications.
     *
     * @param values the list to be mapped.
     * @param f      function to apply on list elements.
     * @param <T>    type of elements of the initial list.
     * @param <U>    type of elements of the resulting list.
     * @return list of results of function applications.
     */
    <T, U> List<U> map(List<? extends T> values, Function<? super T, ? extends U> f) {
        return values.stream().map(f).collect(Collectors.toList());
    }

    /**
     * Joins list of lists into one list
     *
     * @param list list of lists
     * @param <T>  type of elements of the initial list.
     * @return list of all elements
     */
    <T> List<T> joinToList(List<? extends List<T>> list) {
        return list.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * Concatenates list elements into a string.
     *
     * @param values the list to be joined.
     * @return Concatenated string representations of list elements.
     */
    String join(List<?> values) {
        return values.stream().map(Object::toString).collect(Collectors.joining());
    }
}
