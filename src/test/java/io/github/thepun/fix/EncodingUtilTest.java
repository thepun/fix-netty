package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingUtilTest {

    private Cursor cursor;

    @BeforeEach
    void prepare() {
        byte[] temp = new byte[1024];
        ByteBuf buffer = Unpooled.directBuffer(1024);
        cursor = new Cursor();
        cursor.setTemp(new byte[1024]);
        CommonCodecUtil.startDecoding(cursor, buffer, temp);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 45, 10, 99, 100, 456, 999})
    void encodeTag(int tag) {
        cursor.setTag(tag);
        CommonCodecUtil.encodeTag(cursor);
        assertEquals(tag + "=", FixHelper.readString(cursor));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 9, 10, 345, Integer.MAX_VALUE, -1, -9, -10, -457, Integer.MIN_VALUE + 1})
    void encodeInt(int value) {
        cursor.setIntValue(value);
        CommonCodecUtil.encodeIntValue(cursor);
        assertEquals(value + "|", FixHelper.readString(cursor));
    }

    // TODO: identify min/max possible values
    @Disabled
    @ParameterizedTest
    @ValueSource(doubles = {0, 1, 9, 10, 67678, -0, -1, -9, -10, -4567, 0.1, 2.1, 1.0, 99.12, -0.1, -2.1, -1.0, -99.12,
            1000000000, 1000000000.0000000001, -1000000000, -1000000000.0000000001})
    void encodeDouble(double value) {
        NumberFormat format = new DecimalFormat("0.0#########");
        cursor.setDoubleValue(value);
        CommonCodecUtil.encodeDoubleValue(cursor);
        assertEquals(format.format(value) + "|", FixHelper.readString(cursor));
    }

    // TODO: add some more special characters
    @ParameterizedTest
    @ValueSource(strings = {"", "asdrty_sdf", "a", "qwertyuiopasdfghjklzxcvbQWERRYUIOPASDFGHJKLZXCVBNM", "_`@#$%^&@!^&*(){}1,.\\';"})
    void encodeNativeString(String value) {
        ByteBuf string = Unpooled.directBuffer();
        string.writeCharSequence(value, CharsetUtil.US_ASCII);
        cursor.setStrStart(string.memoryAddress() + string.readerIndex());
        cursor.setStrLength(string.readableBytes());
        CommonCodecUtil.encodeStringNativeValue(cursor);
        assertEquals(value + "|", FixHelper.readString(cursor));
    }

    @Test
    void encodeMarketDataRequest() {
        MarketDataRequest marketDataRequest = new MarketDataRequest();
        marketDataRequest.setMdReqId("asdfghrty");
        marketDataRequest.setMarketDepth(99);
        marketDataRequest.setRelatedSymCount(3);
        marketDataRequest.setStreamReference("stream1");
        marketDataRequest.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE);
        marketDataRequest.getRelatedSym(0).setSymbol("EURUSD");
        marketDataRequest.getRelatedSym(1).setSymbol("EURCAD_");
        marketDataRequest.getRelatedSym(2).setSymbol("XYZ");

        PrimeXmCodecUtil.encodeMarketDataRequest(cursor, marketDataRequest);
        assertEquals("262=asdfghrty|263=1|264=99|146=3|55=EURUSD|55=EURCAD_|55=XYZ|", FixHelper.readString(cursor));
    }

}
