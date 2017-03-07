package ru.ifmo.ctddev.yaglamunov.arrayset;


import java.util.*;

@SuppressWarnings("WeakerAccess")
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    private class ArraySetIterator implements Iterator<T> {
        private int position;
        private final boolean descending;

        ArraySetIterator(int position) {
            this.position = position;
            descending = false;
        }

        ArraySetIterator(int position, boolean descending) {
            this.position = position;
            this.descending = descending;
        }

        @Override
        public boolean hasNext() {
            return (!descending ? position < size() : position >= 0);
        }

        @Override
        public T next() {
            return data.get(!descending ? position++ : position--);
        }
    }

    private class DescendingList extends AbstractList<T> {
        private final List<T> data;

        DescendingList(List<T> other) {
            data = other;
        }

        @Override
        public T get(int index) {
            return data.get(size() - index - 1);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private final List<T> data;
    private final Comparator<T> comparator;
    private final int size;

    private int find(T e, boolean inclusive, boolean lowerBound) {
        int position = Collections.binarySearch(data, e, comparator);
        if (position < 0) {
            if (lowerBound) {
                return -position - (inclusive ? 1 : 2);
            } else {
                return -position - (inclusive ? 2 : 1);
            }
        } else {
            if (lowerBound) {
                return position + (inclusive ? 0 : -1);
            } else {
                return position + (inclusive ? 0 : 1);
            }
        }
    }

    private int compare(T a, T b) {
        if (comparator == null) {
            return ((Comparable<? super T>) a).compareTo(b);
        } else {
            return comparator.compare(a, b);
        }
    }

    private T get(int position) {
        if (position < 0) {
            return null;
        }
        if (position >= size) {
            return null;
        }
        return data.get(position);
    }

    public ArraySet(Collection<T> collection, Comparator<T> comparator) {
        this.comparator = comparator;
        ArrayList<T> tmp = new ArrayList<>(collection);
        tmp.sort(this.comparator);
        data = new ArrayList<>();
        for (int i = 0; i < tmp.size(); i++) {
            if (i == 0 || compare(tmp.get(i), tmp.get(i - 1)) != 0) {
                data.add(tmp.get(i));
            }
        }
        size = data.size();
    }

    public ArraySet(Collection<T> collection){
        this(collection, null);
    }

    public ArraySet() {
        this(Collections.emptyList());
    }

    private ArraySet(List<T> collection, Comparator<T> comparator) {
        data = collection;
        size = data.size();
        this.comparator = comparator;
    }

    @Override
    public T lower(T e) {
        return get(find(e, false, true));
    }

    @Override
    public T floor(T e) {
        return get(find(e, true, false));
    }

    @Override
    public T ceiling(T e) {
        return get(find(e, true, true));
    }

    @Override
    public T higher(T e) {
        return get(find(e, false, false));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        T pos = floor((T) o);
        return pos != null && compare(pos, (T) o) == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArraySetIterator(0);
    }

    @Override
    public boolean add(T T) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> descendingIterator() {
        return new ArraySetIterator(size - 1, true);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(new DescendingList(data), comparator.reversed());
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int from = Math.max(find(fromElement, fromInclusive, fromInclusive), 0);
        int to = Math.min(find(toElement, toInclusive, !toInclusive), size - 1);
        if (from > to) {
            return new ArraySet<>();
        }
        return new ArraySet<>(data.subList(from, to + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>();
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty()) {
            return new ArraySet<>();
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return data.get(size - 1);
    }
}
