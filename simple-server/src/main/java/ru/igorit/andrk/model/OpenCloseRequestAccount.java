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
@Entity(name = "open_close_request_accounts")
public class OpenCloseRequestAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private OpenCloseRequest request;

    @NonNull
    private Integer sort;

    private String account;

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



}
