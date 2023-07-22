package ru.igorit.andrk.mainstore;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import ru.igorit.andrk.model.*;
import ru.igorit.andrk.repository.main.OpenCloseRequestRepository;
import ru.igorit.andrk.repository.main.OpenCloseResponseRepository;
import ru.igorit.andrk.repository.main.RequestRepository;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.store.MainStoreServiceJPAImpl;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DataJpaTest
@DirtiesContext
@TestPropertySource(locations = "/testDataJPA.properties")
public class TestOpenCloseResponseRepo {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private RequestRepository mainReqRepo;

    @Autowired
    private OpenCloseRequestRepository ocReqRepo;

    @Autowired
    private OpenCloseResponseRepository ocRespRepo;

    private MainStoreService svc;

    @BeforeEach
    private void initService() {
        svc = MainStoreServiceJPAImpl.builder()
                .reqRepo(mainReqRepo)
                .ocReqRepo(ocReqRepo)
                .ocRespRepo(ocRespRepo)
                .build();
    }

    @Test
    @DisplayName("Save open/close response in storage")
    public void testSaveResponse() {
        int accountCounts = 3;
        var request = CommonCreators.makeMainRequest();
        request = svc.saveRequest(request);
        var ocReq = CommonCreators.makeOCRequest(request, accountCounts);
        ocReq = svc.saveOpenCloseRequest(ocReq);
        Long reqId = ocReq.getId();
        var ocResp = makeOCResponse(ocReq);
        ocResp = svc.saveOpenCloseResponse(ocResp);

        assertThat(ocResp.getRequest().getId())
                .withFailMessage("Id request in stored response %d must me equal %d", ocResp.getRequest().getId(), reqId)
                .isEqualTo(reqId);
        assertThat(ocResp.getId())
                .withFailMessage("Id stored response must be not null")
                .isNotNull();
        assertThat(ocResp.getAccounts().size())
                .withFailMessage("Stored request contain %d account, but mus be %d", ocResp.getAccounts().size(), accountCounts)
                .isEqualTo(accountCounts);
        Long ocRespId = ocResp.getId();
        assertThat(ocResp.getAccounts())
                .withFailMessage("Each account in stored request must be have id")
                .allMatch(r -> r.getId() != null)
                .withFailMessage("Request id in each account must be equal %d", ocRespId)
                .allMatch(r -> r.getResponse().getId().equals(ocRespId));
        assertThat(ocRespRepo.findAll().size()).isEqualTo(1);

    }

    @Test
    @DisplayName("Load open/close response from storage")
    public void testLoadResponse() {
        int accountCounts = 3;
        var request = CommonCreators.makeMainRequest();
        request = svc.saveRequest(request);
        var ocReq = CommonCreators.makeOCRequest(request, accountCounts);
        ocReq = svc.saveOpenCloseRequest(ocReq);
        var ocResp = makeOCResponse(ocReq);
        var ocRespId = svc.saveOpenCloseResponse(ocResp).getId();

        entityManager.flush();
        entityManager.clear();


        assertThatThrownBy(() -> {
            var ocResp1 = svc.getOpenCloseResponseById(ocRespId);
            entityManager.detach(ocResp1);
            var size = ocResp1.getAccounts().size();
        }).isInstanceOf(LazyInitializationException.class);

        assertThatNoException().isThrownBy(() -> {
            var ocResp1 = svc.getOpenCloseResponseById(ocRespId);
            var size = ocResp1.getAccounts().size();
            entityManager.detach(ocResp1);
            if (size != 3) {
                throw new RuntimeException("Incorrect account Size after Lazy load");
            }
        });

        assertThatNoException().isThrownBy(() -> {
            var ocResp1 = svc.getOpenCloseResponseById(ocRespId, true);
            entityManager.detach(ocResp1);
            var size = ocResp1.getAccounts().size();
            if (size != 3) {
                throw new RuntimeException("Incorrect account Size after Lazy load");
            }
        });

    }


