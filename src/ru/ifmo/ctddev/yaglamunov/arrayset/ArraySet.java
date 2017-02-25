package ru.ifmo.ctddev.yaglamunov.arrayset;


import java.util.*;

@SuppressWarnings("WeakerAccess")
public class ArraySet extends AbstractSet<Integer> implements NavigableSet<Integer> {

    private final static ArraySet emptyArraySet = new ArraySet();

    private class ArraySetIterator implements Iterator<Integer> {
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
        public Integer next() {
            return data.get(!descending ? position++ : position--);
        }
    }

    private class DescendingList extends AbstractList<Integer> {
        private final List<Integer> data;

        DescendingList(List<Integer> other) {
            data = other;
        }

        @Override
        public Integer get(int index) {
            return data.get(size() - index - 1);
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    private final List<Integer> data;
    private final Comparator<Integer> comparator;
    private final Comparator<Integer> officialComparator;
    private final int size;

    private int find(Integer e, boolean inclusive, boolean lowerBound) {
        int l = -1;
        int r = size;
        while (r - l > 1) {
            int m = (l + r) / 2;
            if ((comparator.compare(data.get(m), e) < 0 && lowerBound) ||
                    (comparator.compare(data.get(m), e) <= 0 && !lowerBound)) {
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

    public ArraySet(Collection<Integer> collection, Comparator<Integer> comparator) {
        officialComparator = comparator;
        if (comparator == null) {
            this.comparator = Integer::compareTo;
        } else {
            this.comparator = comparator;
        }
        ArrayList<Integer> tmp = new ArrayList<>(collection);
        tmp.sort(this.comparator);
        data = new ArrayList<>();
        for (int i = 0; i < tmp.size(); i++) {
            if (i == 0 || this.comparator.compare(tmp.get(i), tmp.get(i - 1)) != 0) {
                data.add(tmp.get(i));
            }
        }
        size = data.size();
    }

    public ArraySet(Collection<Integer> collection) {
        this(collection, null);
    }

    public ArraySet() {
        this(Collections.emptyList());
    }

    private ArraySet(List<Integer> collection, Comparator<Integer> comparator, Comparator<Integer> officialComparator) {
        data = collection;
        size = data.size();
        this.comparator = comparator;
        this.officialComparator = officialComparator;
    }

    @Override
    public Integer lower(Integer e) {
        int position = find(e, false, true);
        if (position < 0) return null;
        return data.get(position);
    }

    @Override
    public Integer floor(Integer e) {
        int position = find(e, true, false);
        if (position < 0) return null;
        return data.get(position);
    }

    @Override
    public Integer ceiling(Integer e) {
        int position = find(e, true, true);
        if (position >= size) return null;
        return data.get(position);
    }

    @Override
    public Integer higher(Integer e) {
        int position = find(e, false, false);
        if (position >= size) return null;
        return data.get(position);
    }

    @Override
    public Integer pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer pollLast() {
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
        Integer pos = floor((Integer) o);
        return pos != null && comparator.compare(pos, (Integer) o) == 0;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new ArraySetIterator(0);
    }

    @Override
    public boolean add(Integer integer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
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
    public Iterator<Integer> descendingIterator() {
        return new ArraySetIterator(size - 1, true);
    }

    @Override
    public NavigableSet<Integer> descendingSet() {
        return new ArraySet(new DescendingList(data), comparator.reversed(), (officialComparator == null ? null : officialComparator.reversed()));
    }

    @Override
    public NavigableSet<Integer> subSet(Integer fromElement, boolean fromInclusive, Integer toElement, boolean toInclusive) {
        int from = Math.max(find(fromElement, fromInclusive, fromInclusive), 0);
        int to = Math.min(find(toElement, toInclusive, !toInclusive), size - 1);
        if (from > to) return emptyArraySet;
        return new ArraySet(data.subList(from, to + 1), comparator, officialComparator);
    }

    @Override
    public NavigableSet<Integer> headSet(Integer toElement, boolean inclusive) {
        if (isEmpty()) return emptyArraySet;
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<Integer> tailSet(Integer fromElement, boolean inclusive) {
        if (isEmpty()) return emptyArraySet;
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super Integer> comparator() {
        return officialComparator;
    }

    @Override
    public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<Integer> headSet(Integer toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<Integer> tailSet(Integer fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Integer first() {
        if (isEmpty()) throw new NoSuchElementException();
        return data.get(0);
    }

    @Override
    public Integer last() {
        if (isEmpty()) throw new NoSuchElementException();
        return data.get(size - 1);
    }
}
