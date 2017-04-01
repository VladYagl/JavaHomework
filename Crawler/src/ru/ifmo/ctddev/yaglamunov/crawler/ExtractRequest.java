package ru.ifmo.ctddev.yaglamunov.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;

class ExtractRequest extends DownloadRequest {
    Document document;

    ExtractRequest(DownloadRequest request, Document document) {
        super(request);
        status.addTask();
        this.document = document;
    }
}
