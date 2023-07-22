package ru.igorit.andrk.mainstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import ru.igorit.andrk.model.StoredSetting;
import ru.igorit.andrk.model.StoredSettingKey;
import ru.igorit.andrk.repository.main.*;
import ru.igorit.andrk.service.MainStoreService;
import ru.igorit.andrk.service.store.MainStoreServiceJPAImpl;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DataJpaTest
@DirtiesContext
//@AutoConfigureJson
@TestPropertySource(locations = "/testDataJPA.properties")
public class TestSetting {
    @Autowired
    private DataSource dataSource;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private SettingRepository repo;


    @ParameterizedTest
    @ArgumentsSource(AnyTypeArgumentsProvider.class)
    @DisplayName("Check that StoredSetting correct serialize and deserialize")
    public void testSerializeDeserializeStoredSetting(Object srcVal) {
        assertThatNoException().isThrownBy(() -> {
            var tstKey = new StoredSettingKey("tstGrp", "testKey");
            var setting = new StoredSetting(tstKey, srcVal);
            var resVal = setting.getValue();
            assertThat(resVal).isEqualTo(srcVal);
        });
    }

    @ParameterizedTest
    @ArgumentsSource(AnyTypeArgumentsProvider.class)
    @DisplayName("Check that StoredSetting correct serialize/deserialize and store service correct save/load")
    public void testSerializeSaveLoadDeserialize(Object srcVal) {
        assertThatNoException().isThrownBy(() -> {
            var svc = getStoreService();
            var tstKey = new StoredSettingKey("tstGrp", "testKey");
            var setting = new StoredSetting(tstKey, srcVal);
            setting = svc.saveSetting(setting);
            entityManager.detach(setting);
            setting = svc.getSetting(tstKey);
            var resVal = setting.getValue();
            assertThat(resVal).isEqualTo(srcVal);
        });
    }

    private MainStoreService getStoreService() {
        var svc = MainStoreServiceJPAImpl.builder()
                .setRepo(repo)
                .build();
        return svc;
    }


    static class AnyTypeArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(10L),
                    Arguments.of(true),
                    Arguments.of("test value"),
                    Arguments.of(LocalDateTime.now())
            );
        }
    }
}
