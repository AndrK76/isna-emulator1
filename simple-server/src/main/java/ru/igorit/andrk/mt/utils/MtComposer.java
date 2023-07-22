package ru.igorit.andrk.mt.utils;

import lombok.extern.log4j.Log4j2;
import ru.igorit.andrk.mt.structure.MtBlock;
import ru.igorit.andrk.mt.structure.MtContent;
import ru.igorit.andrk.mt.structure.MtItem;
import ru.igorit.andrk.mt.structure.MtNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Log4j2
public class MtComposer {

    public static String Compose(MtContent content) {
        String nl$ = System.lineSeparator();
        StringBuilder bld = new StringBuilder();
        content.getNodeList().stream()
                .sorted(Comparator.comparing(MtNode::getOrder))
                .forEach(node -> {
                    node.getBlocks().stream()
                            .sorted(Comparator.comparing(MtBlock::getId))
                            .forEach(block -> {
                                var composed = composeBlockText(block);
                                bld.append(nl$);
                                bld.append(composed);
                            });
                });
        String ret = bld.toString();
        if (!ret.isEmpty()){
            ret = ret.substring(nl$.length());
        }
        return "{"+ret+"-}";
    }


    private static String composeBlockText(MtBlock block) {
        StringBuilder sb = new StringBuilder();
        String splitter = block.getText().split("~")[0];
        String fmtStr = block.getText().split("~")[1];
        var values = block.getValues();
        int fmtPos = 0;
        boolean inFormat = false;
        List<Character> fmtBuf = new ArrayList<>();
        char fmtChar;
        while (fmtPos < fmtStr.length()) {
            fmtChar = fmtStr.charAt(fmtPos);
            if (!inFormat) {
                if (fmtChar == '{') {
                    inFormat = true;
                } else {
                    sb.append(fmtChar);
                }
            } else {
                if (fmtChar != '}') {
                    fmtBuf.add(fmtChar);
                } else {
                    var fmtString = MtParser.buf2str(fmtBuf);
                    fmtBuf.clear();
                    var fmtItem = MtItem.parse(fmtString);
                    var valItem = block.getItem(fmtItem.getCode());
                    if (valItem != null) {
                        sb.append(values.get(valItem));
                    }

                    inFormat = false;
                }
            }
            fmtPos++;
        }
        return sb.toString();
    }
}
