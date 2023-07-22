package ru.igorit.andrk.parser;

import org.junit.jupiter.api.Test;
import ru.igorit.andrk.mt.structure.MtBlock;
import ru.igorit.andrk.mt.structure.MtContent;
import ru.igorit.andrk.mt.structure.MtFormat;
import ru.igorit.andrk.mt.structure.MtNode;
import ru.igorit.andrk.mt.utils.MtComposer;
import ru.igorit.andrk.mt.utils.MtConfigParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class MtComposerTests {

    private static final String SAMPLE_CFG = "sample_composer.cfg";

    private byte[] getConfig() throws IOException {
        return Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream(SAMPLE_CFG)).readAllBytes();
    }

    private MtContent createSampleContent() throws IOException {
        MtFormat outFormat = new MtFormat();
        MtConfigParser.parseOutputFormatFromXML(getConfig(), outFormat);
        MtContent content = new MtContent(outFormat);
        String[] constantNodeNames = new String[]{"HEAD", "ID", "MT_FORM", "SUBJECT"};
        Arrays.stream(constantNodeNames).forEach(nodeName -> {
            var node = content.getNode(nodeName, MtContent.FindNodeType.ByOrigCode);
            var block = new MtBlock(
                    0,
                    outFormat.getOutBlockFormatString(nodeName),
                    node);
            node.getBlocks().add(block);
        });

        LocalDateTime sampleDate = LocalDateTime.of(2023, 01, 02, 03, 04, 05, 06);
        var idBlock = content.getNode("ID").getBlocks().get(0);
        idBlock.setItem(outFormat.getItem("reference"), "REF001");
        var subjBlock = content.getNode("SUBJECT").getBlocks().get(0);
        subjBlock.setItem(outFormat.getItem("code_form"), "F01");
        subjBlock.setItem(outFormat.getItem("notify_date"), sampleDate);
        subjBlock.setItem(outFormat.getItem("name_form"), "Документ");
        MtNode accNode = content.getNode("ACCOUNT");
        for (int i = 0; i < 3; i++) {
            var accBlock = new MtBlock(
                    i,
                    outFormat.getOutBlockFormatString(accNode.getCurrentCode()),
                    accNode);
            accBlock.setBlockFormat(outFormat.getOutBlockFormat(accNode.getCurrentCode()));
            accNode.getBlocks().add(accBlock);
            accBlock.setItem(outFormat.getItem("bic"), "12345");
            accBlock.setItem(outFormat.getItem("account"), "Q" + i + "NUMBER001002003");
            accBlock.setItem(outFormat.getItem("account_type"), "01");
            accBlock.setItem(outFormat.getItem("oper_type"), 2);
            accBlock.setItem(outFormat.getItem("oper_date"), sampleDate);
        }
        content.getItems().putAll(outFormat.getItems());
        return content;
    }


    @Test
    void checkThat_composeDontThrowErrorAndMakeExpectedResult() throws IOException {
        var content = createSampleContent();
        assertThatNoException().isThrownBy(()->{
            String composedText = MtComposer.Compose(content);
            content.setRawData(composedText);
        });
        String exceptedText = "{4:\r\n" +
                ":20:REF001\r\n" +
                ":12:400\r\n" +
                ":77E:FORMS/F01/202301020304/Документ\r\n" +
                "/ACCOUNT/12345/Q0NUMBER001002003/01/2/20230102\r\n" +
                "/ACCOUNT/12345/Q1NUMBER001002003/01/2/20230102\r\n" +
                "/ACCOUNT/12345/Q2NUMBER001002003/01/2/20230102-}";
        assertThat(content.getRawData()).isEqualTo(exceptedText);
    }
}
