package ru.igorit.andrk.service;

import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.service.processor.ProcessResult;

import java.util.UUID;

public interface DataProcessor {
    String document();

    ProcessResult process(Request request, UUID messageId);

    void configure(byte[] config);
}
