package ru.igorit.andrk.mt.structure;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"nodeName"})
public class MtFormatNodeInfo {
    private String nodeName;
    private int order;
    private String searchMask;
    private MtNodeCountMode countMode;

    @Override
    public String toString() {
        return "MtNodeInfo{" +
                "nodeName='" + nodeName + '\'' +
                ", searchMask='" + searchMask + '\'' +
                ", countMode=" + countMode +
                '}';
    }
}
