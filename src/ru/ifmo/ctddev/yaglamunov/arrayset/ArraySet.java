package ru.ifmo.ctddev.yaglamunov.arrayset;


import java.util.*;

public class ArraySet implements NavigableSet<Integer> {

    private final static ArraySet emptyArraySet = new ArraySet();

    private class ArraySetIterator implements Iterator<Integer> {
        private int position;

        ArraySetIterator(int position) {
            this.position = position;
        }

        @Override
        public boolean hasNext() {
            return position < right;
        }

        @Override
        public Integer next() {
            return data[position++];
        }
    }

    private final Integer[] data;
    private final Comparator<Integer> comparator;
    private final Comparator<Integer> officialComparator;
    private final int left;
    private final int right;
    private final int size;

    private int find(Integer e, boolean inclusive, boolean lowerBound) {
        int l = left - 1;
        int r = right;
        while (r - l > 1) {
            int m = (l + r) / 2;
            if ((comparator.compare(data[m], e) < 0 && lowerBound) ||
                    (comparator.compare(data[m], e) <= 0 && !lowerBound)) {
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
        Integer[] tmp = new Integer[collection.size()];
        int position = 0;
        for (int i : collection) {
            tmp[position++] = i;
        }
        Arrays.sort(tmp, this.comparator);
        int count = (tmp.length > 0 ? 1 : 0);
        for (int i = 0; i < tmp.length - 1; i++) {
            if (this.comparator.compare(tmp[i], tmp[i + 1]) != 0) {
                count++;
            }
        }
        data = new Integer[count];
        count = 0;
        for (int i = 0; i < tmp.length; i++) {
            if (i == 0 || this.comparator.compare(tmp[i], tmp[i - 1]) != 0) {
                data[count++] = tmp[i];
            }
        }
        left = 0;
        right = data.length;
        size = data.length;
    }

    public ArraySet(Collection<Integer> collection) {
        this(collection, null);
    }

    public ArraySet() {
        this(Collections.emptyList());
    }

    private ArraySet(ArraySet other, int left, int right) {
        data = other.data;
        comparator = other.comparator;
        officialComparator = other.officialComparator;
        this.left = left;
        this.right = right;
        this.size = right - left;
    }

    @Override
    public Integer lower(Integer e) {
        int position = find(e, false, true);
        if (position < left) return null;
        return data[position];
    }

    @Override
    public Integer floor(Integer e) {
        int position = find(e, true, false);
        if (position < left) return null;
        return data[position];
    }

    @Override
    public Integer ceiling(Integer e) {
        int position = find(e, true, true);
        if (position >= right) return null;
        return data[position];
    }

    @Override
    public Integer higher(Integer e) {
        int position = find(e, false, false);
        if (position >= right) return null;
        return data[position];
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
        return new ArraySetIterator(left);
    }


    @Override
    public Object[] toArray() {
        return Arrays.copyOfRange(data, left, right);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.getClass().isAssignableFrom(Integer[].class)) {
            if (size <= a.length) {
                for (int i = left; i < right; i++) {
                    a[i - left] = (T) data[i];
                }
                return a;
            } else {
                return Arrays.copyOfRange((T[]) data, left, right);
            }
        } else {
            throw new ArrayStoreException();
        }
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
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
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
        return null;
    }

    @Override
    public NavigableSet<Integer> descendingSet() {
        return null;
    }

    @Override
    public NavigableSet<Integer> subSet(Integer fromElement, boolean fromInclusive, Integer toElement, boolean toInclusive) {
        int from = Math.max(find(fromElement, fromInclusive, fromInclusive), left);
        int to = Math.min(find(toElement, toInclusive, !toInclusive), right - 1);
        if (from > to) return emptyArraySet;
//        return new ArraySet(Arrays.asList(Arrays.copyOfRange(data, from, to + 1)), comparator);
        return new ArraySet(this, from, to + 1);
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
        return data[left];
    }

    @Override
    public Integer last() {
        if (isEmpty()) throw new NoSuchElementException();
        return data[right - 1];
    }
}
