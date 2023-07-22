package ru.igorit.andrk.controllertests;

import org.mockito.stubbing.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.igorit.andrk.model.*;
import ru.igorit.andrk.service.MainStoreService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class FullStorageInitiator {
    public static final int COUNT_REQUESTS = 10;

    private final List<Request> requests = new ArrayList<>();
    private final List<Response> responses = new ArrayList<>();
    private final List<OpenCloseRequest> ocRequests = new ArrayList<>();
    private final List<OpenCloseResponse> ocResponses = new ArrayList<>();

    public List<Request> getRequests() {
        return requests;
    }

    public List<Response> getResponses() {
        return responses;
    }

    public List<OpenCloseRequest> getOcRequests() {
        return ocRequests;
    }

    public List<OpenCloseResponse> getOcResponses() {
        return ocResponses;
    }

    public void initFullMockStorage(boolean initOpenClose) {
        for (int i = 0; i < COUNT_REQUESTS; i++) {
            var request = Request.builder()
                    .id((long) i)
                    .messageId(UUID.randomUUID())
                    .serviceId("TEST")
                    .messageDate(OffsetDateTime.now().withNano(0))
                    .data("запрос" + i)
                    .build();
            if (i % 2 == 0) {
                var response = Response.builder()
                        .id((long) i * 3)
                        .request(request)
                        .messageId(UUID.randomUUID())
                        .correlationId(request.getMessageId())
                        .serviceId(request.getServiceId())
                        .isSuccess(true)
                        .responseDate(OffsetDateTime.now().withNano(0))
                        .statusCode("Code")
                        .statusMessage("Сообщение")
                        .data("Ответ" + (i * 3))
                        .build();
                responses.add(response);
                if (initOpenClose) {
                    var ocRequest = OpenCloseRequest.builder()
                            .id((long) i)
                            .reference("Qwertiop_" + ((long) i * 5))
                            .codeForm("A000")
                            .accounts(new ArrayList<>())
                            .notifyDate(LocalDateTime.now().withSecond(0).withNano(0))
                            .rawRequest(request)
                            .build();
                    ocRequests.add(ocRequest);
                    var ocRequestAccount = OpenCloseRequestAccount.builder()
                            .id((long) i * 8)
                            .sort(0)
                            .account("000000000000001")
                            .operType(1)
                            .accountType("05")
                            .build();
                    ocRequest.getAccounts().add(ocRequestAccount);
                    var ocResponse = OpenCloseResponse.builder()
                            .id((long) i)
                            .request(ocRequest)
                            .reference(ocRequest.getReference())
                            .codeForm("AC0")
                            .notifyDate(LocalDateTime.now().withSecond(0).withNano(0))
                            .accounts(new ArrayList<>())
                            .build();
                    ocResponses.add(ocResponse);
                    var OcResponseAccount = OpenCloseResponseAccount.builder()
                            .id((long) i * 7)
                            .response(ocResponse)
                            .sort(0)
                            .account(ocRequestAccount.getAccount())
                            .resultCode("00")
                            .build();
                    ocResponse.getAccounts().add(OcResponseAccount);
                }
            }
            requests.add(request);
        }

    }

    public void initFullMockStoreService(MainStoreService mainStoreService, boolean initOpenClose) {
        when(mainStoreService.getRequests(any(), anyInt()))
                .thenAnswer((Answer<Page<Request>>) invocation -> {
                    var count = (int) invocation.getArguments()[1];
                    return new PageImpl<>(requests, PageRequest.of(0, count), requests.size());
                });
        when(mainStoreService.getResponsesForRequests(any()))
                .thenAnswer((Answer<List<Response>>) invocation -> {
                    var reqs = (List<Request>) invocation.getArguments()[0];
                    return responses.stream()
                            .filter(r -> reqs.contains(r.getRequest()))
                            .collect(Collectors.toList());
                });
        when(mainStoreService.getRequestById(any()))
                .thenAnswer((Answer<Request>) invocation -> {
                    Long id = (Long) invocation.getArguments()[0];
                    return requests.stream()
                            .filter(r -> r.getId() == id)
                            .findFirst().orElse(null);
                });
        when(mainStoreService.getIdForNewestRequestWithOffset(any(), anyInt()))
                .thenAnswer((Answer<Long>) invocation -> {
                    Long currRequestId = (Long) invocation.getArguments()[0];
                    int offset = (int) invocation.getArguments()[1];
                    var retRequest = requests.stream()
                            .filter(r -> r.getId() > currRequestId)
                            .sorted(Comparator.comparing(Request::getId))
                            .skip(offset).findFirst();
                    return retRequest.isPresent() ? retRequest.get().getId() : null;
                });
        if (initOpenClose) {
            when(mainStoreService.getOpenCloseRequests(any(), anyInt()))
                    .thenAnswer((Answer<Page<OpenCloseRequest>>) invocation -> {
                        var count = (int) invocation.getArguments()[1];
                        return new PageImpl<>(ocRequests, PageRequest.of(0, count), ocRequests.size());
                    });
            when(mainStoreService.getOpenCloseResponsesForRequests(any()))
                    .thenAnswer((Answer<List<OpenCloseResponse>>) invocation -> {
                        var reqs = (List<OpenCloseRequest>) invocation.getArguments()[0];
                        return ocResponses.stream()
                                .filter(r -> reqs.contains(r.getRequest()))
                                .collect(Collectors.toList());
                    });
            when(mainStoreService.getOpenCloseRequestById(any(), anyBoolean()))
                    .thenAnswer((Answer<OpenCloseRequest>) invocation -> {
                        Long id = (Long) invocation.getArguments()[0];
                        return ocRequests.stream()
                                .filter(r -> r.getId() == id)
                                .findFirst().orElse(null);
                    });
            when(mainStoreService.getIdForNewestOpenCloseRequestWithOffset(any(), anyInt()))
                    .thenAnswer((Answer<Long>) invocation -> {
                        Long currRequestId = (Long) invocation.getArguments()[0];
                        int offset = (int) invocation.getArguments()[1];
                        var retRequest = ocRequests.stream()
                                .filter(r -> r.getId() > currRequestId)
                                .sorted(Comparator.comparing(OpenCloseRequest::getId))
                                .skip(offset).findFirst();
                        return retRequest.isPresent() ? retRequest.get().getId() : null;
                    });
            when(mainStoreService.getOpenCloseResponseById(any(), anyBoolean()))
                    .thenAnswer((Answer<OpenCloseResponse>) invocation -> {
                        Long id = (Long) invocation.getArguments()[0];
                        return ocResponses.stream()
                                .filter(r -> r.getId() == id)
                                .findFirst().orElse(null);
                    });
        }
    }


}
