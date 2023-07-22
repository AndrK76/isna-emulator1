package ru.igorit.andrk.repository.main;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.igorit.andrk.model.*;

import java.util.List;

public interface OpenCloseResponseRepository extends JpaRepository<OpenCloseResponse, Long> {

    Page<OpenCloseResponse> findAllByIdLessThan(Long id, Pageable pageable);

    List<OpenCloseResponse> findAllByRequestBetween(OpenCloseRequest first, OpenCloseRequest last);

    @Query("select s from OpenCloseResponseAccount s where s.account=?1 and s.operType=?2 and s.resultCode=?3 order by s.dateModify desc, s.id desc")
    Page<OpenCloseResponseAccount> findLastAccountsByCondition(String account, Integer operType, String resultCode, Pageable pageable);

}
