package ru.igorit.andrk.service.store;

import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.igorit.andrk.model.*;
import ru.igorit.andrk.repository.main.*;
import ru.igorit.andrk.service.MainStoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Builder
public class MainStoreServiceJPAImpl implements MainStoreService {

    private final RequestRepository reqRepo;
    private final ResponseRepository respRepo;
    private final OpenCloseRequestRepository ocReqRepo;
    private final OpenCloseResponseRepository ocRespRepo;

    private final SettingRepository setRepo;

    public MainStoreServiceJPAImpl(
            RequestRepository reqRepo,
            ResponseRepository respRepo,
            OpenCloseRequestRepository ocReqRepo,
            OpenCloseResponseRepository ocRespRepo,
            SettingRepository setRepo) {
        this.reqRepo = reqRepo;
        this.respRepo = respRepo;
        this.ocReqRepo = ocReqRepo;
        this.ocRespRepo = ocRespRepo;
        this.setRepo = setRepo;
    }

    @Override
    @Transactional
    public Request saveRequest(Request request) {
        return reqRepo.save(request);
    }


    @Override
    public Page<Request> getRequests(Long lastId, int count) {
        Pageable condition = makePageCondition(count, true);
        Page<Request> ret;
        if (lastId == null) {
            ret = reqRepo.findAll(condition);
        } else {
            ret = reqRepo.findAllByIdLessThan(lastId, condition);
        }
        return ret;
    }

    @Override
    public Long getIdForNewestRequestWithOffset(Long currRequestId, int offset) {
        Pageable condition = makePageCondition(offset, false);
        Page<Request> data = reqRepo.findAllByIdGreaterThan(currRequestId, condition);
        if (data.getTotalElements() < offset) {
            return null;
        } else {
            return data.getContent().get(offset - 1).getId();
        }
    }

    @Override
    public Request getRequestById(Long id) {
        if (id == null) {
            return null;
        }
        return reqRepo.findById(id).orElse(null);
    }

    @Override
    public boolean containRequestWithMessageId(Request request) {
        long cou = request.getId() == null
                ? reqRepo.countByMessageId(request.getMessageId())
                : reqRepo.countByMessageIdAndIdNot(request.getMessageId(), request.getId());
        return cou != 0;
    }

    @Override
    @Transactional
    public Response saveResponse(Response response) {
        Request request = response.getRequest();
        if (request.getId() != null) {
            request = reqRepo.getReferenceById(request.getId());
            response.setRequest(request);
        }
        return respRepo.save(response);
    }

    @Override
    public Page<Response> getResponses(Long lastId, int count) {
        Pageable condition = makePageCondition(count, true);
        if (lastId == null) {
            return respRepo.findAll(condition);
        } else {
            return respRepo.findAllByIdLessThan(lastId, condition);
        }
    }

    @Override
    public List<Response> getResponsesForRequests(List<Request> requests) {
        var minRequest = requests.stream().min(Request::compareTo).orElse(null);
        var maxRequest = requests.stream().max(Request::compareTo).orElse(null);
        if (minRequest == null || maxRequest == null) {
            return new ArrayList<>();
        }
        return respRepo.findAllByRequestBetween(minRequest, maxRequest);
    }

