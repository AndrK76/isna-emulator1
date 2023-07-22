package ru.igorit.andrk.repository.main;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.Response;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response,Long> {
    Page<Response> findAllByIdLessThan(Long id, Pageable pageable);

    List<Response> findAllByRequestBetween(Request first, Request last);
}
