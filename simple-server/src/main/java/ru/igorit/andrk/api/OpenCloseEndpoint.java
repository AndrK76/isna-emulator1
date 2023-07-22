package ru.igorit.andrk.api;


import kz.icode.gov.integration.kgd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import ru.igorit.andrk.config.services.Constants;
import ru.igorit.andrk.service.processor.ProcessResult;
import ru.igorit.andrk.service.processor.ProcessorException;
import ru.igorit.andrk.service.processor.ProcessorFactory;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.utils.RequestMapper;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

import java.util.UUID;

import static java.time.LocalDateTime.now;
import static ru.igorit.andrk.config.services.Constants.DEFAULT_NAMESPACE;
import static ru.igorit.andrk.config.services.Constants.SEND_MESSAGE;
import static ru.igorit.andrk.utils.DataHandler.toXmlDate;

@Endpoint
public class OpenCloseEndpoint {

    private static final Logger log = LoggerFactory.getLogger(OpenCloseEndpoint.class);

    private final MainStoreService mainStoreService;
    private final ProcessorFactory processors;


    public OpenCloseEndpoint(
            MainStoreService mainStoreService,
            ProcessorFactory processors) {
        this.mainStoreService = mainStoreService;
        this.processors = processors;
    }


    @PayloadRoot(namespace = DEFAULT_NAMESPACE, localPart = SEND_MESSAGE)
    @ResponsePayload
    public JAXBElement<SendMessageResponse> sendMessage(
            @RequestPayload JAXBElement<SendMessage> message) throws DatatypeConfigurationException {
        var msg = message.getValue();

        var req = RequestMapper.toModel(msg.getRequest());
        //TODO: to task
        mainStoreService.saveRequest(req);

        var processor = processors.getProcessor(req.getServiceId());
        if (processor == null) {
            throw raiseProcessException(req, "SE_PROCESSOR", "Не найден сервис обработки " + req.getServiceId(), null);
        }

        UUID messageId;
        try {
            var msgId = msg.getRequest().getRequestInfo().getMessageId();
            messageId = UUID.fromString(msgId);
            if (messageId == null) {
                throw new IllegalArgumentException("Message id is empty");
            }
        } catch (IllegalArgumentException e) {
            throw raiseProcessException(req, "SVC_ERRFORMAT", "Ошибка в номере сообщения", e);
        }

        ProcessResult res;

        try {
            res = processor.process(req, messageId);
        } catch (ProcessorException e) {
            throw raiseProcessException(req, e.getErrorInfo().getErrorCode(), e.getErrorInfo().getErrorMessage(), e);
        } catch (Exception e) {
            throw raiseProcessException(req, "SVC_ERRPROCESS", e.getMessage(), e);
        }

        var resp = createResponse(req, true, res.getStatusCode(), res.getStatusMessage(), res.getData());
        mainStoreService.saveResponse(resp);

        var status = new StatusInfo();
        status.setCode(resp.getStatusCode());
        status.setMessage(resp.getStatusMessage());
        var respInfo = new SyncMessageInfoResponse();

        respInfo.setMessageId(resp.getMessageId().toString());
        respInfo.setCorrelationId(resp.getCorrelationId().toString());

        respInfo.setResponseDate(toXmlDate(resp.getResponseDate()));
        respInfo.setStatus(status);
        var data = new ResponseData();
        data.setData(res.getDataIgnoreCR());
        var syncResponse = new SyncSendMessageResponse();
        syncResponse.setResponseInfo(respInfo);
        syncResponse.setResponseData(data);
        var response = new SendMessageResponse();
        //response.setResponse(syncResponse); //Changed in XSD
        response.setReturn(syncResponse);

        return createResponseJaxbElement(response, SendMessageResponse.class);
    }

    private <T> JAXBElement<T> createResponseJaxbElement(T object, Class<T> clazz) {
        return new JAXBElement<>(new QName(DEFAULT_NAMESPACE, clazz.getSimpleName()), clazz, object);
    }

    private ProcessorException raiseProcessException(ru.igorit.andrk.model.Request request, String errCode, String errMessage, Throwable e) {
        var resp = createResponse(request, false, errCode, errMessage, null);
        mainStoreService.saveResponse(resp);

        ErrorInfo err = new ErrorInfo();
        err.setErrorCode(errCode);
        err.setErrorMessage(errMessage);
        err.setErrorDate(toXmlDate(resp.getResponseDate()));
        return new ProcessorException(err, e);
    }

    private ru.igorit.andrk.model.Response createResponse(
            ru.igorit.andrk.model.Request request,
            boolean isSuccess,
            String statusCode,
            String statusMessage,
            String data) {
        var ret = new ru.igorit.andrk.model.Response(request);
        ret.setIsSuccess(isSuccess);
        ret.setStatusCode(statusCode);
        ret.setStatusMessage(statusMessage);
        ret.setData(data);
        return ret;
    }

}
