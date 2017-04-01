package ru.ifmo.ctddev.yaglamunov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WebCrawler implements Crawler {

    private final int perHostLimit;
    private final Map<String, Host> hosts = new HashMap<>();

    private final List<Thread> downloaders = new ArrayList<>();
    private final List<Thread> extractors = new ArrayList<>();

    private final BlockingQueue<DownloadRequest> downloadRequests = new LinkedBlockingQueue<>();
    private final BlockingQueue<ExtractRequest> extractRequests = new LinkedBlockingQueue<>();

    private final Set<String> requestedUrls = new HashSet<>();

    private Host getHost(String url) throws MalformedURLException {
        String hostUrl = URLUtils.getHost(url);
        if (!hosts.containsKey(hostUrl)) {
            hosts.put(hostUrl, new Host());
        }
        return hosts.get(hostUrl);
    }

    @SuppressWarnings("WeakerAccess")
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        perHostLimit = perHost;

        for (int i = 0; i < downloaders; i++) {
            this.downloaders.add(new Thread(new Loader(downloader)));
            this.downloaders.get(i).start();
        }

        for (int i = 0; i < extractors; i++) {
            this.extractors.add(new Thread(new Extractor()));
            this.extractors.get(i).start();
        }
    }

    @Override
    public Result download(String url, int depth) {
        List<String> result = new LinkedList<>();
        Map<String, IOException> errors = new HashMap<>();

        Request request = new Request(result, errors, depth);
        try {
            downloadRequests.put(new DownloadRequest(request, url));
            request.status.waitFinish();
        } catch (InterruptedException e) {
            return null;
        }

        return new Result(result, errors);
    }

    @Override
    public void close() {
        downloaders.forEach(Thread::interrupt);
        extractors.forEach(Thread::interrupt);
        try {
            for (Thread thread : downloaders) {
                thread.join();
            }
            for (Thread thread : extractors) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.currentThread().interrupt();
    }

    public static void main(String[] args) throws IOException {
        int downloaders = 10;
        int extractors = 10;
        int perHost = 1;
        if (args.length > 1) {
            downloaders = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            extractors = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            perHost = Integer.parseInt(args[3]);
        }

        Path path = Paths.get("Downloads" + File.separator);
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!Files.isDirectory(file)) Files.delete(file);
                return super.visitFile(file, attrs);
            }
        });
        try (Crawler crawler = new WebCrawler(new CachingDownloader(path), downloaders, extractors, perHost)) {

            Result links = crawler.download(args[0], 3);

            System.out.println(links.getDownloaded().size());
            links.getDownloaded().forEach(System.out::println);

            System.out.println();
            System.out.println("errors");
            links.getErrors().forEach((url, exception) -> System.out.println(url + ": " + exception));
        }
    }

    private class Host {
        private final Queue<DownloadRequest> queue = new LinkedList<>();
        private volatile int workingThreads = 0;

        synchronized boolean lockLoader() {
            if (workingThreads < perHostLimit) {
                workingThreads++;
                return true;
            } else {
                return false;
            }
        }

        synchronized void delay(DownloadRequest request) {
            queue.add(request);
        }

        synchronized void removeLoader() {
            workingThreads--;
            if (workingThreads < perHostLimit) {
                if (!queue.isEmpty()) {
                    downloadRequests.add(queue.remove());
                }
            }
        }
    }

    private class Loader implements Runnable {
        private final Downloader downloader;

        private Loader(Downloader downloader) {
            this.downloader = downloader;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DownloadRequest request = downloadRequests.take();
                    try {
                        Host host = getHost(request.url);
                        if (host.lockLoader()) {
                            Document document = downloader.download(request.url);
                            host.removeLoader();
                            request.result.add(request.url);
                            if (request.currentDepth < request.maxDepth) {
                                extractRequests.put(new ExtractRequest(request, document));
                            }
                            request.status.finishTask();
                        } else {
                            host.delay(request);
                        }
                    } catch (IOException e) {
                        request.errors.put(request.url, e);
                        request.status.finishTask();
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private class Extractor implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ExtractRequest request = extractRequests.take();
                    try {
                        List<String> urls = request.document.extractLinks();
                        for (String url : urls) {
                            synchronized (requestedUrls) {
                                if (!requestedUrls.contains(url)) {
                                    downloadRequests.put(new DownloadRequest(new Request(request), url));
                                    requestedUrls.add(url);
                                }
                            }
                        }
                    } catch (IOException e) {
                        request.errors.put(request.url, e);
                    }
                    request.status.finishTask();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}

