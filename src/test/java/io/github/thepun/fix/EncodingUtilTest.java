package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingUtilTest {

    private Cursor cursor;

    @BeforeEach
    void prepare() {
        ByteBuf buffer = Unpooled.directBuffer(1024);
        cursor = new Cursor();
        cursor.setTemp(new byte[1024]);
        DecodingUtil.startDecoding(cursor, buffer);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 45, 10, 99, 100, 456, 999})
    void encodeTag(int tag) {
        cursor.setTag(tag);
        EncodingUtil.encodeTag(cursor);
        assertEquals(tag + "=", readString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9, 10, 345, Integer.MAX_VALUE, -1, -9, -10, -457, Integer.MIN_VALUE + 1})
    void encodeInt(int value) {
        cursor.setIntValue(value);
        EncodingUtil.encodeIntValue(cursor);
        assertEquals(value + "|", readString());
    }

    // TODO: identify min/max possible values
    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 9, 10, 67678, -0, -1, -9, -10, -4567, 0.1, 2.1, 1.0, 99.12, -0.1, -2.1, -1.0, -99.12,
            1000000000, 1000000000.0000000001, -1000000000, -1000000000.0000000001})
    void encodeDouble(double value) {
        NumberFormat format = new DecimalFormat("0.0#########");
        cursor.setDoubleValue(value);
        EncodingUtil.encodeDoubleValue(cursor);
        assertEquals(format.format(value) + "|", readString());
    }

    // TODO: add some more special characters
    @ParameterizedTest
    @ValueSource(strings = {"", "asdrty_sdf", "a", "qwertyuiopasdfghjklzxcvbQWERRYUIOPASDFGHJKLZXCVBNM", "_`@#$%^&@!^&*(){}1,.\\';"})
    void encodeString(String value) {
        ByteBuf string = Unpooled.directBuffer();
        string.writeCharSequence(value, CharsetUtil.US_ASCII);
        cursor.setStrStart(string.memoryAddress() + string.readerIndex());
        cursor.setStrLength(string.readableBytes());
        EncodingUtil.encodeStringNativeValue(cursor);
        assertEquals(value + "|", readString());
    }

    private String readString() {
        ByteBuf buffer = cursor.getBuffer();
        buffer.writerIndex(cursor.getIndex());
        return buffer.readCharSequence(buffer.readableBytes(), CharsetUtil.US_ASCII).toString().replace((char) 1, '|');
    }
}
