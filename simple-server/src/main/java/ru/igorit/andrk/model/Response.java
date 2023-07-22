package ru.igorit.andrk.model;

import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "responses")
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    @NonNull
    @Setter
    private Request request;

    @NonNull
    @Column(name = "message_id")
    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID messageId;

    @Column(name = "correlation_id")
    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID correlationId;

    @Column(name = "service_id")
    private String serviceId;

    @NonNull
    @Column(name = "is_success")
    @Setter
    private Boolean isSuccess;

    @NonNull
    @Column(name = "response_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime responseDate;

    @Column(name = "status_code")
    @Setter
    private String statusCode;

    @Column(name = "status_message")
    @Setter
    private String statusMessage;

    @Column(name = "data",length = 32000)
    @Setter
    private String data;
    public Response(Request request){
        this();
        this.request = request;
        this.messageId = UUID.randomUUID();
        this.correlationId = request.getMessageId();
        this.serviceId = request.getServiceId();
        this.responseDate = OffsetDateTime.now();
    }
}
