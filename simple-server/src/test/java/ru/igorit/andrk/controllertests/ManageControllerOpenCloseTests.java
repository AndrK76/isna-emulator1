package ru.igorit.andrk.controllertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.igorit.andrk.api.ManageController;
import ru.igorit.andrk.dto.OpenCloseRequestDTO;
import ru.igorit.andrk.dto.OpenCloseResponseForRequestDTO;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.utils.RestPage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.igorit.andrk.config.services.Constants.API_VERSION;
import static ru.igorit.andrk.controllertests.FullStorageInitiator.COUNT_REQUESTS;

@WebMvcTest(ManageController.class)
public class ManageControllerOpenCloseTests {

    private static final String PATH = "/api/" + API_VERSION;

    @MockBean
    private MainStoreService mainStoreService;

    @Autowired
    private MockMvc client;

    @Autowired
    private ObjectMapper objectMapper;

    private final FullStorageInitiator storeInitiator = new FullStorageInitiator();


    @ParameterizedTest
    @ValueSource(ints = {0, COUNT_REQUESTS})
    void checkThatGetAllOpenCloseRequestQuery_ReturnCorrectData(int reqCount) throws Exception {
        if (reqCount != 0) {
            storeInitiator.initFullMockStorage(true);
        }
        int ocReqCount = storeInitiator.getOcRequests().size();
        storeInitiator.initFullMockStoreService(mainStoreService,true);
        var requestRes = client.perform(get(PATH + "/opencloserequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(ocReqCount))
                .andDo(print())
                .andReturn();

        var responseString = requestRes.getResponse().getContentAsString();

        var typeDef = TypeFactory.defaultInstance().constructParametricType(RestPage.class, OpenCloseRequestDTO.class);
        var responseContent = (RestPage<OpenCloseRequestDTO>) objectMapper.readValue(responseString, typeDef);
        var expectedRequests = storeInitiator.getOcRequests().stream().map(r -> OpenCloseRequestDTO.create(r, true)).collect(Collectors.toList());
        OpenCloseRequestDTO.setResponseData(expectedRequests, storeInitiator.getOcResponses());
        assertThat(responseContent.getContent())
                .hasSize(ocReqCount)
                .containsExactlyElementsOf(expectedRequests)
                .allMatch(r -> r.getAccounts() == null || r.getAccounts().size() == 0);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, COUNT_REQUESTS - 5, COUNT_REQUESTS + 10})
    void checkThatGetOneOpenCloseRequestQuery_ReturnCorrectData(Long id) throws Exception {
        storeInitiator.initFullMockStorage(true);
        storeInitiator.initFullMockStoreService(mainStoreService,true);
        var requestRes = client.perform(get(PATH + "/opencloserequest/{id}/account", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();

        if (id < COUNT_REQUESTS && storeInitiator.getOcRequests().stream().filter(r -> r.getId().equals(id)).findFirst().isPresent()) {
            var actualId = (Integer) JsonPath.read(responseString, "$.id");
            assertThat(actualId.longValue()).isEqualTo(id);
            var responseContent = (OpenCloseRequestDTO) objectMapper.readValue(responseString, OpenCloseRequestDTO.class);
            assertThat(responseContent.getAccounts()).isNotNull().hasSize(1);
        } else {
            assertThat(responseString).isNullOrEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("getNewestRequestParameters")
    void checkThatGetNewestOpenCloseRequest_CallStorageWhenNeeded_andReturnResultFromStorage
            (Long id, Integer offset, Long answer, int statusCode, boolean callStorage) throws Exception {
        AtomicBoolean actualCallStorage = new AtomicBoolean(false);
        if (offset != null) {
            when(mainStoreService.getIdForNewestOpenCloseRequestWithOffset(id, offset))
                    .thenAnswer((Answer<Long>) invocation -> {
                        actualCallStorage.set(true);
                        return answer;
                    });
        }
        var requestRes = client.perform(get(PATH + "/opencloserequest/{id}/getnewest", id)
                        .param("offset", "" + offset))
                .andDo(print())
                .andReturn();

        assertThat(requestRes.getResponse().getStatus()).isEqualTo(statusCode);

        var responseString = requestRes.getResponse().getContentAsString();
        if (answer == null) {
            assertThat(responseString).isNullOrEmpty();
        } else {
            AtomicReference<Long> actualId = new AtomicReference<>();
            assertThatNoException().isThrownBy(() -> actualId.set(Long.parseLong(responseString)));
            assertThat(actualId.get()).isEqualTo(answer);
        }

        assertThat(actualCallStorage.get()).isEqualTo(callStorage);
    }
    private static Stream<Arguments> getNewestRequestParameters() {
        return Stream.of(
                Arguments.of(2L, 1, 5L, 200, true)
                , Arguments.of(null, 8, null, 404, false)
                , Arguments.of(10L, null, null, 400, false)
                , Arguments.of(null, null, null, 404, false)
                , Arguments.of(1L, 5, 88L, 200, true)
                , Arguments.of(0L, 5, 18L, 200, true)
                , Arguments.of(55L, 100, 77L, 200, true)

        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, COUNT_REQUESTS - 5, COUNT_REQUESTS + 10})
    void checkThatGetOneOpenCloseResponseQuery_ReturnCorrectData(Long id) throws Exception {
        storeInitiator.initFullMockStorage(true);
        storeInitiator.initFullMockStoreService(mainStoreService,true);
        var requestRes = client.perform(get(PATH + "/opencloseresponse/{id}/account", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();

        if (id < COUNT_REQUESTS && storeInitiator.getOcResponses().stream().filter(r -> r.getId().equals(id)).findFirst().isPresent()) {
            var actualId = (Integer) JsonPath.read(responseString, "$.id");
            assertThat(actualId.longValue()).isEqualTo(id);
            var responseContent = (OpenCloseResponseForRequestDTO) objectMapper.readValue(responseString, OpenCloseResponseForRequestDTO.class);
            assertThat(responseContent.getAccounts()).isNotNull().hasSize(1);
        } else {
            assertThat(responseString).isNullOrEmpty();
        }
    }



}
