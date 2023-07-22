package ru.igorit.andrk.mt.structure;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ru.igorit.andrk.config.services.ConfigFormatException;
import ru.igorit.andrk.mt.utils.MtParser;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"code"})
public class MtItem {
    private final String code;
    private boolean required;
    private boolean strongLength;
    private MtItemType type;
    private int length;
    private String formatString;

    public static MtItem parse(String format) {
        if (format == null || format.length() == 0) {
            throw new ConfigFormatException(String.format("Обнаружен пустой формат", format));
        }
        var fields = format.split(":");
        if (fields.length < 5) {
            throw new ConfigFormatException(String.format("Некорректный формат {%s}", format));
        }
        var ret = new MtItem(fields[0]);
        ret.setRequired(fields[1].equals("1"));
        ret.setStrongLength(fields[2].equals("1"));
        ret.setType(MtParser.getItemTypes().get(fields[3]));
        ret.setLength(Integer.parseInt(fields[4]));
        if (fields.length > 5) {
            ret.setFormatString(fields[5]);
        }
        return ret;
    }

    public Object extractValue(String value) {
        if (value == null) {
            return null;
        }
        if (type == MtItemType.DATE) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
            try {
                LocalDateTime ret = LocalDateTime.parse(value, formatter);
                return ret;
            } catch (DateTimeParseException e) {
                if (e.getCause() != null && e.getCause() instanceof DateTimeException) {
                    LocalDateTime ret = LocalDate.parse(value, formatter).atStartOfDay();
                    return ret;
                } else {
                    throw (e);
                }


            }
        } else if (type == MtItemType.INTEGER) {
            Integer ret = Integer.parseInt(value);
            return ret;
        }
        return value;
    }

    public String formatValue(Object value){
        return this.formatValue(value, true);
    }

    public String formatValue(Object value, boolean checkRequired){
        String ret = "";
        if (value==null) {
            if (this.isRequired() && checkRequired){
                throw new IllegalArgumentException("Пустое значение обязательного элемента "+this.code);
            }
            return "";
        }
        if (this.type==MtItemType.STRING || this.type==MtItemType.INTEGER){
            ret = value.toString();
            if (this.strongLength){
                if (this.type==MtItemType.STRING){
                    ret = StringUtils.rightPad(ret,this.length,' ');
                } else if (this.type == MtItemType.INTEGER) {
                    ret = StringUtils.leftPad(ret,this.length, "0");
                }
            }
        } else if (this.type == MtItemType.DATE) {
            var formatter = DateTimeFormatter.ofPattern(this.formatString);
            if (value.getClass() == LocalDateTime.class){
                ret = ((LocalDateTime)value).format(formatter);
            } else if (value.getClass() == LocalDate.class) {
                ret = ((LocalDate)value).format(formatter);
            }
        }
        if (ret.length() > this.length){
            throw new IllegalArgumentException("Превышена длина элемента "+this.code);
        }
        return ret;
    }

    @Override
    public String toString() {
        return "MtItem{" +
                "code='" + code + '\'' +
                ", required=" + required +
                ", strongLength=" + strongLength +
                ", type=" + type +
                ", length=" + length +
                ", formatString='" + formatString + '\'' +
                '}';
    }
}
