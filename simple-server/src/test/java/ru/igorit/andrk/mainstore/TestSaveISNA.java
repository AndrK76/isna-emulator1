package ru.igorit.andrk.mainstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import ru.igorit.andrk.model.*;
import ru.igorit.andrk.repository.main.OpenCloseRequestRepository;
import ru.igorit.andrk.repository.main.OpenCloseResponseRepository;
import ru.igorit.andrk.repository.main.RequestRepository;
import ru.igorit.andrk.repository.main.ResponseRepository;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.store.MainStoreServiceJPAImpl;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.UUID;

@DataJpaTest
@DirtiesContext
@TestPropertySource(locations = "/testDataJPA.properties")
public class TestSaveISNA {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private RequestRepository mainReqRepo;
    @Autowired
    private ResponseRepository mainRespRepo;
    @Autowired
    private OpenCloseRequestRepository ocReqRepo;
    @Autowired
    private OpenCloseResponseRepository ocRespRepo;

    private MainStoreService svc;

    @BeforeEach
    private void initService() {

        svc = MainStoreServiceJPAImpl.builder()
                .reqRepo(mainReqRepo)
                .respRepo(mainRespRepo)
                .ocReqRepo(ocReqRepo)
                .ocRespRepo(ocRespRepo)
                .build();
    }

    @Test
    @DisplayName("Saving process for valid request for OPEN_CLOSE Service")
    public void testSaveCorrectData() {
        var request = CommonCreators.makeMainRequest();
        request = svc.saveRequest(request);
        entityManager.detach(request);

        var ocRequest = makeOCRequest(request, 3);
        ocRequest = svc.saveOpenCloseRequest(ocRequest);
        entityManager.detach(ocRequest);

        var ocResponse = makeOCResponse(ocRequest);
        ocResponse = svc.saveOpenCloseResponse(ocResponse);
        entityManager.detach(ocResponse);

        var response = CommonCreators.makeMainResponse(request, true);
        response = svc.saveResponse(response);
        entityManager.detach(response);
        entityManager.flush();
    }

    @Test
    @DisplayName("Saving process for incorrect request OPEN_CLOSE Service")
    public void testSaveInvalidData() {
        var request =CommonCreators.makeMainRequest();
        request = svc.saveRequest(request);
        entityManager.detach(request);

        var response = CommonCreators.makeMainResponse(request, false);
        response = svc.saveResponse(response);
        entityManager.detach(response);
        entityManager.flush();
    }

    private OpenCloseRequest makeOCRequest(Request request, int accountCounts) {
        OpenCloseRequest ocRequest = new OpenCloseRequest(request);
        ocRequest.setCodeForm("TEST");
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

    private OpenCloseResponse makeOCResponse(OpenCloseRequest request) {
        OpenCloseResponse resp = new OpenCloseResponse(request);
        request.getAccounts().forEach(r ->
        {
            var acc = new OpenCloseResponseAccount();
            acc.setResponse(resp);
            resp.getAccounts().add(acc);
        });
        return resp;
    }

}
