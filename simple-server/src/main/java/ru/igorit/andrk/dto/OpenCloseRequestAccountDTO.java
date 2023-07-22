package ru.igorit.andrk.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import ru.igorit.andrk.model.OpenCloseRequest;
import ru.igorit.andrk.model.OpenCloseRequestAccount;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
@AllArgsConstructor
@NoArgsConstructor
public class OpenCloseRequestAccountDTO {
    private Long id;
    private Integer sort;
    private String bic;
    private String account;
    private String accountType;
    private Integer operType;
    private LocalDateTime operDate;
    private String rnn;
    private String dog;
    private LocalDateTime dogDate;
    private String bicOld;
    private String accountOld;
    private LocalDateTime dateModify;
    public static OpenCloseRequestAccountDTO create(OpenCloseRequestAccount account){
        var ret = new OpenCloseRequestAccountDTO();
        ret.id = account.getId();
        ret.sort = account.getSort();
        ret.bic = account.getBic();
        ret.account = account.getAccount();
        ret.accountType = account.getAccountType();
        ret.operType = account.getOperType();
        ret.operDate = account.getOperDate();
        ret.rnn = account.getRnn();
        ret.dog = account.getDog();
        ret.dogDate = account.getDogDate();
        ret.bicOld = account.getBicOld();
        ret.accountOld = account.getAccountOld();
        ret.dateModify = account.getDateModify();
        return ret;
    }
}
