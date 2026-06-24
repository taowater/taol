package com.taowater.core.convert;

import com.taowater.taol.core.convert.ConvertUtil;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharCopyConvertTest {

    private static final char CHAR_VAL = 'A';

    @Test
    void convertCharPrimitiveToPrimitive() {
        CharPrimitiveSource source = new CharPrimitiveSource();
        source.setValue(CHAR_VAL);

        CharPrimitiveTarget target = ConvertUtil.convert(source, CharPrimitiveTarget.class);
        assertEquals(CHAR_VAL, target.getValue());
    }

    @Test
    void convertCharPrimitiveToWrapper() {
        CharPrimitiveSource source = new CharPrimitiveSource();
        source.setValue(CHAR_VAL);

        CharWrapperTarget target = ConvertUtil.convert(source, CharWrapperTarget.class);
        assertEquals(Character.valueOf(CHAR_VAL), target.getValue());
    }

    @Test
    void convertCharWrapperToPrimitive() {
        CharWrapperSource source = new CharWrapperSource();
        source.setValue(CHAR_VAL);

        CharPrimitiveTarget target = ConvertUtil.convert(source, CharPrimitiveTarget.class);
        assertEquals(CHAR_VAL, target.getValue());
    }

    @Test
    void convertCharWrapperToWrapper() {
        CharWrapperSource source = new CharWrapperSource();
        source.setValue(CHAR_VAL);

        CharWrapperTarget target = ConvertUtil.convert(source, CharWrapperTarget.class);
        assertEquals(Character.valueOf(CHAR_VAL), target.getValue());
    }

    @Getter
    @Setter
    static class CharPrimitiveSource {
        private char value;
    }

    @Getter
    @Setter
    static class CharWrapperSource {
        private Character value;
    }

    @Getter
    @Setter
    static class CharPrimitiveTarget {
        private char value;
    }

    @Getter
    @Setter
    static class CharWrapperTarget {
        private Character value;
    }
}
