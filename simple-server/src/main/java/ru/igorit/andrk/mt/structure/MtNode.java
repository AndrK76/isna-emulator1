package ru.igorit.andrk.mt.structure;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MtNode {
    private final MtFormatNodeInfo format;
    private final List<MtBlock> blocks = new ArrayList<>();
    private int order;
    private String currentCode;

    public MtNode(MtFormatNodeInfo format) {
        this.format = format;
        this.order = format.getOrder();
        currentCode = format.getNodeName();
    }

    @Override
    public String toString() {
        return "MtNode{" +
                "format=" + format.getNodeName() +
                ", order=" + order +
                ", blocks=" + blocks +
                ", currentCode='" + currentCode + '\'' +
                '}';
    }
}
