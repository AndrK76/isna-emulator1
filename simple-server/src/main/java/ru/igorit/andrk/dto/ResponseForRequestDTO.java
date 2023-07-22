package ru.igorit.andrk.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import ru.igorit.andrk.model.Response;

import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Jacksonized
@Getter
@EqualsAndHashCode(of={"id","messageId","correlationId"})
public class ResponseForRequestDTO {
    private Long id;
    private UUID messageId;
    private UUID correlationId;
    private String serviceId;
    private Boolean isSuccess;
    private OffsetDateTime responseDate;
    private String statusCode;
    private String statusMessage;
    private String data;

    public static ResponseForRequestDTO create(Response response, boolean addData) {
        return new ResponseForRequestDTO(
                response.getId(),
                response.getMessageId(),
                response.getCorrelationId(),
                response.getServiceId(),
                response.getIsSuccess(),
                response.getResponseDate().withNano(0),
                response.getStatusCode(),
                response.getStatusMessage(),
                addData ? response.getData() : null);
    }

    public static ResponseForRequestDTO create(Response response) {
        return create(response, false);
    }


}
