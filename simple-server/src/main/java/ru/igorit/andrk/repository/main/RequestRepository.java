package ru.igorit.andrk.repository.main;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.igorit.andrk.model.Request;

import java.util.List;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, Long> {
    Page<Request> findAllByIdLessThan(Long id, Pageable pageable);
    Page<Request> findAllByIdGreaterThan(Long id, Pageable pageable);
    long countByMessageId(UUID messageId);
    long countByMessageIdAndIdNot(UUID messageId, Long id);

    List<Request> findAllByIdBetween (Long lowId, Long highId);
}
