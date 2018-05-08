package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

class CommonCodecUtilTest {

    /*@Test
    void decodeTag() {
        String fix = "2233=";

        Cursor cursor = prepareCursor(fix);
        CommonCodecUtil.decodeTag(cursor);
        assertEquals(2233, cursor.getTag());
    }*/

    /*@Disabled
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9, 10, 345, Integer.MAX_VALUE, -1, -9, -10, - 4356, Integer.MIN_VALUE})
    void decodeInt(int value) {
        Cursor cursor = prepareCursor(value + "|");
        CommonCodecUtil.decodeIntValue(cursor);
        assertEquals(value, cursor.getIntValue());
    }*/

    // TODO: identify min/max possible values
    /*@Disabled
    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 9, 10, 234, -0, -1, -9, -10, -234, 0.1, 2.1, 1.0, 45.34, -0.1, -2.1, -1.0, -45.34,
            1000000000, 1000000000.0000000001, -1000000000, -1000000000.0000000001})
    void decodeDouble(double value) {
        NumberFormat format = new DecimalFormat("0.0#########");
        format.setMaximumFractionDigits(10);
        String fix = format.format(value) + "|";

        Cursor cursor = prepareCursor(fix);
        CommonCodecUtil.decodeDoubleValue(cursor);
        assertEquals(value, cursor.getDoubleValue());
    }*/

    // TODO: add some more special characters
    /*@ParameterizedTest
    @ValueSource(strings = {"", "asdrty_sdf", "a", "qwertyuiopasdfghjklzxcvbQWERRYUIOPASDFGHJKLZXCVBNM", "_`@#$%^&@!^&*(){}1,.\\';"})
    void decodeString(String value) {
        String fix = value + "|";

        Cursor cursor = prepareCursor(fix);
        CommonCodecUtil.decodeNativeStringValue(cursor);
        OffHeapCharSequence str = new OffHeapCharSequence(cursor.getStrStart(), cursor.getStrLength());
        assertEquals(value, str.toString());
    }*/

   /* @Test
    void decodeLogon() {
        String fix = "98=1|108=30|141=Y|553=name_q|554=password_q|";

        Logon logon = new Logon();
        CommonCodecUtil.decodeLogon(prepareCursor(fix), logon);

        assertTrue(logon.isResetSqNumFlag());
        assertEquals(1, logon.getEncryptMethod());
        assertEquals(30, logon.getHeartbeatInterval());
        assertEquals("name_q", logon.getUsername());
        assertEquals("password_q", logon.getPassword());
    }*/

    /*@Test
    void decodeLogout() {
        String fix = "58=dfgewrttyucgvcvbsd wertxcg dsfge|";

        Logout logout = new Logout();
        CommonCodecUtil.decodeLogout(prepareCursor(fix), logout);

        assertEquals("dfgewrttyucgvcvbsd wertxcg dsfge", logout.getText());
    }*/

    /*@ParameterizedTest
    @ValueSource(ints = {1, 3, 45, 10, 99, 100, 456, 999})
    void encodeTag(int tag) {
        cursor.setTag(tag);
        CommonCodecUtil.encodeTag(cursor);
        assertEquals(tag + "=", FixHelper.readString(cursor));
    }*/

    /*@ParameterizedTest
    @ValueSource(ints = {0, 1, 9, 10, 345, Integer.MAX_VALUE, -1, -9, -10, -457, Integer.MIN_VALUE + 1})
    void encodeInt(int value) {
        cursor.setIntValue(value);
        CommonCodecUtil.encodeIntValue(cursor);
        assertEquals(value + "|", FixHelper.readString(cursor));
    }*/

    // TODO: identify min/max possible values
    /*@Disabled
    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 9, 10, 67678, -0, -1, -9, -10, -4567, 0.1, 2.1, 1.0, 99.12, -0.1, -2.1, -1.0, -99.12,
            1000000000, 1000000000.0000000001, -1000000000, -1000000000.0000000001})
    void encodeDouble(double value) {
        NumberFormat format = new DecimalFormat("0.0#########");
        cursor.setDoubleValue(value);
        CommonCodecUtil.encodeDoubleValue(cursor);
        assertEquals(format.format(value) + "|", FixHelper.readString(cursor));
    }*/

    // TODO: add some more special characters
    /*@ParameterizedTest
    @ValueSource(strings = {"", "asdrty_sdf", "a", "qwertyuiopasdfghjklzxcvbQWERRYUIOPASDFGHJKLZXCVBNM", "_`@#$%^&@!^&*(){}1,.\\';"})
    void encodeNativeString(String value) {
        ByteBuf string = Unpooled.directBuffer();
        string.writeCharSequence(value, CharsetUtil.US_ASCII);
        cursor.setStrStart(string.memoryAddress() + string.readerIndex());
        cursor.setStrLength(string.readableBytes());
        CommonCodecUtil.encodeStringNativeValue(cursor);
        assertEquals(value + "|", FixHelper.readString(cursor));
    }*/




    /*private static Cursor prepareCursor(String fix) {
        byte[] temp = new byte[1024];
        ByteBuf buffer = Unpooled.directBuffer(fix.length());
        buffer.writeBytes(fix.replace('|', (char) 1).getBytes(CharsetUtil.US_ASCII));
        Cursor cursor = new Cursor();
        CommonCodecUtil.startDecoding(cursor, buffer, temp);
        return cursor;
    }*/
}
