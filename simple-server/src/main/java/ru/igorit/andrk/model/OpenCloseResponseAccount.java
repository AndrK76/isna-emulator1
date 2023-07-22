package ru.igorit.andrk.model;

import lombok.*;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "open_close_response_accounts")
public class OpenCloseResponseAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "response_id")
    private OpenCloseResponse response;


    @NonNull
    private Integer sort;

    private String account;

    @NonNull
    @Column(name = "result_code")
    private String resultCode;

    @Column(name = "result_message")
    private String resultMessage;


    @Column(name = "oper_type")
    private Integer operType;

    @Column(name = "account_type")
    private String accountType;

    private String bic;

    @Column(name = "oper_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime operDate;

    private String rnn;

    private String dog;

    @Column(name = "dog_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime dogDate;

    @Column(name = "bic_old")
    private String bicOld;

    @Column(name = "account_old")
    private String accountOld;

    @Column(name = "date_modify", columnDefinition = "TIMESTAMP")
    private LocalDateTime dateModify;

    public OpenCloseResponseAccount(OpenCloseResponse resp, OpenCloseRequestAccount acc){
        this();
        this.setResponse(resp);
        resp.getAccounts().add(this);
        this.setSort(acc.getSort());
        this.setAccount(acc.getAccount());
        this.setAccountType(acc.getAccountType());
        this.setOperType(acc.getOperType());
        this.setOperDate(acc.getOperDate());
        this.setBic(acc.getBic());
        this.setRnn(acc.getRnn());
        this.setDog(acc.getDog());
        this.setDogDate(acc.getDogDate());
        this.setAccountOld(acc.getAccountOld());
        this.setBicOld(acc.getBicOld());
        this.setDateModify(acc.getDateModify());
    }

}
