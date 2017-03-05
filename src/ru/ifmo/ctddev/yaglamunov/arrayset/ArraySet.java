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
        int l = -1;
        int r = size;
        while (r - l > 1) {
            int m = (l + r) / 2;
            if ((Objects.compare(data.get(m), e, comparator) < 0 && lowerBound) ||
                    (Objects.compare(data.get(m), e, comparator) <= 0 && !lowerBound)) {
                l = m;
            } else {
                r = m;
            }
        }
        if (inclusive == lowerBound) {
            return r;
        } else {
            return l;
        }
    }

    public ArraySet(Collection<T> collection, Comparator<T> comparator) {
        this.comparator = comparator;
        ArrayList<T> tmp = new ArrayList<>(collection);
        tmp.sort(this.comparator);
        data = new ArrayList<>();
        for (int i = 0; i < tmp.size(); i++) {
            if (i == 0 || Objects.compare(tmp.get(i), tmp.get(i - 1), comparator) != 0) {
                data.add(tmp.get(i));
            }
        }
        size = data.size();
    }

    public ArraySet(Collection<? extends T> collection) {
        this((Collection<T>) collection, null);
    }

    public ArraySet() {
        this(Collections.emptyList());
    }

    private ArraySet(List<T> collection, Comparator<T> comparator, boolean my) {
        data = collection;
        size = data.size();
        this.comparator = comparator;
    }

    @Override
    public T lower(T e) {
        int position = find(e, false, true);
        if (position < 0) {
            return null;
        }
        return data.get(position);
    }

    @Override
    public T floor(T e) {
        int position = find(e, true, false);
        if (position < 0) {
            return null;
        }
        return data.get(position);
    }

    @Override
    public T ceiling(T e) {
        int position = find(e, true, true);
        if (position >= size) {
            return null;
        }
        return data.get(position);
    }

    @Override
    public T higher(T e) {
        int position = find(e, false, false);
        if (position >= size) {
            return null;
        }
        return data.get(position);
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
        return pos != null && Objects.compare(pos, (T) o, comparator) == 0;
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
        return new ArraySet<>(new DescendingList(data), comparator.reversed(), true);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int from = Math.max(find(fromElement, fromInclusive, fromInclusive), 0);
        int to = Math.min(find(toElement, toInclusive, !toInclusive), size - 1);
        if (from > to) {
            return new ArraySet<>();
        }
        return new ArraySet<>(data.subList(from, to + 1), comparator, true);
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
