package ru.igorit.andrk.controllertests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.igorit.andrk.api.ManageController;
import ru.igorit.andrk.dto.RequestDto;
import ru.igorit.andrk.dto.StoredSettingDTO;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.model.StoredSetting;
import ru.igorit.andrk.model.StoredSettingKey;
import ru.igorit.andrk.service.MainStoreService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.igorit.andrk.config.services.Constants.API_VERSION;

@WebMvcTest(ManageController.class)
public class ManageControllerSettingsTests {

    private static final String PATH = "/api/" + API_VERSION;

    @MockBean
    private MainStoreService mainStoreService;

    @Autowired
    private MockMvc client;

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final List<StoredSetting> sampleData = new ArrayList<>();


    @BeforeEach
    void init() {
        sampleData.add(new StoredSetting(
                new StoredSettingKey("group1", "setting1"),
                "String value"));
        sampleData.add(new StoredSetting(
                new StoredSettingKey("group1", "setting2"),
                LocalDateTime.now()));
        sampleData.add(new StoredSetting(
                new StoredSettingKey("group1", "setting3"),
                Long.parseLong("1234567890")));
        sampleData.add(new StoredSetting(
                new StoredSettingKey("group2", "setting1"),
                Request.builder().id(1L).messageDate(OffsetDateTime.now()).build()));
        sampleData.add(new StoredSetting(
                new StoredSettingKey("group2", "setting2"),
                RequestDto.builder().id(2L).messageId(UUID.randomUUID()).build()));
        when(mainStoreService.getSettingsByGroup(anyString()))
                .thenAnswer((Answer<List<StoredSetting>>) invocation -> {
                    String key = (String) invocation.getArguments()[0];
                    return sampleData.stream()
                            .filter(r -> r.getId().getGroup().equals(key))
                            .collect(Collectors.toList());
                });
        when(mainStoreService.getSetting(any(StoredSettingKey.class)))
                .thenAnswer(invocation -> {
                    StoredSettingKey key = (StoredSettingKey) invocation.getArguments()[0];
                    var ret = sampleData.stream().filter(r -> r.getId().equals(key)).findFirst();
                    return ret.orElse(null);
                });
        when(mainStoreService.saveSetting(any(StoredSetting.class)))
                .thenAnswer((Answer<StoredSetting>) invocation -> {
                    StoredSetting setting = (StoredSetting) invocation.getArguments()[0];
                    if (setting == null) {
                        return null;
                    }
                    var stored = sampleData.stream()
                            .filter(r -> r.getId().equals(setting.getId()))
                            .findFirst().orElse(null);
                    if (stored == null) {
                        sampleData.add(setting);
                        return setting;
                    } else {
                        stored.setValue(setting.getValue());
                        return stored;
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"group1", "group2", "group3"})
    void givenGetExistingSettings_shouldCorrectRead(String group) throws Exception {
        var expected = StoredSettingDTO.create(sampleData.stream()
                .filter(r -> r.getId().getGroup().equals(group))
                .collect(Collectors.toList()));
        var requestRes = client.perform(get(PATH + "/setting/{service}", group))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();
        var typeDef = TypeFactory.defaultInstance().constructCollectionLikeType(List.class, StoredSettingDTO.class);
        var actualResult = (List<StoredSettingDTO>) objectMapper.readValue(responseString, typeDef);
        assertThat(actualResult).hasSize(Math.toIntExact(expected.size()));
        actualResult.forEach(StoredSettingDTO::actualizeValue);
        assertThat(actualResult.stream().map(StoredSettingDTO::getKey).collect(Collectors.toList()))
                .containsExactlyElementsOf(expected.stream().map(StoredSettingDTO::getKey).collect(Collectors.toList()));
        assertThat(actualResult.stream().map(r -> r.getValueType().getName()).collect(Collectors.toList()))
                .containsExactlyElementsOf(expected.stream().map(r -> r.getValueType().getName()).collect(Collectors.toList()));
        assertThat(actualResult.stream().map(StoredSettingDTO::getValue).collect(Collectors.toList()))
                .containsExactlyElementsOf(expected.stream().map(StoredSettingDTO::getValue).collect(Collectors.toList()));
    }

    @ParameterizedTest
    @MethodSource("sourceGetExisting")
    void givenSettingValue_shouldCorrectRead(String group, String name, Object exceptedValue) throws Exception {
        var requestRes = client.perform(get(PATH + "/setting/{service}/{setting}", group, name))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();
        if (exceptedValue == null) {
            assertThat(responseString).isNullOrEmpty();
        } else {
            if (exceptedValue.getClass().equals(String.class)) {
                assertThat(responseString).isEqualTo(exceptedValue);
            } else {
                var actualValue = objectMapper.readValue(responseString, exceptedValue.getClass());
                assertThat(actualValue).isEqualTo(exceptedValue);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("sourceGetExisting")
    void givenSettingSetting_shouldCorrectRead(String group, String name, Object exceptedValue) throws Exception {
        var requestRes = client.perform(get(PATH + "/setting/{service}/{setting}/exact", group, name))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();
        if (exceptedValue == null) {
            assertThat(responseString).isNullOrEmpty();
        } else {
            var actualDto = objectMapper.readValue(responseString, StoredSettingDTO.class);
            actualDto.actualizeValue();
            assertThat(actualDto.getValue()).isEqualTo(exceptedValue);
        }
    }

    private static Stream<Arguments> sourceGetExisting() {
        return Stream.of(
                Arguments.of("group1", "setting1", "String value")
                , Arguments.of("group2", "setting1", Request.builder().id(1L).messageDate(OffsetDateTime.now()).build())
                , Arguments.of("group3", "setting1", null)
                , Arguments.of("group1", "setting4", null)
        );
    }

    @ParameterizedTest
    @MethodSource("sourceSave")
    void givenValue_shouldCorrectSave(String group, String name, Object exceptedValue) throws Exception {
        var json = objectMapper.writeValueAsString(exceptedValue);

        var requestRes = client.perform(post(PATH + "/setting/{service}/{setting}", group, name)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsString();
        if (exceptedValue.getClass().equals(String.class)) {
            assertThat(responseString).isEqualTo(exceptedValue);
        } else {
            var actualValue = objectMapper.readValue(responseString, exceptedValue.getClass());
            assertThat(actualValue).isEqualTo(exceptedValue);
        }
    }

    @ParameterizedTest
    @MethodSource("sourceSave")
    void givenSetting_shouldCorrectSave(String group, String name, Object exceptedValue) throws Exception {
        var expected = StoredSettingDTO.builder()
                .key(name)
                .valueType(exceptedValue.getClass())
                .value(exceptedValue)
                .build();
        var json = objectMapper.writeValueAsString(expected);

        var requestRes = client.perform(post(PATH + "/setting/{service}/{setting}/exact", group, name)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        var responseString = requestRes.getResponse().getContentAsByteArray();
        var actualDto = objectMapper.readValue(responseString, StoredSettingDTO.class);
        actualDto.actualizeValue();
        assertThat(actualDto.getValue()).isEqualTo(exceptedValue);
    }

    private static Stream<Arguments> sourceSave() {
        return Stream.of(
                Arguments.of("group1", "setting1", 50L)
                , Arguments.of("group2", "setting1", LocalDateTime.now().withNano(0))
                , Arguments.of("group3", "setting1", Request.builder().id(1L).messageDate(OffsetDateTime.now()).build())
                , Arguments.of("group1", "setting4", "Абракадабра")
        );
    }


}
