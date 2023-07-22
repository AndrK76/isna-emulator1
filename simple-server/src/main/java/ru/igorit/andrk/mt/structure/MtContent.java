package ru.igorit.andrk.mt.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;

public class MtContent {

    public enum FindNodeType {
        ByCurrentCode,
        ByOrigCode
    }


    @Getter
    private final Map<MtFormatNodeInfo, MtNode> nodes = new HashMap<>();

    @Getter
    private final Map<String, MtItem> items = new HashMap<>();

    @Getter
    @Setter
    private String rawData;

    public MtContent(String rawData, MtFormat format) {
        this.rawData=rawData;
        for (var fmt : format.getNodes()) {
            nodes.put(fmt, new MtNode(fmt));
        }
    }

    public MtContent(MtFormat format) {
        this("",format);
    }


    public List<MtNode> getNodeList() {
        return new ArrayList<>(nodes.values());
    }

    public List<MtBlock> getBlocks() {
        return getNodeList().stream().flatMap(r -> r.getBlocks().stream()).collect(Collectors.toList());
    }

    public Object getValue(String itemCode, int level) {
        if (!getItems().containsKey(itemCode)) {
            return null;
        }
        var item = getItems().get(itemCode);
        for (var block : getBlocks().stream().filter(r -> r.getId() == level).collect(Collectors.toList())) {
            if (block.getValues().containsKey(item)) {
                var val = block.getValues().get(item);
                return item.extractValue(val);
            }
        }
        return null;
    }

    public Object getValue(String itemCode) {
        return getValue(itemCode, 0);
    }

    public boolean checkOnEmpty(String itemCode, int level) {
        if (!getItems().containsKey(itemCode)) {
            return false;
        }
        var item = getItems().get(itemCode);
        for (var block : getBlocks().stream().filter(r -> r.getId() == level).collect(Collectors.toList())) {
            if (block.getValues().containsKey(item)) {
                var val = block.getValues().get(item);
                if (val!=null && !val.isEmpty()|| !item.isRequired() ){
                    return true;
                }
            }
        }
        return false;
    }

    public MtNode getNode(String code, FindNodeType findType) {
        var ret = getNodeList().stream()
                .filter(r -> code.equals(
                        findType == FindNodeType.ByCurrentCode ? r.getCurrentCode() : r.getFormat().getNodeName()
                ))
                .findFirst();
        if (ret.isPresent()) {
            return ret.get();
        }
        return null;
    }

    public MtNode getNode(String code) {
        return getNode(code, FindNodeType.ByCurrentCode);
    }


    @Override
    public String toString() {
        return "MtContent{" +
                "nodes=" + nodes +
                '}';
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private class DumpInfo {
        private String name;
        private String value;
        private int nodeOrder;
        private int blockOrder;
    }

    public String dumpValues() {
        var ret = new ArrayList<DumpInfo>();
        for (var block : getBlocks()) {
            block.getValues().entrySet().stream().forEach(r ->
                    ret.add(new DumpInfo(
                            r.getKey().getCode(),
                            r.getValue(),
                            block.getOwnerNode().getOrder(),
                            block.getId()
                    )));
        }
        var nl$ = System.lineSeparator();
        StringBuilder sb = new StringBuilder("Value:" + nl$);
        ret.stream()
                .sorted(Comparator.comparing(DumpInfo::getNodeOrder)
                        .thenComparing(DumpInfo::getBlockOrder)
                        .thenComparing(DumpInfo::getName)).forEach(r -> {
                    sb.append(
                            String.format("%-20s\t%3d\t%s%n", r.getName(), r.getBlockOrder(), r.getValue()));
                    //r.getName()+"\t"+r.getBlockOrder()+"\t"+r.getValue()+nl$);
                });

        return sb.toString();
    }
}
