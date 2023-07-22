package ru.igorit.andrk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.igorit.andrk.model.*;

import java.util.List;
import java.util.UUID;

public interface MainStoreService {
    Request saveRequest(Request request);
    Page<Request> getRequests(Long lastId, int count);
    Long getIdForNewestRequestWithOffset(Long currRequestId, int offset);
    Request getRequestById(Long id);
    boolean containRequestWithMessageId(Request request);

    Response saveResponse(Response response);
    Page<Response> getResponses(Long lastId, int count);
    List<Response> getResponsesForRequests(List<Request> requests);
    Response getResponse(Long id);

    OpenCloseRequest saveOpenCloseRequest(OpenCloseRequest request);
    Page<OpenCloseRequest> getOpenCloseRequests(Long lastId, int count);
    Long getIdForNewestOpenCloseRequestWithOffset(Long currRequestId, int offset);
    OpenCloseRequest getOpenCloseRequestById(Long id, boolean loadAccounts);
    OpenCloseRequest getOpenCloseRequestById(Long id);
    boolean containOpenCloseRequestWithReference(OpenCloseRequest request);

    OpenCloseResponse saveOpenCloseResponse(OpenCloseResponse response);
    OpenCloseResponse getOpenCloseResponseById(Long id, boolean loadAccounts);
    OpenCloseResponse getOpenCloseResponseById(Long id);
    Page<OpenCloseResponse> getOpenCloseResponses(Long lastId, int count);
    List<OpenCloseResponse> getOpenCloseResponsesForRequests(List<OpenCloseRequest> requests);
    OpenCloseResponseAccount lastResponseForAccountByOperTypeAndResult(String accountNum, Integer operType, String resultCode);

    List<StoredSetting> getSettingsByGroup(String groupName);
    StoredSetting saveSetting(StoredSetting setting);
    StoredSetting getSetting(StoredSettingKey key);

}
