package ru.ifmo.ctddev.yaglamunov.crawler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class Request {
    final Status status;
    int currentDepth;
    final int maxDepth;
    final List<String> result;
    final Map<String, IOException> errors;

    Request(List<String> result, Map<String, IOException> errors, int maxDepth) {
        this.result = result;
        this.errors = errors;
        this.maxDepth = maxDepth;
        this.currentDepth = 0;
        status = new Status();
    }

    Request(Request other) {
        status = other.status;
        currentDepth = other.currentDepth;
        maxDepth = other.maxDepth;
        result = other.result;
        errors = other.errors;
    }
}
