package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DateTimeCopyConvertTest {

    private static final LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
    private static final Date DATE = Date.from(LOCAL_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant());

    @Test
    void convertDateToLocalDateTime() {
        DateSource source = new DateSource();
        source.setValue(DATE);

        LocalDateTimeTarget target = ConvertUtil.convert(source, LocalDateTimeTarget.class);
        assertEquals(LOCAL_DATE_TIME, target.getValue());
    }

    @Test
    void convertLocalDateTimeToDate() {
        LocalDateTimeSource source = new LocalDateTimeSource();
        source.setValue(LOCAL_DATE_TIME);

        DateTarget target = ConvertUtil.convert(source, DateTarget.class);
        assertEquals(DATE, target.getValue());
    }

    @Test
    void convertTimestampToLocalDateTime() {
        DateSource source = new DateSource();
        source.setValue(Timestamp.valueOf(LOCAL_DATE_TIME));

        LocalDateTimeTarget target = ConvertUtil.convert(source, LocalDateTimeTarget.class);
        assertEquals(LOCAL_DATE_TIME, target.getValue());
    }

    @Test
    void convertDateToLocalDate() {
        DateSource source = new DateSource();
        source.setValue(DATE);

        LocalDateTarget target = ConvertUtil.convert(source, LocalDateTarget.class);
        assertEquals(LOCAL_DATE_TIME.toLocalDate(), target.getValue());
    }

    @Test
    void convertLocalDateToDate() {
        LocalDateSource source = new LocalDateSource();
        source.setValue(LOCAL_DATE_TIME.toLocalDate());

        DateTarget target = ConvertUtil.convert(source, DateTarget.class);
        Date expected = Date.from(LOCAL_DATE_TIME.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertEquals(expected, target.getValue());
    }

    @Test
    void convertDateToInstant() {
        DateSource source = new DateSource();
        source.setValue(DATE);

        InstantTarget target = ConvertUtil.convert(source, InstantTarget.class);
        assertEquals(DATE.toInstant(), target.getValue());
    }

    @Test
    void convertInstantToLocalDateTime() {
        InstantSource source = new InstantSource();
        source.setValue(DATE.toInstant());

        LocalDateTimeTarget target = ConvertUtil.convert(source, LocalDateTimeTarget.class);
        assertEquals(LOCAL_DATE_TIME, target.getValue());
    }

    @Test
    void convertNullDateSkipped() {
        DateSource source = new DateSource();
        source.setValue(null);

        LocalDateTimeTarget target = ConvertUtil.convert(source, LocalDateTimeTarget.class);
        assertNull(target.getValue());
    }

    @Getter
    @Setter
    static class DateSource {
        private Date value;
    }

    @Getter
    @Setter
    static class LocalDateTimeSource {
        private LocalDateTime value;
    }

    @Getter
    @Setter
    static class LocalDateSource {
        private LocalDate value;
    }

    @Getter
    @Setter
    static class InstantSource {
        private Instant value;
    }

    @Getter
    @Setter
    static class DateTarget {
        private Date value;
    }

    @Getter
    @Setter
    static class LocalDateTimeTarget {
        private LocalDateTime value;
    }

    @Getter
    @Setter
    static class LocalDateTarget {
        private LocalDate value;
    }

    @Getter
    @Setter
    static class InstantTarget {
        private Instant value;
    }
}
