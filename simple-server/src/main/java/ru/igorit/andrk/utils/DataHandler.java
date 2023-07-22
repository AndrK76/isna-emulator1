package ru.igorit.andrk.utils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.GregorianCalendar;

public class DataHandler {
    public static XMLGregorianCalendar toXmlDate(LocalDateTime date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(Timestamp.valueOf(date));
        try {
            XMLGregorianCalendar result = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            return result;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static XMLGregorianCalendar toXmlDate(OffsetDateTime date) {
        return toXmlDate(date.toLocalDateTime());
    }

    public static OffsetDateTime toTimeWithTZ(XMLGregorianCalendar xmlDate) {
        var gcDate = xmlDate.toGregorianCalendar();
        var zdt = gcDate.toZonedDateTime();
        var instant = zdt.toInstant() ;
        var ofDate = instant.atOffset(zdt.getOffset());
        return ofDate;
    }
}
