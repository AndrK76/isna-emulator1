package ru.igorit.andrk.service.processor.openclose;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.igorit.andrk.dto.StoredSettingDTO;
import ru.igorit.andrk.model.StoredSetting;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
public class OpenCloseDynamicSettings {
    private boolean checkUniqueMessageId = false;
    private boolean checkUniqueReference = false;
    private boolean validateAccountState = false;
    private boolean validateOperationDate = false;
    private boolean raiseTestError = false;

    private boolean useFixReference = false;

    private String fixReference = "";

    public static OpenCloseDynamicSettings create(List<StoredSetting> storedSettings) {
        var ret = new OpenCloseDynamicSettings();
        var stored = storedSettings.stream()
                .map(StoredSettingDTO::new)
                .collect(Collectors.toMap(StoredSettingDTO::getKey, StoredSettingDTO::getValue));
        ret.checkUniqueMessageId = getStoredBool(stored, "CheckUniqueMessageId", ret.checkUniqueMessageId);
        ret.checkUniqueReference = getStoredBool(stored, "CheckUniqueReference", ret.checkUniqueReference);
        ret.validateAccountState = getStoredBool(stored, "ValidateAccountState", ret.validateAccountState);
        ret.validateOperationDate = getStoredBool(stored, "ValidateOperationDate", ret.validateOperationDate);
        ret.raiseTestError = getStoredBool(stored, "RaiseTestError", ret.raiseTestError);
        ret.useFixReference = getStoredBool(stored, "UseFixReference", ret.useFixReference);
        ret.fixReference = getStoredString(stored, "FixReference", ret.fixReference);
        return ret;
    }

    private static boolean getStoredBool(Map<String, Object> values, String propertyName, boolean defaultValue) {
        return (Boolean) values.getOrDefault(propertyName, defaultValue);
    }

    private static String getStoredString(Map<String, Object> values, String propertyName, String defaultValue) {
        return (String) values.getOrDefault(propertyName, defaultValue);
    }
}
