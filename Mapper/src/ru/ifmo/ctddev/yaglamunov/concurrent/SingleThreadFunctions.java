package ru.ifmo.ctddev.yaglamunov.concurrent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class SingleThreadFunctions {
    <T> T maximum(List<? extends T> list, Comparator<? super T> comparator) {
        return Collections.max(list, comparator);
    }

    <T> T minimum(List<? extends T> list, Comparator<? super T> comparator) {
        return Collections.min(list, comparator);
    }

    <T> boolean all(List<? extends T> list, Predicate<? super T> predicate) {
        return list.stream().allMatch(predicate);
    }

    <T> boolean any(List<? extends T> list, Predicate<? super T> predicate) {
        return list.stream().anyMatch(predicate);
    }

    <T> List<T> filter(List<? extends T> values, Predicate<? super T> predicate) {
        return values.stream().filter(predicate).collect(Collectors.toList());
    }

    <T, U> List<U> map(List<? extends T> values, Function<? super T, ? extends U> f) {
        return values.stream().map(f).collect(Collectors.toList());
    }

    <T> List<T> joinToList(List<? extends List<T>> list) {
        return list.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    String join(List<?> values) {
        return values.stream().map(Object::toString).collect(Collectors.joining());
    }
}
