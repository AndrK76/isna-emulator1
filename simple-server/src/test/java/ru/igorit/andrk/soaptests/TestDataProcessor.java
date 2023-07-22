package ru.igorit.andrk.soaptests;

import ru.igorit.andrk.config.services.Constants;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.service.DataProcessor;
import ru.igorit.andrk.service.processor.ProcessResult;

import java.util.UUID;

public class TestDataProcessor implements DataProcessor {
    @Override
    public String document() {
        return Constants.OPEN_CLOSE_SERVICE;
    }

    @Override
    public ProcessResult process(Request request, UUID messageId) {
        var ret = ProcessResult.successResult();
        ret.setData(request.getData());
        return ret;
    }

    @Override
    public void configure(byte[] config) {

    }
}
