package ru.igorit.andrk.service.processor;

import kz.icode.gov.integration.kgd.ErrorInfo;
import ru.igorit.andrk.config.services.ServiceFaultException;

public class ProcessorException extends ServiceFaultException {

    public ProcessorException(ErrorInfo errorInfo) {
        super(errorInfo);
    }

    public ProcessorException(ErrorInfo errorInfo, Throwable cause) {
        super(errorInfo, cause);
    }
}
