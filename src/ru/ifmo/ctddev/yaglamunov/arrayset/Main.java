package ru.ifmo.ctddev.yaglamunov.arrayset;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;

public class Main {
    public static void main(String[] args) {
        Comparator<Integer> comparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return 0;
            }
        };
        ArraySet mySet = new ArraySet(Arrays.asList(1, 2, 3, 4, 5), comparator);
        TreeSet<Integer> treeSet = new TreeSet<Integer>(comparator);
        treeSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        ArraySet emptySet = new ArraySet();
        System.out.println(mySet.floor(4));
        System.out.println(treeSet.floor(4));

        System.out.println(Arrays.toString(new ArraySet(Arrays.asList(1), Integer::compareTo).subSet(1, true, 1, true).toArray()));
        System.out.println(mySet.contains(null));


        for (Integer i : new ArraySet(Arrays.asList(1, 2, 3, 4, 5))) {
            System.out.println(i);
        }
    }
}
