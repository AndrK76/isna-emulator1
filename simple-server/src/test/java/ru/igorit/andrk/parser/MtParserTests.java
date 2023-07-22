package ru.igorit.andrk.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.igorit.andrk.mt.structure.MtFormat;
import ru.igorit.andrk.mt.structure.MtFormatNodeInfo;
import ru.igorit.andrk.mt.utils.MtConfigParser;
import ru.igorit.andrk.mt.utils.MtParser;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class MtParserTests {

    private static final String SAMPLE_CFG = "sample_parser.cfg";

    @ParameterizedTest
    @MethodSource("clearStringParameters")
    void clearString_shouldReturnCorrectResult(String srcString, String resString) {
        assertThat(MtParser.clearMtString(srcString)).isEqualTo(resString);
    }

    private static Stream<Arguments> clearStringParameters() {
        return Stream.of(
                Arguments.of("{test}", "test"),
                Arguments.of("{test\n-}", "test"),
                Arguments.of("{test\ntest}", "test\ntest"),
                Arguments.of("{test\ntest\n-}", "test\ntest"),
                Arguments.of("{test\ntest\n\n\n-}", "test\ntest"),
                Arguments.of("{test\r\ntest}", "test\r\ntest"),
                Arguments.of("{test\ntest\r\n\r\n\r\n\r\n\r\n-}", "test\ntest"),
                Arguments.of("{{test\ntest\r\n\r\n\r\n\r\n}\r\n-}", "{test\ntest\r\n\r\n\r\n\r\n}")
        );
    }


    @ParameterizedTest
    @MethodSource("mtDataSource")
    void parsePreview_shouldReturnCorrectResult(String srcData, String expectedCodeForm) throws IOException {
        MtFormat inputFormat = new MtFormat();
        MtConfigParser.parseInputFormatFromXML(getConfig(), inputFormat);
        var content = MtParser.parsePreview(srcData, inputFormat);
        var nodeKeys = content.getNodes().keySet().stream().map(MtFormatNodeInfo::getNodeName).collect(Collectors.joining(","));
        assertThat(nodeKeys).isEqualTo("SUBJECT,ID,ACCOUNT");
        var itemKeys = content.getItems().keySet();
        assertThat(itemKeys).containsAll(Arrays.asList("reference", "code_form", "notify_date"));
        var actualCodeForm = (String) content.getValue("code_form");
        assertThat(actualCodeForm).isEqualTo(expectedCodeForm);
    }

    @ParameterizedTest
    @MethodSource("mtDataSource")
    void parseFinal_shouldReturnCorrectResult(String srcData,
                                              String expectedCodeForm,
                                              Map<String, String> changedValues,
                                              Map<String, DataItemInfo> dataItems
    ) throws IOException {
        MtFormat inputFormat = new MtFormat();
        MtConfigParser.parseInputFormatFromXML(getConfig(), inputFormat);
        var content = MtParser.parsePreview(srcData, inputFormat);

        assertThatNoException().isThrownBy(() -> {
            if (changedValues != null) {
                for (var val : changedValues.entrySet()) {
                    content.getNode(val.getKey()).setCurrentCode(val.getValue());
                }
            }
            MtParser.parseFinal(content, inputFormat);
        });

        if (dataItems != null) {
            for (var blockName : dataItems.keySet()) {
                var itemInfo = dataItems.get(blockName);
                int exceptedCount = itemInfo.countBlocks;
                var actualCount = content.getNode(blockName).getBlocks().size();
                assertThat(actualCount).isEqualTo(exceptedCount);
                var itemValMap = itemInfo.values;
                if (itemValMap != null) {
                    for (var item : itemValMap.keySet()) {
                        var expectedVals = itemValMap.get(item);
                        List<Object> actualVals = new ArrayList<>();
                        for (int i = 0; i < exceptedCount; i++) {
                            actualVals.add(content.getValue(item, i));
                        }
                        assertThat(actualVals).containsExactly(expectedVals);
                    }
                }
            }
        }
    }


    private byte[] getConfig() throws IOException {
        return Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(SAMPLE_CFG)).readAllBytes();
    }


    private static class DataItemInfo {
        private int countBlocks;
        private Map<String, Object[]> values;

        public DataItemInfo(int countBlocks, Map<String, Object[]> values) {
            this.countBlocks = countBlocks;
            this.values = values;
        }
    }

    private static Stream<Arguments> mtDataSource() {
        var itemValues1_1 = Stream.of(
                        new AbstractMap.SimpleEntry<>(
                                "account",
                                new Object[]{
                                        "KZ484324302398A00006",
                                        "KZ164322204398R09704"})
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var dataItems1 = Stream.of(
                        new AbstractMap.SimpleEntry<>("ACCOUNT",
                                new DataItemInfo(2, itemValues1_1)
                        ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                );
        var itemValues2_1 = Stream.of(
                        new AbstractMap.SimpleEntry<>(
                                "account",
                                new Object[]{
                                        "KZ484324302398A00006"})
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var dataItems2 = Stream.of(
                        new AbstractMap.SimpleEntry<>("ACCOUNT_CHANGE",
                                new DataItemInfo(1, itemValues2_1)
                        ))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                );

        return Stream.of(
                Arguments.of("{4:\n" +
                                ":20:V306154735451685\n" +
                                ":12:400\n" +
                                ":77E:FORMS/A01/202306151309/Увед. об откр. и закр. банковских счетов\n" +
                                "/ACCOUNT/VTBAKZKZ/KZ484324302398A00006/05/1/20230301/450509833484//\n" +
                                "/ACCOUNT/VTBAKZKZ/KZ164322204398R09704/20/2/20210406/143346407250//\n" +
                                "-}",
                        "A01",
                        null,
                        dataItems1
                ),
                Arguments.of("{4:\n" +
                                ":20:V306197160121274\n" +
                                ":12:400\n" +
                                ":77E:FORMS/A03/202306191953/Увед. об изменении банковских счетов\n" +
                                "/ACCOUNT/VTBAKZKZ/KZ484324302398A00006/05/20230301/450509833484/VTBAKZKZ/398A00006/20230301\n" +
                                "-}",
                        "A03",
                        Stream.of(
                                        new AbstractMap.SimpleEntry<>("ACCOUNT", "ACCOUNT_CHANGE"))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
                                ),
                        dataItems2
                )
        );
    }
}
