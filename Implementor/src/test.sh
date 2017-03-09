#!/bin/bash

git pull

mkdir -p ../../res/

javac -cp ".:/media/OS/Programming/java-advanced-2017/artifacts/JarImplementorTest.jar" @sources.txt -d "../../res/"

cd ../../res/

java -cp ".:/media/OS/Programming/java-advanced-2017/artifacts/JarImplementorTest.jar:/media/OS/Programming/java-advanced-2017/lib/hamcrest-core-1.3.jar:/media/OS/Programming/java-advanced-2017/lib/jsoup-1.8.1.jar:/media/OS/Programming/java-advanced-2017/lib/junit-4.11.jar:/media/OS/Programming/java-advanced-2017/lib/quickcheck-0.6.jar" info.kgeorgiy.java.advanced.implementor.Tester jar-class ru.ifmo.ctddev.yaglamunov.implementor.Implementor
