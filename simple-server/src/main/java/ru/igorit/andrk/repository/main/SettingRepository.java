package ru.igorit.andrk.repository.main;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.igorit.andrk.model.StoredSetting;
import ru.igorit.andrk.model.StoredSettingKey;

import java.util.List;

public interface SettingRepository extends JpaRepository<StoredSetting, StoredSettingKey> {

    @Query("select s from StoredSetting s where s.id.group = ?1")
    List<StoredSetting> findAllByGroupName(String groupName);
}
