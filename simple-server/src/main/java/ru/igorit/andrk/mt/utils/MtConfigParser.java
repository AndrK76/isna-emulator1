package ru.igorit.andrk.mt.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.igorit.andrk.config.services.ConfigFormatException;
import ru.igorit.andrk.mt.structure.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class MtConfigParser {
    private static final Logger log = LoggerFactory.getLogger(MtConfigParser.class);

    public static void parseInputFormatFromXML(byte[] configData, MtFormat format) {
        try {
            Document cfgDoc = getXmlDoc(configData);
            format.getNodes().clear();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/data/input/text()";
            String inputConfig = xPath.compile(expression).evaluate(cfgDoc);
            if (inputConfig.isEmpty()) {
                throw new Exception("Не найдено описание входного формата");
            }
            inputConfig = clearText(inputConfig);

            var sectionCfgData = Arrays.stream(inputConfig.split("\n"))
                    .map(s -> s.replace("\r", ""))
                    .map(MtConfigParser::parseInputFormatNodeRow)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            log.trace("nodes: {}", sectionCfgData);
            format.getNodes().addAll(sectionCfgData);

            var previewFormatsData = Arrays.stream(inputConfig.split("\n"))
                    .map(s -> s.replace("\r", ""))
                    .map(s -> parseItemInputFormatRow(s, "2"))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            for (var previewFormat : previewFormatsData) {
                format.getPreviewFormats().put(previewFormat.getFirst(), previewFormat.getSecond());
            }

            var detailFormatsData = Arrays.stream(inputConfig.split("\n"))
                    .map(s -> s.replace("\r", ""))
                    .map(s -> parseItemInputFormatRow(s, "3"))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            for (var detailFormat : detailFormatsData) {
                format.getDetailFormats().put(detailFormat.getFirst(), detailFormat.getSecond());
            }
        } catch (Exception e) {
            throw new ConfigFormatException(e);
        }
    }

    public static void parseOutputFormatFromXML(byte[] configData, MtFormat format) {
        try {
            Document cfgDoc = getXmlDoc(configData);
            format.getNodes().clear();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/data/output/text()";
            String outputConfig = xPath.compile(expression).evaluate(cfgDoc);
            if (outputConfig.isEmpty()) {
                throw new Exception("Не найдено описание выходного формата");
            }
            outputConfig = clearText(outputConfig);

            var sectionCfgData = Arrays.stream(outputConfig.split("\n"))
                    .map(s -> s.replace("\r", ""))
                    .map(MtConfigParser::parseOutputFormatNodeRow)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            log.trace("nodes: {}", sectionCfgData);
            format.getNodes().addAll(sectionCfgData);

            var detailFormatsData = Arrays.stream(outputConfig.split("\n"))
                    .map(s -> s.replace("\r", ""))
                    .map(MtConfigParser::parseItemOutputFormatRow)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            for (var detailFormat : detailFormatsData) {
                format.getDetailFormats().put(detailFormat.getFirst(), detailFormat.getSecond());
            }
            fillAllItemsInOutFormat(format);

        } catch (Exception e) {
            throw new ConfigFormatException(e);
        }
    }

    public static NodeList getCustomSection(byte[] configData, String sectionName) {
        Document cfgDoc = null;
        try {
            cfgDoc = getXmlDoc(configData);
            return cfgDoc.getElementsByTagName(sectionName);
        } catch (Exception e) {
            throw new ConfigFormatException(e);
        }
    }

    private static String clearText(String srcText) {
        return Arrays.stream(srcText.split("\n"))
                .map(r -> r.replace("\r", "").trim())
                .filter(r -> !r.isEmpty()).collect(Collectors.joining("\n"));
    }

    public static Document getXmlDoc(byte[] configData) throws ParserConfigurationException, IOException, SAXException {
        Document cfgDoc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder bld = factory.newDocumentBuilder();
        try (InputStream is = new ByteArrayInputStream(configData)) {
            cfgDoc = bld.parse(is);
        }
        return cfgDoc;
    }

    private static Optional<MtFormatNodeInfo> parseInputFormatNodeRow(String str) {
        var fields = str.split("~");
        if (fields.length > 1 && fields[0].equals("1")) {
            if (fields.length > 3) {
                var nodeInfo = new MtFormatNodeInfo(
                        fields[1],
                        Integer.parseInt(fields[2]),
                        fields[3],
                        MtNodeCountMode.One);
                if (fields.length > 4) {
                    nodeInfo.setCountMode(parseNode(fields[4]));
                }
                return Optional.of(nodeInfo);
            }
        }
        return Optional.empty();
    }

    private static Optional<MtFormatNodeInfo> parseOutputFormatNodeRow(String str) {
        var fields = str.split("~");
        if (fields.length > 3) {
            var nodeInfo = new MtFormatNodeInfo(
                    fields[0],
                    Integer.parseInt(fields[1]),
                    "",
                    parseNode(fields[2])
            );
            return Optional.of(nodeInfo);
        }
        return Optional.empty();
    }

    private static MtNodeCountMode parseNode(String countText) {
        if (countText.equals("N")) {
            return MtNodeCountMode.Many;
        } else if (countText.equals("1")) {
            return MtNodeCountMode.One;
        } else if (countText.equals("0")) {
            return MtNodeCountMode.ZeroOrOne;
        } else {
            return MtNodeCountMode.Other;
        }
    }

    private static Optional<Pair<String, MtBlockFormat>> parseItemInputFormatRow(String str, String levelKey) {
        var fields = str.split("~");
        if (fields.length > 3 && fields[0].equals(levelKey)) {
            Pair<String, MtBlockFormat> fmtInfo = Pair.of(fields[1], new MtBlockFormat(fields[2], fields[3]));
            return Optional.of(fmtInfo);
        }
        return Optional.empty();
    }

    private static Optional<Pair<String, MtBlockFormat>> parseItemOutputFormatRow(String str) {
        var fields = str.split("~");
        if (fields.length > 4) {
            Pair<String, MtBlockFormat> fmtInfo = Pair.of(fields[0], new MtBlockFormat(fields[3], fields[4]));
            return Optional.of(fmtInfo);
        }
        return Optional.empty();
    }

    private static void fillAllItemsInOutFormat(MtFormat format) {
        format.getItems().clear();
        for (var block : format.getDetailFormats().values()) {
            var fmtStr = block.getFormatString();
            if (fmtStr == null) {
                continue;
            }
            int fmtPos = 0;
            boolean inFormat = false;
            List<Character> fmtBuf = new ArrayList<>();
            char fmtChar;
            while (fmtPos < fmtStr.length()) {
                fmtChar = fmtStr.charAt(fmtPos);
                if (!inFormat) {
                    if (fmtChar == '{') {
                        inFormat = true;
                    }
                } else {
                    if (fmtChar != '}') {
                        fmtBuf.add(fmtChar);
                    } else {
                        var fmtString = MtParser.buf2str(fmtBuf);
                        fmtBuf.clear();
                        var item = MtItem.parse(fmtString);
                        if (!format.getItems().containsKey(item.getCode())) {
                            format.getItems().put(item.getCode(), item);
                        }
                        inFormat = false;
                    }
                }
                fmtPos++;
            }
            if (fmtBuf.size() > 0) {
                throw new ConfigFormatException("Некорректный выходной формат: " + fmtStr);
            }
        }
    }

}
