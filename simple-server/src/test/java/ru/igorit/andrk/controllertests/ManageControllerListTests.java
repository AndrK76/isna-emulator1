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
import ru.igorit.andrk.dto.RequestDto;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.Response;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.utils.RestPage;

import java.time.OffsetDateTime;
import java.util.UUID;
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
public class ManageControllerListTests {

    private static final String PATH = "/api/" + API_VERSION;

    @MockBean
    private MainStoreService mainStoreService;

    @Autowired
    private MockMvc client;

    @Autowired
    private ObjectMapper objectMapper;

    private final FullStorageInitiator storeInitiator =new FullStorageInitiator();


    @ParameterizedTest
    @ValueSource(ints = {0, COUNT_REQUESTS})
    void checkThatGetAllRequestQuery_ReturnCorrectData(int reqCount) throws Exception {
        if (reqCount != 0) {
            storeInitiator.initFullMockStorage(false);
        }
        storeInitiator.initFullMockStoreService(mainStoreService,false);
        var requestRes = client.perform(get(PATH + "/request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(reqCount))
                .andDo(print())
                .andReturn();

        var responseString = requestRes.getResponse().getContentAsString();

        var typeDef = TypeFactory.defaultInstance().constructParametricType(RestPage.class, RequestDto.class);
        var responseContent = (RestPage<RequestDto>) objectMapper.readValue(responseString, typeDef);
        var expectedRequests = storeInitiator.getRequests().stream().map(r -> RequestDto.create(r, true)).collect(Collectors.toList());
        RequestDto.setResponseData(expectedRequests, storeInitiator.getResponses());
        assertThat(responseContent.getContent())
                .hasSize(reqCount)
                .containsExactlyElementsOf(expectedRequests);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, COUNT_REQUESTS - 5, COUNT_REQUESTS + 10})
    void checkThatGetOneRequestQuery_ReturnCorrectData(Long id) throws Exception {
        storeInitiator.initFullMockStorage(false);
        storeInitiator.initFullMockStoreService(mainStoreService,false);
        var requestRes = client.perform(get(PATH + "/request/{id}", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();
        if (id < COUNT_REQUESTS) {
            var actualId = (Integer) JsonPath.read(responseString, "$.id");
            assertThat(actualId.longValue()).isEqualTo(id);
        } else {
            assertThat(responseString).isNullOrEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("getNewestRequestParameters")
    void checkThatGetNewestRequest_CallStorageWhenNeeded_andReturnResultFromStorage
            (Long id, Integer offset, Long answer, int statusCode, boolean callStorage) throws Exception {
        AtomicBoolean actualCallStorage = new AtomicBoolean(false);
        if (offset != null) {
            when(mainStoreService.getIdForNewestRequestWithOffset(id, offset))
                    .thenAnswer((Answer<Long>) invocation -> {
                        actualCallStorage.set(true);
                        return answer;
                    });
        }
        var requestRes = client.perform(get(PATH + "/request/{id}/getnewest", id)
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
    @MethodSource("getRequestDataAndResponseData_Parameters")
    void checkThatGetRequestData_CallStorageWhenCalled_andReturnResultFromStorage
            (Long id, String answer, int statusCode, boolean callStorage) throws Exception {
        AtomicBoolean actualCallStorage = new AtomicBoolean(false);
        when(mainStoreService.getRequestById(id))
                .thenAnswer((Answer<Request>) invocation -> {
                    actualCallStorage.set(true);
                    return Request.builder()
                            .messageDate(OffsetDateTime.now())
                            .data(answer)
                            .build();
                });
        var requestRes = client.perform(get(PATH + "/request/{id}/data", id))
                .andDo(print())
                .andReturn();

        assertThat(requestRes.getResponse().getStatus()).isEqualTo(statusCode);

        var responseString = requestRes.getResponse().getContentAsString();
        if (answer==null || answer.isEmpty()) {
            assertThat(responseString).isNullOrEmpty();
        } else {
            assertThat(responseString).isEqualTo(answer);
        }

        assertThat(actualCallStorage.get()).isEqualTo(callStorage);
    }

    private static Stream<Arguments> getRequestDataAndResponseData_Parameters() {
        return Stream.of(
                Arguments.of(2L, "Текст\nтекст", 200, true)
                , Arguments.of(null, null, 404, false)
                , Arguments.of(55L, "", 200, true)

        );
    }

    @ParameterizedTest
    @MethodSource("getRequestDataAndResponseData_Parameters")
    void checkThatGetResponseData_CallStorageWhenCalled_andReturnResultFromStorage
            (Long id, String answer, int statusCode, boolean callStorage) throws Exception {
        AtomicBoolean actualCallStorage = new AtomicBoolean(false);
        when(mainStoreService.getResponse(id))
                .thenAnswer((Answer<Response>) invocation -> {
                    actualCallStorage.set(true);
                    return Response.builder()
                            .request(
                                    Request.builder().messageDate(OffsetDateTime.now()).build()
                            )
                            .messageId(UUID.randomUUID())
                            .responseDate(OffsetDateTime.now())
                            .data(answer)
                            .isSuccess(true)
                            .build();
                });
        var requestRes = client.perform(get(PATH + "/response/{id}/data", id))
                .andDo(print())
                .andReturn();

        assertThat(requestRes.getResponse().getStatus()).isEqualTo(statusCode);

        var responseString = requestRes.getResponse().getContentAsString();
        if (answer==null || answer.isEmpty()) {
            assertThat(responseString).isNullOrEmpty();
        } else {
            assertThat(responseString).isEqualTo(answer);
        }

        assertThat(actualCallStorage.get()).isEqualTo(callStorage);
    }

}
