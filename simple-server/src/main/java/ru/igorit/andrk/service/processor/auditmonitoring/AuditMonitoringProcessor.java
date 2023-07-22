package ru.igorit.andrk.service.processor.auditmonitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.service.DataProcessor;
import ru.igorit.andrk.service.processor.ProcessResult;

import java.util.UUID;

@Service
public class AuditMonitoringProcessor implements DataProcessor {

    private static final String DOCUMENT="AUDIT_MONITORING";
    private static final Logger log = LoggerFactory.getLogger(AuditMonitoringProcessor.class);

    @Override
    public String document() {
        return DOCUMENT;
    }

    @Override
    public ProcessResult process(Request request, UUID messageId) {
        return null;
    }

    @Override
    public void configure(byte[] config) {
        log.debug("apply config");
    }
}