    @Test
    @DisplayName("find Last account response by conditionals")
    public void testFindLastAccountResponseByConditionals() {
        String[] accountNums = new String[]{"ACC001", "ACC002", "ACC003", "ACC004"};
        String[] operResults = new String[]{"01", "99"};
        LocalDateTime[] dates = new LocalDateTime[]{
                LocalDateTime.of(2023, 01, 01, 01, 00),
                LocalDateTime.of(2023, 01, 01, 02, 00),
                LocalDateTime.of(2023, 01, 01, 03, 00),
        };
        OpenCloseResponseAccount expectedAccount = null;

        for (int i = 0; i < dates.length; i++) {
            for (int operType = 1; operType < 3; operType++) {
                for (int operRes = 0; operRes < operResults.length; operRes++) {
                    var request = svc.saveRequest(CommonCreators.makeMainRequest());
                    entityManager.detach(request);
                    var ocReq = svc.saveOpenCloseRequest(makeOCRequest(request, dates[i], operType, accountNums));
                    entityManager.detach(ocReq);
                    var ocResp = makeOCResponse(ocReq);
                    String res = operResults[operRes];
                    ocResp.getAccounts().forEach(r -> r.setResultCode(res));
                    svc.saveOpenCloseResponse(ocResp);
                    entityManager.detach(ocResp);
                    assertThat(ocResp.getAccounts())
                            .allMatch(r -> r.getId() != null)
                            .withFailMessage("Error prepare data, not save accounts");
                    if (i == dates.length - 1 && res.equals("01") && operType == 1) {
                        expectedAccount = ocResp.getAccounts().get(0);
                    }
                }
            }
        }
        assertThat(expectedAccount).withFailMessage("Error prepare data, empty expected result").isNotNull();

        var actualAccount = svc.lastResponseForAccountByOperTypeAndResult(expectedAccount.getAccount(), expectedAccount.getOperType(), expectedAccount.getResultCode());

        assertThat(actualAccount)
                .withFailMessage("No account finded, excepted accound with id=%d",expectedAccount.getId())
                        .isNotNull();


        assertThat(actualAccount.getId())
                .withFailMessage("Incorrect finded account id, actual=%d, expected=%d",actualAccount.getId(), expectedAccount.getId())
                .isEqualTo(expectedAccount.getId());

        var emptyAccount = svc.lastResponseForAccountByOperTypeAndResult("999", expectedAccount.getOperType(), expectedAccount.getResultCode());
        assertThat(emptyAccount)
                .withFailMessage("Finded account for noexist accountNum, excepted null")
                .isNull();
        emptyAccount = svc.lastResponseForAccountByOperTypeAndResult(null, expectedAccount.getOperType(), expectedAccount.getResultCode());
        assertThat(emptyAccount)
                .withFailMessage("Finded account for null accountNum, excepted null")
                .isNull();
        emptyAccount = svc.lastResponseForAccountByOperTypeAndResult(actualAccount.getAccount(), 5, expectedAccount.getResultCode());
        assertThat(emptyAccount)
                .withFailMessage("Finded account for noexist operType, excepted null")
                .isNull();
        emptyAccount = svc.lastResponseForAccountByOperTypeAndResult(actualAccount.getAccount(), null, expectedAccount.getResultCode());
        assertThat(emptyAccount)
                .withFailMessage("Finded account for null operType, excepted null")
                .isNull();
        emptyAccount = svc.lastResponseForAccountByOperTypeAndResult(actualAccount.getAccount(), expectedAccount.getOperType(), "33");
        assertThat(emptyAccount)
                .withFailMessage("Finded account for noexist resultCode, excepted null")
                .isNull();
        emptyAccount = svc.lastResponseForAccountByOperTypeAndResult(actualAccount.getAccount(), expectedAccount.getOperType(), null);
        assertThat(emptyAccount)
                .withFailMessage("Finded account for null resultCode, excepted null")
                .isNull();
    }



    private OpenCloseRequest makeOCRequest(Request request, LocalDateTime operDate, Integer operType, String[] accountNums) {
        OpenCloseRequest ocRequest = new OpenCloseRequest(request);
        ocRequest.setCodeForm("TEST");
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


    private OpenCloseResponse makeOCResponse(OpenCloseRequest request) {
        OpenCloseResponse resp = new OpenCloseResponse(request);
        request.getAccounts().forEach(r -> new OpenCloseResponseAccount(resp, r));
        return resp;
    }
}
