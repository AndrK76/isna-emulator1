package ru.igorit.andrk.soaptests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.webservices.server.WebServiceServerTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ws.NoEndpointFoundException;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;
import ru.igorit.andrk.config.services.Constants;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.Response;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.processor.ProcessorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.*;
import static ru.igorit.andrk.config.services.Constants.DEFAULT_NAMESPACE;
import static ru.igorit.andrk.config.services.Constants.OPEN_CLOSE_SERVICE;

@WebServiceServerTest
public class OpenCloseSoapTests {

    @Autowired
    private MockWebServiceClient client;

    @MockBean
    private MainStoreService mainStoreService;

    @MockBean
    private ProcessorFactory processorFactory;

    private final List<Request> requests = new ArrayList<>();
    private final List<Response> responses = new ArrayList<>();


    @BeforeEach
    void initMockStoreService() {
        requests.clear();
        responses.clear();
        when(mainStoreService.saveRequest(any()))
                .thenAnswer((Answer<Request>) invocation -> {
                    var request = (Request) invocation.getArguments()[0];
                    requests.add(request);
                    return request;
                });
        when(mainStoreService.saveResponse(any()))
                .thenAnswer((Answer<Response>) invocation -> {
                    var response = (Response) invocation.getArguments()[0];
                    responses.add(response);
                    return response;
                });
    }

    @BeforeEach
    void initProcessorFactory() {
        when(processorFactory.getProcessor(any()))
                .thenReturn(null);
        when(processorFactory.getProcessor("ISNA_BVU_BA_OPEN_CLOSE"))
                .thenReturn(new TestDataProcessor());
    }


    @Test
    void givenValidRequest_CallingProcessor_andReturnCorrectData_andSaveResultsToStore() {
        UUID messageId = UUID.randomUUID();
        String data = "Текст\nвторая строка текста";

        StringSource request = new StringSource(makeValidRequest(messageId, data));

        String messageIdXPath = "//*[local-name()='SendMessageResponse']/return/responseInfo/correlationId/text()";
        String dataXPath = "//*[local-name()='SendMessageResponse']/return/responseData/data/text()";

        client.sendRequest(withPayload(request))
                .andExpect((req, resp) -> resp.writeTo(System.out))
                .andExpect(noFault())
                .andExpect(xpath(messageIdXPath).evaluatesTo(messageId.toString()))
                .andExpect(xpath(dataXPath).evaluatesTo(data));

        assertThat(requests.size()).isEqualTo(1);
        assertThat(requests.get(0).getMessageId()).isEqualTo(messageId);
        assertThat(requests.get(0).getData()).isEqualTo(data);

        assertThat(responses.size()).isEqualTo(1);
        assertThat(responses.get(0).getCorrelationId()).isEqualTo(messageId);
        assertThat(responses.get(0).getData()).isEqualTo(data);
    }

    @Test
    void givenRequestForInvalidServiceName_ReturnFaultResponse_butSaveResultsToStore() {
        UUID messageId = UUID.randomUUID();
        String serviceName = "NO_EXISTS_SERVICE";

        StringSource request = new StringSource(makeRequestWithIncorrectService(messageId, serviceName));
        client.sendRequest(withPayload(request))
                .andExpect((req, resp) -> resp.writeTo(System.out))
                .andExpect(serverOrReceiverFault());
        assertThat(requests.size()).isEqualTo(1);
        assertThat(requests.get(0).getMessageId()).isEqualTo(messageId);

        assertThat(responses.size()).isEqualTo(1);
        assertThat(responses.get(0).getCorrelationId()).isEqualTo(messageId);
        assertThat(responses.get(0).getServiceId()).isEqualTo(serviceName);
    }

    @Test
    void givenRequestForInvalidNamespace_dontCallService() {
        UUID messageId = UUID.randomUUID();
        String namespace = "http://my.namespace";
        StringSource request = new StringSource(makeRequestWithIncorrectNamespace(messageId, namespace));

        assertThatThrownBy(() -> client.sendRequest(withPayload(request)))
                .hasMessageContaining("No endpoint can be found");
        assertThat(requests.size()).isEqualTo(0);
        assertThat(responses.size()).isEqualTo(0);

        System.out.println();

    }


    private String makeValidRequest(UUID messageId, String data) {
        return "<ns3:SendMessage xmlns:ns3=\"" + DEFAULT_NAMESPACE + "\">\n" +
                "      <request>\n" +
                "        <requestInfo>\n" +
                "          <messageId>" + messageId.toString() + "</messageId>\n" +
                "          <serviceId>" + OPEN_CLOSE_SERVICE + "</serviceId>\n" +
                "          <messageDate>2023-06-09T15:47:05+07:00</messageDate>\n" +
                "          <sender/>\n" +
                "        </requestInfo>\n" +
                "        <requestData>\n" +
                "          <data xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">" +
                data + "</data>\n" +
                "        </requestData>\n" +
                "      </request>\n" +
                "    </ns3:SendMessage>";
    }

    private String makeRequestWithIncorrectService(UUID messageId, String serviceName) {
        return "<ns3:SendMessage xmlns:ns3=\"" + DEFAULT_NAMESPACE + "\">\n" +
                "      <request>\n" +
                "        <requestInfo>\n" +
                "          <messageId>" + messageId.toString() + "</messageId>\n" +
                "          <serviceId>" + serviceName + "</serviceId>\n" +
                "          <messageDate>2023-06-09T15:47:05+07:00</messageDate>\n" +
                "          <sender/>\n" +
                "        </requestInfo>\n" +
                "        <requestData>\n" +
                "          <data xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\"></data>\n" +
                "        </requestData>\n" +
                "      </request>\n" +
                "    </ns3:SendMessage>";
    }

    private String makeRequestWithIncorrectNamespace(UUID messageId, String nameSpace) {
        return "<ns3:SendMessage xmlns:ns3=\"" + nameSpace + "\">\n" +
                "      <request>\n" +
                "        <requestInfo>\n" +
                "          <messageId>" + messageId.toString() + "</messageId>\n" +
                "          <serviceId>" + OPEN_CLOSE_SERVICE + "</serviceId>\n" +
                "          <messageDate>2023-06-09T15:47:05+07:00</messageDate>\n" +
                "          <sender/>\n" +
                "        </requestInfo>\n" +
                "        <requestData>\n" +
                "          <data xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\"></data>\n" +
                "        </requestData>\n" +
                "      </request>\n" +
                "    </ns3:SendMessage>";
    }

}
