package ru.igorit.andrk.mt.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MtBlockFormat {
    private final String splitter;
    private final String formatString;

    public boolean containItem(MtItem item){
        return this.formatString.contains("{"+item.getCode()+":");
    }


    @Override
    public String toString() {
        return "MtBlockFormat{" +
                "splitter='" + splitter + '\'' +
                ", formatString='" + formatString + '\'' +
                '}';
    }
}
