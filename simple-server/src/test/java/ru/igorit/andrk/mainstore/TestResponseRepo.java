package ru.igorit.andrk.mainstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.Response;
import ru.igorit.andrk.repository.main.RequestRepository;
import ru.igorit.andrk.repository.main.ResponseRepository;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.store.MainStoreServiceJPAImpl;

import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DataJpaTest
@TestPropertySource(locations = "/testDataJPA.properties")
public class TestResponseRepo {


    @Autowired
    private RequestRepository reqRepo;

    @Autowired
    private ResponseRepository respRepo;
    private MainStoreService svc;

    @BeforeEach
    private void initService() {
        svc = MainStoreServiceJPAImpl.builder()
                .reqRepo(reqRepo)
                .respRepo(respRepo)
                .build();
    }

    @Test
    @DisplayName("Save Response in storage")
    public void testSaveResponse() {
        var couInRepo = respRepo.findAll().size();
        assertThat(couInRepo).withFailMessage("Response table must be Empty before test")
                .isEqualTo(0);
        var response = CommonCreators.makeMainResponse(CommonCreators.makeMainRequest());
        var oldMessageId = UUID.fromString(response.getMessageId().toString());
        response = svc.saveResponse(response);
        assertThat(response.getRequest().getId()).withFailMessage("Request Must be saved when saving Response")
                .isNotNull();
        assertThat(response.getMessageId()).withFailMessage("Message Id after save must be not change")
                .isEqualByComparingTo(oldMessageId);
        couInRepo = respRepo.findAll().size();
        assertThat(couInRepo).withFailMessage("After insert one Request Repo Must contain one record")
                .isEqualTo(1);
        response = CommonCreators.makeMainResponse(CommonCreators.makeMainRequest());
        svc.saveRequest(response.getRequest());
        couInRepo = reqRepo.findAll().size();
        var reqId = response.getRequest().getId();
        svc.saveResponse(response);
        assertThat(reqRepo.findAll().size())
                .withFailMessage("Save response with persisted request must don't create new request ")
                .isEqualTo(couInRepo);
        assertThat(response.getRequest().getId().longValue())
                .withFailMessage("Save response with persisted request must don't change request ")
                .isEqualTo(reqId);
    }


    @Test
    @DisplayName("Find Responses by Request range")
    public void testFindResponsesByRequestRange() {
        int totalSize = 95;
        int minBoundStep = 10, maxBoundStep = 80;
        long minBound = 0L, maxBound = 0L;
        for (int i = 1; i <= totalSize; i++) {
            long curReqId = 0;
            if (i % 2 == 0) {
                var res = svc.saveResponse(CommonCreators.makeMainResponse(CommonCreators.makeMainRequest()));
                curReqId = res.getRequest().getId();
            } else {
                var res = svc.saveRequest(CommonCreators.makeMainRequest());
                curReqId = res.getId();
            }
            if (i == minBoundStep) {
                minBound = curReqId;
            }
            if (i == maxBoundStep) {
                maxBound = curReqId;
            }
        }
        var reqMin = reqRepo.findById(minBound).get();
        var reqMax = reqRepo.findById(maxBound).get();
        var ret = respRepo.findAllByRequestBetween(reqMin, reqMax)
                .stream().map(Response::getRequest).collect(Collectors.toList());
        var min = ret.stream().min(Request::compareTo).get().getId();
        var max = ret.stream().max(Request::compareTo).get().getId();
        assertThatNoException().isThrownBy(() -> {
            if (min != reqMin.getId().longValue() || max != reqMax.getId().longValue()) {
                throw new RuntimeException("Incorrect range bounds");
            }
        });

    }

}
