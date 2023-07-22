package ru.igorit.andrk.repository.main;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.igorit.andrk.model.OpenCloseRequest;
import ru.igorit.andrk.model.Request;

public interface OpenCloseRequestRepository extends JpaRepository<OpenCloseRequest,Long> {
    Page<OpenCloseRequest> findAllByIdLessThan(Long id, Pageable pageable);
    Page<OpenCloseRequest> findAllByIdGreaterThan(Long id, Pageable pageable);
    long countByReference(String reference);
    long countByReferenceAndIdNot(String reference, Long id);
}
