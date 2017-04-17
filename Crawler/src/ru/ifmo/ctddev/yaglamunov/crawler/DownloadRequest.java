package ru.ifmo.ctddev.yaglamunov.crawler;

/**
 * Contains additional information to {@code Request} about download url.
 */
class DownloadRequest extends Request {
    /**
     * URL to download from
     */
    String url;

    /**
     * Creates new {@code DownloadRequest] from request and URL
     * <p>
     * adds unfinished task to request status
     *
     * @param request
     * @param url
     */
    DownloadRequest(Request request, String url) {
        super(request);
        this.url = url;
        status.addTask();
        currentDepth++;
        synchronized (requestedUrls) {
            requestedUrls.add(url);
        }
    }

    /**
     * Creates a copy of other request.
     *
     * @param other request for coping
     */
    protected DownloadRequest(DownloadRequest other) {
        super(other);
        this.url = other.url;
    }
}
