package ru.igorit.andrk.model;

import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "open_close_requests")
public class OpenCloseRequest implements Comparable<OpenCloseRequest> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private String reference;

    @NonNull
    @Column(name = "code_form")
    private String codeForm;

    @NonNull
    @Column(name = "notify_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime notifyDate;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "request", cascade = CascadeType.ALL)
    private List<OpenCloseRequestAccount> accounts = new ArrayList<>();

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_request_id")
    private Request rawRequest;

    public OpenCloseRequest(Request request) {
        this();
        this.rawRequest = request;
        //this.messageId = request.getMessageId();
    }
    @Override
    public int compareTo(OpenCloseRequest o) {
        return this.getId().compareTo(o.getId());
    }
}
