package ru.ifmo.ctddev.yaglamunov.crawler;

class DownloadRequest extends Request {
    String url;

    DownloadRequest(Request request, String url) {
        super(request);
        this.url = url;
        status.addTask();
        currentDepth++;
    }

    DownloadRequest(DownloadRequest other) {
        super(other);
        this.url = other.url;
    }
}
