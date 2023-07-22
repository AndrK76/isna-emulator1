package ru.igorit.andrk.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import ru.igorit.andrk.model.OpenCloseRequestAccount;
import ru.igorit.andrk.model.OpenCloseResponse;
import ru.igorit.andrk.model.OpenCloseResponseAccount;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Jacksonized
@EqualsAndHashCode(of = {"id"})
public class OpenCloseResponseForRequestDTO {

    private Long id;
    private String codeForm;
    private LocalDateTime notifyDate;

    private List<OpenCloseResponseAccountDTO> accounts = new ArrayList<>();


    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static OpenCloseResponseForRequestDTO create(OpenCloseResponse response, boolean loadAccounts) {
        var ret = new OpenCloseResponseForRequestDTO();
        ret.id = response.getId();
        ret.codeForm = response.getCodeForm();
        ret.notifyDate = response.getNotifyDate().withNano(0);
        if (loadAccounts) {
            response.getAccounts().stream()
                    .sorted(Comparator.comparing(OpenCloseResponseAccount::getSort))
                    .forEach(r -> ret.getAccounts().add(OpenCloseResponseAccountDTO.create(r)));
        }
        return ret;
    }

    public static OpenCloseResponseForRequestDTO create(OpenCloseResponse response) {
        return create(response, false);
    }

}
