package ru.igorit.andrk.model;


import lombok.*;

import org.hibernate.annotations.Type;
import org.hibernate.type.UUIDCharType;
import org.springframework.lang.NonNull;


import javax.annotation.Generated;
import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity(name = "requests")
@EqualsAndHashCode(of = "id")
public class Request implements Comparable<Request>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id")
    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID messageId;

    @Column(name = "correlation_id")
    @Type(type = "org.hibernate.type.UUIDCharType")
    private UUID correlationId;

    @Column(name = "service_id")
    private String serviceId;

    @NonNull
    @Column(name = "message_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime messageDate;

    @Column(name = "data",length = 32000)
    private String data;

    @Override
    public int compareTo(Request o) {
        return this.getId().compareTo(o.getId());
    }
}
