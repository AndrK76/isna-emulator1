package ru.igorit.andrk.opecloseprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import ru.igorit.andrk.model.*;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.processor.ProcessResult;
import ru.igorit.andrk.service.processor.openclose.OpenCloseProcessor;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.igorit.andrk.config.services.Constants.OPEN_CLOSE_SERVICE;

public class OpenCloseProcessorTests {
    private static final String SAMPLE_CFG = "sample_openclose.cfg";


    private MainStoreService mainStoreService;
    private final List<OpenCloseResponse> responses = new ArrayList<>();
    private final List<OpenCloseRequest> requests = new ArrayList<>();

    private final Map<String, Object> dynSettings = Stream.of(
            new AbstractMap.SimpleEntry<>("CheckUniqueMessageId", false),
            new AbstractMap.SimpleEntry<>("CheckUniqueReference", false),
            new AbstractMap.SimpleEntry<>("ValidateAccountState", false),
            new AbstractMap.SimpleEntry<>("ValidateOperationDate", false),
            new AbstractMap.SimpleEntry<>("RaiseTestError", false)
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    private byte[] getConfig() throws IOException {
        return Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(SAMPLE_CFG))
                .readAllBytes();
    }

    @BeforeEach
    void initMockStoreService() {
        mainStoreService = mock(MainStoreService.class);
    }


    @Test
    void configureProcessor_dontThrowError() throws IOException {
        var processor = new OpenCloseProcessor(mainStoreService);
        var cfgData = getConfig();
        assertThatNoException().isThrownBy(() -> processor.configure(cfgData));
    }

    @Test
    void checkThat_OpenCloseResults_parsedCorrect() throws IOException {
        var processor = new OpenCloseProcessor(mainStoreService);
        var resValues = processor.initResultValues(getConfig());
        String[] expectedResults = {"SUCCESS", "TEST_ERROR"};
        assertThat(resValues.keySet()).contains(expectedResults);
        System.out.println();
    }

    @Test
    void processValidData_mustReturnSuccessAndCorrectResult() throws IOException {
        responses.clear();
        var processor = getProcessor();
        var accResultMap = processor.initResultValues(getConfig());
        var request = sampleSuccessRequest();
        configureMockStoreService();
        var res = processor.process(request, request.getMessageId());
        var invocations = Mockito.mockingDetails(mainStoreService).getInvocations();

        String[] expectedMethodNames = {"getSettingsByGroup", "saveOpenCloseRequest", "saveOpenCloseResponse"};
        var actualMethodNames = List.copyOf(invocations).stream().map(r -> r.getMethod().getName()).distinct()
                .collect(Collectors.toList());
        assertThat(actualMethodNames).containsExactly(expectedMethodNames);
        assertThat(requests).hasSize(1);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getAccounts().size()).isEqualTo(requests.get(0).getAccounts().size());
        var accResults = responses.get(0).getAccounts();
        assertThat(accResults).allMatch(r -> r.getResultCode().equals(accResultMap.get("SUCCESS").getCode()));
        assertThat(res).isEqualTo(ProcessResult.successResult());
    }

    @Test
    void dynamicSettings_CorrectlyApplied() throws IOException {
        responses.clear();
        var processor = getProcessor();
        var accResultMap = processor.initResultValues(getConfig());
        var request = sampleSuccessRequest();
        dynSettings.put("RaiseTestError", true);
        configureMockStoreService();
        var res = processor.process(request, request.getMessageId());
        assertThat(responses).hasSize(1);
        var accResults = responses.get(0).getAccounts();
        assertThat(accResults).allMatch(r -> r.getResultCode().equals(accResultMap.get("TEST_ERROR").getCode()));
        assertThat(res).isEqualTo(ProcessResult.successResult());
    }

    @Test
    void processRequestWithInvalidContent_DontThrowErrorAndReturnErrorResult() throws IOException {
        responses.clear();
        var processor = getProcessor();
        var request = sampleInvalidRequest();
        configureMockStoreService();
        assertThatNoException().isThrownBy(() -> {
            var res = processor.process(request, request.getMessageId());
            assertThat(res).isEqualTo(ProcessResult.errorResult());
        });
    }


    private OpenCloseProcessor getProcessor() throws IOException {
        var processor = new OpenCloseProcessor(mainStoreService);
        processor.configure(getConfig());
        return processor;
    }

    private void configureMockStoreService() {
        when(mainStoreService.saveOpenCloseRequest(any()))
                .thenAnswer((Answer<OpenCloseRequest>) invocation -> {
                    var request = (OpenCloseRequest) invocation.getArguments()[0];
                    requests.add(request);
                    return request;
                });
        when(mainStoreService.saveOpenCloseResponse(any()))
                .thenAnswer((Answer<OpenCloseResponse>) invocation -> {
                    var response = (OpenCloseResponse) invocation.getArguments()[0];
                    responses.add(response);
                    return response;
                });
        when(mainStoreService.getSettingsByGroup(OPEN_CLOSE_SERVICE))
                .thenAnswer((Answer<List<StoredSetting>>) invocation -> {
                    List<StoredSetting> ret = new ArrayList<>();
                    for (var setKey : dynSettings.keySet()) {
                        var value = dynSettings.get(setKey);
                        var setting = StoredSetting.builder()
                                .id(new StoredSettingKey(OPEN_CLOSE_SERVICE, setKey))
                                .valueType(value.getClass())
                                .value(value.toString())
                                .build();
                        ret.add(setting);
                    }
                    return ret;
                });
    }


    private Request sampleSuccessRequest() {
        return Request.builder()
                .id(1L)
                .messageId(UUID.randomUUID())
                .serviceId("ISNA_BVU_BA_OPEN_CLOSE")
                .messageDate(OffsetDateTime.now().withNano(0))
                .data("{4:\n" +
                        ":20:V306197160121274\n" +
                        ":12:400\n" +
                        ":77E:FORMS/A03/202306191953/Увед. об изменении банковских счетов\n" +
                        "/ACCOUNT/VTBAKZKZ/KZ484324302398A00006/05/20230301/450509833484/VTBAKZKZ/398A00006/20230301\n" +
                        "-}")
                .build();
    }

    private Request sampleInvalidRequest() {
        return Request.builder()
                .id(1L)
                .messageId(UUID.randomUUID())
                .serviceId("ISNA_BVU_BA_OPEN_CLOSE")
                .messageDate(OffsetDateTime.now().withNano(0))
                .data("{4:\n" +
                        ":20:V306197160121274\n" +
                        ":12:400\n" +
                        "-}")
                .build();
    }


}
