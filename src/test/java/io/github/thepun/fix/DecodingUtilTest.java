package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

class DecodingUtilTest {

    @Test
    void decodeTag() {
        String fix = "2233=";

        Cursor cursor = prepareCursor(fix);
        DecodingUtil.decodeTag(cursor);
        assertEquals(2233, cursor.getTag());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9, 10, 345, Integer.MAX_VALUE, -1, -9, -10, - 4356, Integer.MIN_VALUE})
    void decodeInt(int value) {
        Cursor cursor = prepareCursor(value + "|");
        DecodingUtil.decodeIntValue(cursor);
        assertEquals(value, cursor.getIntValue());
    }

    // TODO: identify min/max possible values
    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 9, 10, 234, -0, -1, -9, -10, -234, 0.1, 2.1, 1.0, 45.34, -0.1, -2.1, -1.0, -45.34,
            1000000000, 1000000000.0000000001, -1000000000, -1000000000.0000000001})
    void decodeDouble(double value) {
        NumberFormat format = new DecimalFormat("0.0#########");
        format.setMaximumFractionDigits(10);
        String fix = format.format(value) + "|";

        Cursor cursor = prepareCursor(fix);
        DecodingUtil.decodeDoubleValue(cursor);
        assertEquals(value, cursor.getDoubleValue());
    }

    // TODO: add some more special characters
    @ParameterizedTest
    @ValueSource(strings = {"", "asdrty_sdf", "a", "qwertyuiopasdfghjklzxcvbQWERRYUIOPASDFGHJKLZXCVBNM", "_`@#$%^&@!^&*(){}1,.\\';"})
    void decodeString(String value) {
        String fix = value + "|";

        Cursor cursor = prepareCursor(fix);
        DecodingUtil.decodeNativeStringValue(cursor);
        OffHeapCharSequence str = new OffHeapCharSequence(cursor.getStrStart(), cursor.getStrLength());
        assertEquals(value, str.toString());
    }

    @Test
    void decodeLogon() {
        String fix = "98=1|108=30|141=Y|553=name_q|554=password_q|";

        Logon logon = new Logon();
        DecodingUtil.decodeLogon(prepareCursor(fix), logon);

        assertTrue(logon.isResetSqNumFlag());
        assertEquals(1, logon.getEncryptMethod());
        assertEquals(30, logon.getHeartbeatInterval());
        assertEquals("name_q", logon.getUsername());
        assertEquals("password_q", logon.getPassword());
    }

    @Test
    void decodeLogout() {
        String fix = "58=dfgewrttyucgvcvbsd wertxcg dsfge|";

        Logout logout = new Logout();
        DecodingUtil.decodeLogout(prepareCursor(fix), logout);

        assertEquals("dfgewrttyucgvcvbsd wertxcg dsfge", logout.getText());
    }

    @Test
    void decodeMassQuote() {
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

    @Test
    void decodeMarketDataRequest() {

    }

    @Test
    void decodeMarketDataRequestReject() {

    }

    private static Cursor prepareCursor(String fix) {
        byte[] temp = new byte[1024];
        ByteBuf buffer = Unpooled.directBuffer(fix.length());
        buffer.writeBytes(fix.replace('|', (char) 1).getBytes(CharsetUtil.US_ASCII));
        Cursor cursor = new Cursor();
        DecodingUtil.startDecoding(cursor, buffer, temp);
        return cursor;
    }
}
