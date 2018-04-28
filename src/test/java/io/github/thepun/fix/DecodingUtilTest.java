package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecodingUtilTest {

    @Test
    void decodeTag() {
        String fix = "2233=sdf|";

        DecodingCursor cursor = prepareCursor(fix);
        DecodingUtil.decodeTag(cursor);

        assertEquals(2233, cursor.getTag());
    }

    @Test
    void decodeInt() {
        String fix = "5567=45678|";

        DecodingCursor cursor = prepareCursor(fix);
        DecodingUtil.decodeTag(cursor);
        DecodingUtil.decodeIntValue(cursor);

        assertEquals(45678, cursor.getIntValue());
    }

    @Test
    void decodeString() {
        String fix = "123=asdrty_sdf|";

        DecodingCursor cursor = prepareCursor(fix);
        DecodingUtil.decodeTag(cursor);
        DecodingUtil.decodeStrValue(cursor);

        OffHeapCharSequence str = new OffHeapCharSequence(cursor.getStrStart(), cursor.getStrLength());
        assertEquals("asdrty_sdf", str.toString());
    }

    @Test
    void decodeDoubleWithFloating() {
        String fix = "567=123.012|";

        DecodingCursor cursor = prepareCursor(fix);
        DecodingUtil.decodeTag(cursor);
        DecodingUtil.decodeDoubleValue(cursor);

        assertEquals(123.012, cursor.getDoubleValue());
    }

    @Test
    void decodeDoubleWithoutFloating() {
        String fix = "111=778|";

        DecodingCursor cursor = prepareCursor(fix);
        DecodingUtil.decodeTag(cursor);
        DecodingUtil.decodeDoubleValue(cursor);

        assertEquals(778, cursor.getDoubleValue());
    }

    @Test
    void massQuote() {
        String fix = "296=1|302=43|295=1|299=0|106=1|134=1000000|135=50000|188=186.129|190=186.14|299=1|10=132|";

        MassQuote massQuote = MassQuote.newInstance();
        DecodingUtil.decodeMassQuote(prepareCursor(fix), massQuote);

        assertFalse(massQuote.isQuoteIdDefined());
        assertEquals(1, massQuote.getQuoteSetCount());

        MassQuote.QuoteSet quoteSet = massQuote.getQuoteSet(0);
        assertNotNull(quoteSet);
        assertEquals(1, quoteSet.getEntryCount());
        assertEquals("43", quoteSet.getQuoteSetId().toString());

        MassQuote.QuoteEntry quoteSetEntry = quoteSet.getEntry(0);
        assertTrue(quoteSetEntry.isIssuerIsDefined());
        assertEquals("1", quoteSetEntry.getIssuer().toString());
        assertEquals("0", quoteSetEntry.getQuoteEntryId().toString());
        assertEquals(1000000, quoteSetEntry.getBidSize());
        assertEquals(186.129, quoteSetEntry.getBidSpotRate());
        assertEquals(50000, quoteSetEntry.getOfferSize());
        assertEquals(186.14, quoteSetEntry.getOfferSpotRate());
    }

    private static DecodingCursor prepareCursor(String fix) {
        ByteBuf buffer = Unpooled.directBuffer(fix.length());
        buffer.writeBytes(fix.replace('|', (char) 1).getBytes(CharsetUtil.US_ASCII));
        DecodingCursor cursor = new DecodingCursor();
        DecodingUtil.start(cursor, buffer);
        return cursor;
    }
}
