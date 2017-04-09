package ru.ifmo.ctddev.yaglamunov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import ru.ifmo.ctddev.yaglamunov.Log;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

/**
 * Implementation of class {@code Crawler}
 */
public class WebCrawler implements Crawler {

    /**
     * Max number of threads per host
     */
    private final int perHostLimit;

    /**
     * Map between urls and their {@link Host} object
     */
    private final Map<String, Host> hosts = new HashMap<>();

    /**
     * Thread pools for downloaders and extractors
     */
    private final ExecutorService downloadPool;
    private final ExecutorService extractPool;

    /**
     * Provided {@code Downloader}
     */
    private final Downloader downloader;

    /**
     * Set of already add to request URLs
     */
    private final Set<String> requestedUrls = new HashSet<>();

    /**
     * Returns a {@link Host} object witch mapped to provided URL
     *
     * @param url URL to return Host
     * @return a mapped Host object
     * @throws MalformedURLException wrong URL provided
     */
    private Host getHost(String url) throws MalformedURLException {
        String hostUrl = URLUtils.getHost(url);
        synchronized (hosts) {
            if (!hosts.containsKey(hostUrl)) {
                hosts.put(hostUrl, new Host());
            }
            return hosts.get(hostUrl);
        }
    }

    /**
     * Creates a new {@code WebCrawler}
     *
     * @param downloader  to be used to download URLs
     * @param downloaders max number of downloading threads
     * @param extractors  max number of extracting threads
     * @param perHost     max number of threads downloading from one host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        perHostLimit = perHost;
        this.downloader = downloader;

        downloadPool = Executors.newFixedThreadPool(downloaders);
        extractPool = Executors.newFixedThreadPool(extractors);
    }

    /**
     * Recursively downloads all web pages
     *
     * @param url   URL to start recursion from
     * @param depth max depth of recursion
     * @return a {@link Result} object for downloaded urls
     */
    @Override
    public Result download(String url, int depth) {
        if (Log.getInt("test") == null) {
            Log.putInt("test", 1);
        }
        int testNumber = Log.getInt("test");
        Log.putInt("test", testNumber + 1);
        Log.println();
        Log.println();
        Log.println("Test " + testNumber);

        Queue<String> result = new LinkedBlockingQueue<String>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();

        Request request = new Request(result, errors, depth);
        try {
            requestedUrls.add(url);
            downloadPool.execute(new Loader(downloader, new DownloadRequest(request, url)));
            request.status.waitFinish();
        } catch (InterruptedException e) {
            return null;
        }

        List<String> list = new ArrayList<>();
        list.addAll(result);
        return new Result(list, errors);
    }

    /**
     * Stops all working threads and awaits their termination
     */
    @Override
    public void close() {
        downloadPool.shutdown();
        extractPool.shutdown();
        try {
            downloadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
            extractPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Contains information of how many threads downloading from host
     */
    private class Host {
        private final Queue<DownloadRequest> queue = new LinkedList<>();
        private volatile int workingThreads = 0;

        /**
         * Tries to add another downloader to host, returns false if reached max number of threads
         *
         * @return false if reached max number of downloaders else true
         */
        synchronized boolean lockLoader() {
            if (workingThreads < perHostLimit) {
                workingThreads++;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Delays request until not maximum of downloaders
         *
         * @param request request to delay
         */
        synchronized void delay(DownloadRequest request) {
            queue.add(request);
        }

        /**
         * Decreases number of downloading threads
         */
        synchronized void removeLoader() {
            workingThreads--;
            if (workingThreads < perHostLimit) {
                if (!queue.isEmpty()) {
                    downloadPool.execute(new Loader(downloader, queue.remove()));
                }
            }
        }
    }

    /**
     * Task to download from {@link DownloadRequest}
     */
    private class Loader implements Runnable {
        private final Downloader downloader;
        private final DownloadRequest request;

        /**
         * Creates a new downloader task
         *
         * @param downloader provided downloader
         * @param request    request
         */
        private Loader(Downloader downloader, DownloadRequest request) {
            this.downloader = downloader;
            this.request = request;
        }

        /**
         * Downloads {@link Document} for {@link DownloadRequest}'s link and add new extraction task
         */
        @Override
        public void run() {
            Log.println(request.url);
            try {
                Host host = getHost(request.url);
                if (host.lockLoader()) {
                    Log.println(request.url + "  --start--");
                    Document document = downloader.download(request.url);
                    request.result.add(request.url);
                    Log.println(request.url + "  --finish--");
                    host.removeLoader();
                    if (request.currentDepth < request.maxDepth) {
                        extractPool.execute(new Extractor(new ExtractRequest(request, document)));
                    }
                    request.status.finishTask();
                } else {
                    Log.println(request.url + "  ----> delay");
                    host.delay(request);
                }
            } catch (IOException e) {
                Log.println(request.url + "  --error-->  " + e.toString());
                request.errors.put(request.url, e);
                request.status.finishTask();
            }
        }
    }

    /**
     * Task to extract from {@link Document}
     */
    private class Extractor implements Runnable {
        private final ExtractRequest request;

        /**
         * Creates new Extractor
         *
         * @param request request for extraction
         */
        private Extractor(ExtractRequest request) {
            this.request = request;
        }

        /**
         * Extracts all links from {@link Document} and adds new downloading tasks
         */
        @Override
        public void run() {
            try {
                List<String> urls = request.document.extractLinks();
                for (String url : urls) {
                    synchronized (requestedUrls) {
                        if (!requestedUrls.contains(url)) {
                            downloadPool.execute(new Loader(downloader, new DownloadRequest(request, url)));
                            requestedUrls.add(url);
                        }
                    }
                }
            } catch (IOException e) {
                request.errors.put(request.url, e);
            }
            request.status.finishTask();
        }
    }

    /**
     * Main function.
     * <p>
     * Usage:
     * <li>{@code url [downloads [extractors [perHost]]]} - recursively downloads all links from URL </li>
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) throws IOException {
        int downloaders = Integer.MAX_VALUE;
        int extractors = Integer.MAX_VALUE;
        int perHost = Integer.MAX_VALUE;
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
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isDirectory(file)) Files.deleteIfExists(file);
                    return super.visitFile(file, attrs);
                }
            });
        }
        try (Crawler crawler = new WebCrawler(new CachingDownloader(path), downloaders, extractors, perHost)) {

            Result links = crawler.download(args[0], 2);

            System.out.println(links.getDownloaded().size());
            links.getDownloaded().forEach(System.out::println);

            System.out.println();
            System.out.println("errors");
            links.getErrors().forEach((url, exception) -> System.out.println(url + ": " + exception));
        }
    }
}

