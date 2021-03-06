package ru.ifmo.ctddev.yaglamunov.crawler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Contains general information about request to {@code WebCrawler}: it's status,
 * current and max depths, and where to store result and errors
 */
class Request {
    final Status status;
    int currentDepth;
    final int maxDepth;
    final Queue<String> result;
    final Map<String, IOException> errors;
    final Set<String> requestedUrls;

    /**
     * Creates new request
     *
     * @param result   queue to where store result
     * @param errors   mep to where store errors
     * @param maxDepth max depth of crawling
     */
    Request(Queue<String> result, Map<String, IOException> errors, int maxDepth) {
        this.result = result;
        this.errors = errors;
        this.maxDepth = maxDepth;
        this.currentDepth = 0;
        status = new Status();
        requestedUrls = new HashSet<>();
    }

    /**
     * Creates a copy of other request.
     *
     * @param other request for coping
     */
    protected Request(Request other) {
        status = other.status;
        currentDepth = other.currentDepth;
        maxDepth = other.maxDepth;
        result = other.result;
        errors = other.errors;
        requestedUrls = other.requestedUrls;
    }

    public boolean requestUrl(String url) {
        synchronized (requestedUrls) {
            if (!requestedUrls.contains(url)) {
                requestedUrls.add(url);
                return true;
            } else {
                return false;
            }
        }
    }
}
