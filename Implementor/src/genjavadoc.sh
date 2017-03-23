#!/bin/bash

mkdir -p javadoc/

javadoc -private -link http://docs.oracle.com/javase/8/docs/api/ @javadoc_sources.txt -sourcepath ".:/usr/lib/jvm/java-8-openjdk-amd64/src"  -d "javadoc/"

# -classpath ".:/media/OS/Programming/java-advanced-2017/artifacts/JarImplementorTest.jar"
