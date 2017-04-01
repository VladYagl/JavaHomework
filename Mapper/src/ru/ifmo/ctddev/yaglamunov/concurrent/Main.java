package ru.ifmo.ctddev.yaglamunov.concurrent;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        IterativeParallelism parallelism = new IterativeParallelism(new ParallelMapperImpl(5));
        try {
            Integer i = parallelism.maximum(5, Arrays.asList(2, 42351, 236, 3, 5, 1), Integer::compareTo);
            System.out.println(i);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
