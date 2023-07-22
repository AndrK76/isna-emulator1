package ru.igorit.andrk.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.Response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@Jacksonized
@AllArgsConstructor
@EqualsAndHashCode(of={"id","messageId","response"})
public class RequestDto {
    private Long id;

    private UUID messageId;

    private String serviceId;

    private OffsetDateTime messageDate;

    @Setter
    private ResponseForRequestDTO response;

    private String data;

    public static RequestDto create(Request request, boolean addData) {
        return new RequestDto(request.getId(), request.getMessageId(), request.getServiceId(),
                request.getMessageDate().withNano(0),  null, addData? request.getData() : null);
    }

    public static RequestDto create(Request request) {
       return create(request, false);
    }

    public static void setResponseData(Iterable<RequestDto> requests, List<Response> responses) {
        Map<Long, Response> responseMap = responses.stream()
                .collect(Collectors.toMap(k -> k.getRequest().getId(), v -> v));
        requests.forEach(r -> {
            var response = responseMap.get(r.getId());
            if (response != null) {
                r.setResponse(ResponseForRequestDTO.create(response));
            }
        });
    }
}
