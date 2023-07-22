package ru.igorit.andrk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.igorit.andrk.mainstore.CommonCreators;
import ru.igorit.andrk.model.Request;
import ru.igorit.andrk.service.MainStoreService;

import java.time.OffsetDateTime;
import java.util.UUID;

@SpringBootTest()
@TestPropertySource(locations = "/testH2NoMigrate.properties")
class Test2dbConnApplicationTests {

    @Autowired
    private MainStoreService storeSvc;


    //@Test
    void testThatStoreServiceCorrectSaveRequests() {
        var request = CommonCreators.makeMainRequest();
        var request2 = storeSvc.saveRequest(request);
        System.out.println("");
    }

    //@Test
    void testThatStoreServiceCorrectGetPages(){
        for (int i = 0; i < 95; i++) {
            storeSvc.saveRequest(CommonCreators.makeMainRequest());
        }
        Long pos = null;
        for (int i = 0; i < 10; i++) {
            var page = storeSvc.getRequests(pos, 10);
            var min = page.stream().min((o1, o2) -> o1.getId().compareTo(o2.getId()));
            pos = min.get().getId();
            var total = page.getTotalElements();
            var size = page.getContent().size();
            var size2 = page.getSize();
            System.out.println("");
        }

    }


}
