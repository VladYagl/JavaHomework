package ru.ifmo.ctddev.yaglamunov.crawler;

import info.kgeorgiy.java.advanced.crawler.Document;

/**
 * Contains additional information to {@code DownloadRequest} about downloaded document to extract from.
 */
class ExtractRequest extends DownloadRequest {
    /**
     * Document to extract from
     */
    Document document;

    /**
     * Creates new {@code ExtractRequest} from DownloadRequest and Document
     *
     * @param request  DownloadRequest
     * @param document Document to extract from
     */
    ExtractRequest(DownloadRequest request, Document document) {
        super(request);
        status.addTask();
        this.document = document;
    }
}
