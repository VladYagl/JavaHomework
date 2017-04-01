mkdir -p ../../res/

javac -cp ".:/media/OS/Programming/java-advanced-2017/artifacts/JarImplementorTest.jar" @sources.txt -d "../../res/"

cp manifest.mf ../../res/manifest.mf
cd ../../res/

jar cfm Implementor.jar manifest.mf ./ru/ifmo/ctddev/yaglamunov/implementor/*.class

rm -rf info/
