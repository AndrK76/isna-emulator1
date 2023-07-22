package ru.igorit.andrk.parser;

import org.junit.jupiter.api.Test;
import ru.igorit.andrk.mt.structure.MtFormat;
import ru.igorit.andrk.mt.structure.MtItemType;
import ru.igorit.andrk.mt.structure.MtNodeCountMode;
import ru.igorit.andrk.mt.utils.MtConfigParser;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ConfigParserTests {

    private static final String SAMPLE_CFG = "sample_parser.cfg";

    private byte[] getConfig() throws IOException {
        return this.getClass().getClassLoader().getResourceAsStream(SAMPLE_CFG).readAllBytes();
    }

    @Test
    void checkThatInputFormatCorrectlyParsed() throws IOException {
        MtFormat inputFormat = new MtFormat();
        MtConfigParser.parseInputFormatFromXML(getConfig(), inputFormat);
        var nodes = inputFormat.getNodes();
        assertThat(nodes).isNotNull();
        assertThat(nodes.size()).isEqualTo(3);
        assertThat(nodes.get(0).getNodeName()).isEqualTo("ID");
        assertThat(nodes.get(1).getNodeName()).isEqualTo("SUBJECT");
        assertThat(nodes.get(2).getNodeName()).isEqualTo("ACCOUNT");
        assertThat(inputFormat.getPreviewFormats())
                .isNotNull()
                .hasSize(2)
                .containsOnlyKeys("SUBJECT", "ID");
        assertThat(inputFormat.getDetailFormats())
                .isNotNull()
                .hasSize(2)
                .containsOnlyKeys("ACCOUNT", "ACCOUNT_CHANGE");
        assertThat(nodes.get(1).getSearchMask()).isEqualTo(":77E:FORMS/");
        assertThat(nodes.get(1).getCountMode()).isEqualTo(MtNodeCountMode.One);
    }

    @Test
    void checkThatOutputFormatCorrectlyParsed() throws IOException {
        MtFormat outFormat = new MtFormat();
        MtConfigParser.parseOutputFormatFromXML(getConfig(), outFormat);
        assertThat(outFormat.getNodes())
                .isNotNull()
                .hasSize(6);
        assertThat(outFormat.getDetailFormats())
                .isNotNull()
                .hasSize(6);
        assertThat(outFormat.getItems())
                .isNotNull()
                .hasSize(15)
                .containsKeys("account_type", "result_name", "notify_date");
        var accountTypeItem = outFormat.getItem("account_type");
        assertThat(accountTypeItem.isRequired()).isTrue();
        assertThat(accountTypeItem.isStrongLength()).isTrue();
        assertThat(accountTypeItem.getType()).isEqualTo(MtItemType.STRING);
        assertThat(accountTypeItem.getLength()).isEqualTo(2);
        assertThat(accountTypeItem.getFormatString()).isNullOrEmpty();
        var notifyDateItem = outFormat.getItem("notify_date");
        assertThat(notifyDateItem.getFormatString()).isEqualTo("yyyyMMddHHmm");
    }

    @Test
    void testThatCorrectReadCustomSection() throws IOException {
        var nodes = MtConfigParser.getCustomSection(getConfig(), "results");
        assertThat(nodes).isNotNull();
        assertThat(nodes.getLength()).isEqualTo(1);
        var childNodes = nodes.item(0).getChildNodes();
        assertThat(childNodes.getLength()).isEqualTo(39);
    }

}
