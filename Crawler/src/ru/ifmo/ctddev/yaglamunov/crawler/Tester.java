package ru.ifmo.ctddev.yaglamunov.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import ru.ifmo.ctddev.yaglamunov.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Tester {
    private static final int downloaders = 20;
    private static final int extractors = 15;
    private static final int perHost = 10;
    private static final int depth = 2;

    private static final String[] urls = {
            "http://www.kgeorgiy.info",
            "http://sorokin.github.io/cpp-course/",
            "http://sorokin.github.io/cpp-course/task-1.html",
            "http://rigaux.org/language-study/syntax-across-languages-per-language/OCaml.html#4",
            "http://www.kgeorgiy.info",
            "http://sorokin.github.io/cpp-course/",
            "http://sorokin.github.io/cpp-course/task-1.html",
            "http://rigaux.org/language-study/syntax-across-languages-per-language/OCaml.html#4",
            "http://www.kgeorgiy.info",
            "http://sorokin.github.io/cpp-course/",
            "http://sorokin.github.io/cpp-course/task-1.html",
            "http://rigaux.org/language-study/syntax-across-languages-per-language/OCaml.html#4",
            "http://www.kgeorgiy.info",
            "http://sorokin.github.io/cpp-course/",
            "http://sorokin.github.io/cpp-course/task-1.html",
            "http://rigaux.org/language-study/syntax-across-languages-per-language/OCaml.html#4",
            "https://ru.op.gg/champion/azir/statistics/mid"
    };

    private static final Path path = Paths.get("Downloads" + File.separator);

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(urls.length);

        if (Files.exists(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!Files.isDirectory(file)) Files.deleteIfExists(file);
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (Crawler crawler = new WebCrawler(new CachingDownloader(path), downloaders, extractors, perHost)) {
            CountDownLatch countDown = new CountDownLatch(urls.length);
            for (String url : urls) {
                threadPool.execute(() -> {
                    crawler.download(url, depth);
                    countDown.countDown();
                    Log.println("[" + url + "] ---> downloading finished");
                    Log.save();
                });
            }
            countDown.await();
            threadPool.shutdownNow();
            threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
