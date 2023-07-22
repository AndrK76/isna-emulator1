package ru.igorit.andrk.mt.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class MtBlock {

    private final int id;
    private final String text;

    private final MtNode ownerNode;

    private final Map<MtItem, String> values = new HashMap<>();

    private MtBlockFormat blockFormat;

    public void setItem(MtItem item, Object value) {
        if (item != null) {
            String formattedVal = item.formatValue(value, blockFormat==null || blockFormat.containItem(item));
            values.put(item, formattedVal);
        }
    }

    public MtItem getItem(String itemCode) {
        return getValues().keySet().stream()
                .filter(r -> itemCode.equals(r.getCode()))
                .findFirst().orElse(null);
    }


    @Override
    public String toString() {
        return "MtBlock{" +
                "id=" + id +
                ", Text='" + text + '\'' +
                '}';
    }
}
