package ru.igorit.andrk.mt.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.igorit.andrk.config.services.ConfigFormatException;
import ru.igorit.andrk.mt.structure.*;
import ru.igorit.andrk.service.processor.DataFormatFatalException;

import java.util.*;
import java.util.stream.Collectors;

public class MtParser {
    private static final Logger log = LoggerFactory.getLogger(MtParser.class);

    private static final Map<String, MtItemType> itemTypes = new HashMap<>();

    static {
        for (var val : MtItemType.values()) {
            itemTypes.put(val.getCode(), val);
        }
    }

    public static Map<String, MtItemType> getItemTypes() {
        return itemTypes;
    }

    public static MtContent parsePreview(String rawData, MtFormat format) {
        var content = new MtContent(rawData, format);
        log.debug("parse ");
        log.trace(String.format("on start: %n%s%n", rawData));
        String clearedData = MtParser.clearMtString(rawData);
        log.trace(String.format("after clear:%n%s%n", clearedData));
        var strings = Arrays.stream(clearedData.split("\n"))
                .map(s -> s.replace("\r", ""))
                .filter(f -> f.length() > 0)
                .collect(Collectors.toList());
        int curFmtIdx = -1;

        for (String str : strings) {
            curFmtIdx = MtParser.parseDataRow(str, content.getNodes(), format.getNodes(), curFmtIdx);
        }

        for (var node : content.getNodes().values()) {
            var nodeName = node.getFormat().getNodeName();
            if (format.getPreviewFormats().containsKey(nodeName)) {
                var blockFmt = format.getPreviewFormats().get(nodeName);
                for (var block : node.getBlocks()) {
                    processBlockFormat(block, blockFmt, content.getItems());
                }
            }
        }
        return content;
    }

    public static void parseFinal(MtContent content, MtFormat format) {
        for (var node : content.getNodes().values()) {
            var nodeName = node.getCurrentCode();
            if (format.getDetailFormats().containsKey(nodeName)) {
                var blockFmt = format.getDetailFormats().get(nodeName);
                for (var block : node.getBlocks()) {
                    processBlockFormat(block, blockFmt, content.getItems());
                }
            }
        }
    }

    public static String clearMtString(String origText) {
        List<Character> symbolList = origText.codePoints().mapToObj(c -> (char) c).collect(Collectors.toList());

        clearSymbolsAtStart(symbolList, new CharPair('{', '-'), false); //remove { | {-
        Collections.reverse(symbolList);

        clearSymbolsAtStart(symbolList, new CharPair('}', '-'), false); //remove } | }-
        clearSymbolsAtStart(symbolList, new CharPair('\n', '\r'), true); //remove LF | CR
        clearSymbolsAtStart(symbolList, new CharPair('\n', '\n'), true); //remove LF | LF

        Collections.reverse(symbolList);
        return buf2str(symbolList);
    }

    private static int parseDataRow(String str, Map<MtFormatNodeInfo, MtNode> content,
                                    List<MtFormatNodeInfo> formats, int prevFmtIdx) {
        int curFmtIdx = prevFmtIdx;
        //log.debug("Str: {}", str);
        for (int i = (curFmtIdx == -1 ? 0 : curFmtIdx); i < formats.size(); i++) {
            var fmt = formats.get(i);
            var mask = fmt.getSearchMask();
            var testStr = str.substring(0, (Math.min(mask.length(), str.length())));
            if (testStr.equals(mask)) {
                curFmtIdx = i;
                var idx = content.get(fmt).getBlocks().size();
                content.get(fmt).getBlocks().add(new MtBlock(idx, str, content.get(fmt)));
            }
            //log.debug("fmt={}, mask={}, val={}", formats.get(i).getNodeName(), mask, testStr);
        }
        return curFmtIdx;
    }


    private static class CharPair {
        private final char requiredFirstSymbol;
        private final char optionalSecondSymbol;

        private CharPair(char requiredFirstSymbol, char optionalSecondSymbol) {
            this.requiredFirstSymbol = requiredFirstSymbol;
            this.optionalSecondSymbol = optionalSecondSymbol;
        }
    }

