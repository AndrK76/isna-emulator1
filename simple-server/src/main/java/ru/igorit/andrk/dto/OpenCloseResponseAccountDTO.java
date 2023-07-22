package ru.igorit.andrk.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import ru.igorit.andrk.model.OpenCloseResponseAccount;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Jacksonized
public class OpenCloseResponseAccountDTO {
    private Long id;
    private Integer sort;
    private String bic;
    private String account;
    private String accountType;
    private Integer operType;
    private LocalDateTime operDate;
    private String resultCode;
    private String resultMessage;
    private String rnn;
    private String dog;
    private LocalDateTime dogDate;
    private String bicOld;
    private String accountOld;
    private LocalDateTime dateModify;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static OpenCloseResponseAccountDTO create(OpenCloseResponseAccount account) {
        var ret = new OpenCloseResponseAccountDTO();
        ret.id = account.getId();
        ret.sort = account.getSort();
        ret.account=account.getAccount();
        ret.accountType=account.getAccountType();
        ret.operType=account.getOperType();
        ret.operDate=account.getOperDate();
        ret.resultCode=account.getResultCode();
        ret.resultMessage= account.getResultMessage();
        ret.rnn=account.getRnn();
        ret.dog= account.getDog();
        ret.dogDate=account.getDogDate();
        ret.bicOld= account.getBicOld();
        ret.accountOld=account.getAccountOld();
        ret.dateModify=account.getDateModify();
        return ret;
    }
}
