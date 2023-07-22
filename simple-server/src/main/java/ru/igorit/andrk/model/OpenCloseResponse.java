package ru.igorit.andrk.model;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity(name = "open_close_responses")
public class OpenCloseResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private OpenCloseRequest request;

    @NonNull
    private String reference;

    @NonNull
    @Column(name = "code_form")
    private String codeForm;

    @NonNull
    @Column(name = "notify_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime notifyDate;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "response", cascade = CascadeType.ALL)
    private List<OpenCloseResponseAccount> accounts = new ArrayList<>();

    public OpenCloseResponse(OpenCloseRequest request){
        this();
        this.request = request;
        //this.messageId = request.getMessageId();
        this.reference = request.getReference();
    }

}
