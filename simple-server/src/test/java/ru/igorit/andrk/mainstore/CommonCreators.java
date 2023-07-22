package ru.igorit.andrk.mainstore;

import ru.igorit.andrk.model.OpenCloseRequest;
import ru.igorit.andrk.model.OpenCloseRequestAccount;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.Response;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

public class CommonCreators {
    public static Request makeMainRequest(String serviceId) {
        return Request.builder()
                .messageId( UUID.randomUUID())
                .correlationId( UUID.randomUUID())
                .serviceId(serviceId)
                .messageDate(OffsetDateTime.now().withNano(0))
                .build();
    }

    public static Request makeMainRequest(){
        return makeMainRequest("TEST");
    }

    public static Response makeMainResponse(Request request, boolean isSuccess) {
        var resp = new Response(request);
        resp.setIsSuccess(isSuccess);
        resp.setStatusMessage("status message");
        resp.setStatusCode("code");
        return resp;
    }

    public static Response makeMainResponse(Request request) {
        return makeMainResponse(request, true);
    }

    public static OpenCloseRequest makeOCRequest(Request request, int accountCounts) {
        OpenCloseRequest ocRequest = new OpenCloseRequest(request);
        ocRequest.setCodeForm("TEST");
        ocRequest.setNotifyDate(LocalDateTime.now());
        byte[] array = new byte[10];
        new Random().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        ocRequest.setReference(generatedString);
        var accounts = ocRequest.getAccounts();
        for (int i = 0; i < accountCounts; i++) {
            var account = new OpenCloseRequestAccount();
            account.setRequest(ocRequest);
            account.setSort(i);
            account.setAccount("QWERTY123456");
            accounts.add(account);
        }
        return ocRequest;
    }

    public OpenCloseRequest makeOCRequest(
            Request request,
            LocalDateTime operDate,
            Integer operType,
            String[] accountNums) {
        OpenCloseRequest ocRequest = new OpenCloseRequest(request);
        ocRequest.setCodeForm("TEST");
        ocRequest.setNotifyDate(LocalDateTime.now());
        var accounts = ocRequest.getAccounts();
        for (int i = 0; i < accountNums.length; i++) {
            var account = new OpenCloseRequestAccount();
            account.setRequest(ocRequest);
            account.setSort(i);
            account.setOperType(operType);
            account.setOperDate(operDate);
            account.setAccount(accountNums[i]);
            accounts.add(account);
        }
        return ocRequest;
    }
}