    @Override
    public Response getResponse(Long id) {
        if (id == null) {
            return null;
        }
        return respRepo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public OpenCloseRequest saveOpenCloseRequest(OpenCloseRequest request) {
        Request rawRequest = request.getRawRequest();
        if (rawRequest.getId() != null) {
            rawRequest = reqRepo.getReferenceById(rawRequest.getId());
            request.setRawRequest(rawRequest);
        }
        return ocReqRepo.save(request);
    }

    @Override
    public Page<OpenCloseRequest> getOpenCloseRequests(Long lastId, int count) {
        Page<OpenCloseRequest> ret = null;
        Pageable condition = makePageCondition(count, true);
        if (lastId == null) {
            ret = ocReqRepo.findAll(condition);
        } else {
            ret = ocReqRepo.findAllByIdLessThan(lastId, condition);
        }
        if (ret.getContent().size() > 0) {
            var highId = ret.getContent().get(0).getRawRequest().getId();
            var lowerId = ret.getContent().get(ret.getContent().size() - 1).getId();
            var requests = reqRepo.findAllByIdBetween(lowerId, highId)
                    .stream().collect(Collectors.toMap(k -> k.getId(), v -> v));
            ret.getContent().forEach(r -> r.setRawRequest(requests.get(r.getRawRequest().getId())));
            System.out.println();
        }
        return ret;
    }

    @Override
    public Long getIdForNewestOpenCloseRequestWithOffset(Long currRequestId, int offset) {
        Pageable condition = makePageCondition(offset, false);
        Page<OpenCloseRequest> data = ocReqRepo.findAllByIdGreaterThan(currRequestId, condition);
        if (data.getTotalElements() < offset) {
            return null;
        } else {
            return data.getContent().get(offset - 1).getId();
        }
    }

    @Override
    @Transactional
    public OpenCloseRequest getOpenCloseRequestById(Long id, boolean loadAccounts) {
        if (id == null) {
            return null;
        }
        var ret = ocReqRepo.findById(id).orElse(null);
        var msgId = ret.getRawRequest().getMessageId();
        if (loadAccounts) {
            var cou = ret.getAccounts().size();
        }
        return ret;
    }

    @Override
    @Transactional
    public OpenCloseRequest getOpenCloseRequestById(Long id) {
        return getOpenCloseRequestById(id, false);
    }

    @Override
    public boolean containOpenCloseRequestWithReference(OpenCloseRequest request) {
        long cou = request.getId() == null
                ? ocReqRepo.countByReference(request.getReference())
                : ocReqRepo.countByReferenceAndIdNot(request.getReference(), request.getId());
        return cou != 0;
    }

    @Override
    @Transactional
    public OpenCloseResponse saveOpenCloseResponse(OpenCloseResponse response) {
        OpenCloseRequest request = response.getRequest();
        if (request.getId() != null) {
            request = ocReqRepo.getReferenceById(request.getId());
            response.setRequest(request);
        }
        return ocRespRepo.save(response);
    }

    @Override
    @Transactional
    public OpenCloseResponse getOpenCloseResponseById(Long id, boolean loadAccounts) {
        if (id == null) {
            return null;
        }
        var ret = ocRespRepo.findById(id).orElse(null);
        if (ret != null && loadAccounts) {
            var cou = ret.getAccounts().size();
        }
        return ret;
    }

    @Override
    @Transactional
    public OpenCloseResponse getOpenCloseResponseById(Long id) {
        return getOpenCloseResponseById(id, false);
    }

    @Override
    public Page<OpenCloseResponse> getOpenCloseResponses(Long lastId, int count) {
        Pageable condition = makePageCondition(count, true);
        if (lastId == null) {
            return ocRespRepo.findAll(condition);
        } else {
            return ocRespRepo.findAllByIdLessThan(lastId, condition);
        }

    }

    @Override
    public List<OpenCloseResponse> getOpenCloseResponsesForRequests(List<OpenCloseRequest> requests) {
        var minRequest = requests.stream().min(OpenCloseRequest::compareTo).orElse(null);
        var maxRequest = requests.stream().max(OpenCloseRequest::compareTo).orElse(null);
        if (minRequest == null) {
            return new ArrayList<>();
        }
        return ocRespRepo.findAllByRequestBetween(minRequest, maxRequest);
    }

    @Override
    public OpenCloseResponseAccount lastResponseForAccountByOperTypeAndResult(String accountNum, Integer operType, String resultCode) {
        var res = ocRespRepo.findLastAccountsByCondition(accountNum, operType, resultCode, makePageCondition(1));
        if (res.getContent().size() == 0) {
            return null;
        }
        return res.getContent().get(0);
    }

    @Override
    public List<StoredSetting> getSettingsByGroup(String groupName) {
        return setRepo.findAllByGroupName(groupName);
    }

    @Override
    @Transactional
    public StoredSetting saveSetting(StoredSetting setting) {
        return setRepo.saveAndFlush(setting);
    }

    @Override
    public StoredSetting getSetting(StoredSettingKey key) {
        return setRepo.findById(key).orElse(null);
    }

    private Pageable makePageCondition(int count, boolean descending) {
        if (descending) {
            return PageRequest.of(0, count, Sort.by("id").descending());
        } else {
            return PageRequest.of(0, count, Sort.by("id"));
        }
    }

    private Pageable makePageCondition(int count) {
        return PageRequest.of(0, count);
    }

}