    private enum FindCharMode {
        IN_FIND_FIRST_SYMBOL,
        IN_FIND_SECOND_SYMBOL,
        AFTER_FIND_SYMBOLS;
    }

    private static boolean clearSymbolsAtStart(List<Character> charArray,
                                               CharPair symbolsForClear,
                                               boolean doRecursive) {
        char[] srcData = buf2str(charArray).toCharArray();
        FindCharMode findMode = FindCharMode.IN_FIND_FIRST_SYMBOL;
        charArray.clear();
        boolean clearIsMake = false;
        for (int i = 0; i < srcData.length; i++) {
            if (findMode == FindCharMode.IN_FIND_FIRST_SYMBOL) {
                if (srcData[i] == symbolsForClear.requiredFirstSymbol) {
                    findMode = FindCharMode.IN_FIND_SECOND_SYMBOL;
                    clearIsMake = true;
                } else {
                    charArray.add(srcData[i]);
                    findMode = FindCharMode.AFTER_FIND_SYMBOLS;
                }
            } else if (findMode == FindCharMode.IN_FIND_SECOND_SYMBOL) {
                findMode = FindCharMode.AFTER_FIND_SYMBOLS;
                if (srcData[i] != symbolsForClear.optionalSecondSymbol) {
                    charArray.add(srcData[i]);
                }
            } else {
                charArray.add(srcData[i]);
            }
        }
        if (doRecursive && clearIsMake){
            return clearSymbolsAtStart(charArray, symbolsForClear, true);
        } else{
            return clearIsMake;

        }
    }

    private static void processBlockFormat(MtBlock block, MtBlockFormat format, Map<String, MtItem> items) {
        if (block.getText() == null || block.getText().length() == 0
                || format == null || format.getFormatString() == null || format.getFormatString().length() == 0) {
            return;
        }
        int dataPos = 0;
        int fmtPos = 0;
        boolean inFormat = false;
        List<Character> fmtBuf = new ArrayList<>();
        char fmtChar, dataChar;
        while (dataPos < block.getText().length() && fmtPos < format.getFormatString().length()) {
            fmtChar = format.getFormatString().charAt(fmtPos);
            dataChar = block.getText().charAt(dataPos);
            if (!inFormat) {
                if (fmtChar != '{') {
                    dataPos++;
                    if (fmtChar != dataChar) {
                        var errText = "Несоответствие строки " + block.getText() + " формату " + format.getFormatString();
                        log.error(errText);
                        throw new DataFormatFatalException(errText);
                    }
                } else {
                    inFormat = true;
                }
            } else {
                if (fmtChar != '}') {
                    fmtBuf.add(fmtChar);
                } else {
                    var fmtString = buf2str(fmtBuf);
                    fmtBuf.clear();
                    var item = MtItem.parse(fmtString);
                    if (!items.containsKey(item.getCode())) {
                        items.put(item.getCode(), item);
                    }
                    var value = extractDataByFormat(block, format.getSplitter(), item, dataPos);
                    if (block.getValues().containsKey(item)) {
                        block.getValues().remove(item);
                    }
                    block.getValues().put(item, value);
                    dataPos += value.length();
                    inFormat = false;
                }
            }
            fmtPos++;
        }
        if (fmtBuf.size() > 0) {
            throw new ConfigFormatException("Некорректный формат: " + format.getFormatString());
        }
    }

    private static String extractDataByFormat(MtBlock block, String splitter, MtItem item, int dataPos) {
        List<Character> dataBuf = new ArrayList<>();
        String srcStr = block.getText();
        boolean isComplete = false;
        int curPos = dataPos;
        int charProcessed = 0;
        while (curPos < srcStr.length() && !isComplete && charProcessed < item.getLength()) {
            char curChar = srcStr.charAt(curPos);
            if (!item.isStrongLength() && splitter.charAt(0) == curChar) {
                isComplete = true;
            } else {
                dataBuf.add(curChar);
                curPos++;
                charProcessed++;
            }
        }
        return buf2str(dataBuf);
    }

    public static String buf2str(List<Character> buf) {
        return buf.stream().map(String::valueOf).collect(Collectors.joining());
    }

}
