package io.github.thepun.fix;

import io.github.thepun.unsafe.OffHeapMemory;
import io.netty.buffer.ByteBuf;

import java.math.BigDecimal;

// TODO: add overflow protections
// TODO: inline cursor
final class EncodingUtil {

    private static final byte DELIMITER = 1;
    private static final int MAX_INT_DIGITS = 10;
    private static final int MAX_INT_DIGITS_PLUS_ONE = MAX_INT_DIGITS + 1;
    private static final int MAX_INT_DIGITS_PLUS_TWO = MAX_INT_DIGITS + 2;
    private static final int EQUAL_SIGN = (int) '=';
    private static final byte MINUS_SIGN = (byte) '-';
    private static final byte DOT_SIGN = (byte) '.';
    private static final int DIGIT_OFFSET = (int) '0';

    static void startEncoding(Cursor cursor, ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        cursor.setBuffer(buffer);
        cursor.setIndex(readerIndex);
        //cursor.setBefore(readerIndex);
        cursor.setPoint(buffer.readableBytes());
        cursor.setNativeAddress(buffer.memoryAddress());
    }

    static int encodeDelimiter(ByteBuf out, int index) {
        out.setByte(index++, EQUAL_SIGN);
        return index;
    }

    static int encodeThreeDigitInt(ByteBuf out, int index, int value) {
        // three digits
        int low = value % 10;
        int high = value / 100;
        int mid = value / 10 - high * 10;
        out.setByte(index++, high + DIGIT_OFFSET);
        out.setByte(index++, mid + DIGIT_OFFSET);
        out.setByte(index++, low + DIGIT_OFFSET);
        return index;
    }

    // expected tag to be less than 1000 and greater then 0
    static void encodeTag(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        int index = cursor.getIndex();

        int tagNum = cursor.getTag();
        if (tagNum < 10) {
            // one digit
            out.setByte(index++, tagNum + DIGIT_OFFSET);
        } else if (tagNum < 100) {
            // two digits
            int low = tagNum % 10;
            int high = tagNum / 10;
            out.setByte(index++, high + DIGIT_OFFSET);
            out.setByte(index++, low + DIGIT_OFFSET);
        } else {
            index = encodeThreeDigitInt(out, index, tagNum);
        }

        index = encodeDelimiter(out, index);

        cursor.setIndex(index);
    }

    // TODO: optimize
    // value from Integer.MIN_VALUE + 1 to Integer.MAX_VALUE
    static void encodeIntValue(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        byte[] temp = cursor.getTemp();
        int index = cursor.getIndex();

        int digit;
        int pos = MAX_INT_DIGITS_PLUS_ONE;
        int intValue = cursor.getIntValue();

        // check for sign
        boolean sign = false;
        if (intValue < 0) {
            temp[0] = MINUS_SIGN;
            sign = true;
            intValue = -intValue;
        }

        // read digits to temp
        do {
            digit = intValue % 10 + DIGIT_OFFSET;
            intValue = intValue / 10;
            temp[--pos] = (byte) digit;
        } while (intValue != 0);

        // add sign
        if (sign) {
            temp[--pos] = MINUS_SIGN;
        }

        // set delimeter
        temp[MAX_INT_DIGITS_PLUS_ONE] = DELIMITER;

        // copy temp to buffer
        int length = MAX_INT_DIGITS_PLUS_TWO - pos;
        out.setBytes(index, temp, pos, length);
        index += length;

        cursor.setIndex(index);
    }

    // TODO: optimize
    static void encodeDoubleValue(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        byte[] temp = cursor.getTemp();
        int index = cursor.getIndex();

        int digit;
        int pos;
        double doubleValue = cursor.getDoubleValue();

        // check for sign
        boolean sign = false;
        if (doubleValue < 0) {
            temp[0] = MINUS_SIGN;
            sign = true;
            doubleValue = -doubleValue;
        }

        // convert integer part
        pos = MAX_INT_DIGITS_PLUS_ONE;
        int intPart = (int) doubleValue;
        do {
            digit = intPart % 10 + DIGIT_OFFSET;
            intPart = intPart / 10;
            temp[--pos] = (byte) digit;
        } while (intPart != 0);

        // write dot
        temp[MAX_INT_DIGITS_PLUS_ONE] = DOT_SIGN;

        // convert floating part
        int intPartPos = pos;
        pos = MAX_INT_DIGITS_PLUS_TWO;
        double floatPart = doubleValue - ((int) doubleValue);
        do {
            floatPart = floatPart * 10;
            digit = ((int) floatPart) + DIGIT_OFFSET;
            temp[pos++] = (byte) digit;
        } while (floatPart != 0);

        // set delimiter
        temp[pos] = DELIMITER;

        // copy temp to buffer
        int length = pos - intPartPos + 1;
        out.setBytes(index, temp, intPartPos, length);
        index += length;

        cursor.setIndex(index);
    }

    static void encodeStringValue(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        int index = cursor.getIndex();

        // TODO: do encoding

        /*// copy text to buffer
        long addressTo = cursor.getNativeAddress() + index;
        long addressFrom = cursor.getStrStart();
        int length = cursor.getStrLength();
        OffHeapMemory.copy(addressFrom, addressTo, length);
        index += length;

        // set delimiter
        out.setByte(index++, DELIMITER);*/

        cursor.setIndex(index);
    }

    static void encodeStringNativeValue(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        int index = cursor.getIndex();

        // copy text to buffer
        long addressTo = cursor.getNativeAddress() + index;
        long addressFrom = cursor.getStrStart();
        int length = cursor.getStrLength();
        OffHeapMemory.copy(addressFrom, addressTo, length);
        index += length;

        // set delimiter
        out.setByte(index++, DELIMITER);

        cursor.setIndex(index);
    }

    static void encodeStringValueAsInt(Cursor cursor) {
        int index = cursor.getIndex();

        // copy text to buffer
        long addressTo = cursor.getNativeAddress() + index;
        long addressFrom = cursor.getStrStart();
        int length = cursor.getStrLength();
        OffHeapMemory.copy(addressFrom, addressTo, length);

        cursor.setIndex(index + length);
    }

    static void encodeLogon(Cursor cursor, Logon message) {
        // TODO: implement logon encoding
    }

    static void encodeLogout(Cursor cursor, Logout message) {
        // TODO: implement logout encoding
    }

    static void encodeMassQuote(Cursor cursor, MassQuote message) {
        // TODO: implement mass quote encoding
    }

    static void encodeMarketDataRequest(Cursor cursor, MarketDataRequest message) {
        // md req id
        cursor.setTag(FixFields.MD_REQ_ID);
        cursor.setStrValue(message.getMdReqId());
        EncodingUtil.encodeTag(cursor);
        EncodingUtil.encodeStringValue(cursor);

        // subscription request type
        cursor.setTag(FixFields.SUBSCRIPTION_REQUEST_TYPE);
        cursor.setIntValue(message.getSubscriptionRequestType());
        EncodingUtil.encodeTag(cursor);
        EncodingUtil.encodeIntValue(cursor);

        // market depth
        cursor.setTag(FixFields.MARKET_DEPTH);
        cursor.setIntValue(message.getMarketDepth());
        EncodingUtil.encodeTag(cursor);
        EncodingUtil.encodeIntValue(cursor);

        // no related sym
        cursor.setTag(FixFields.NO_RELATED_SYM);
        cursor.setIntValue(message.getRelatedSymsCount());
        EncodingUtil.encodeTag(cursor);
        EncodingUtil.encodeIntValue(cursor);

        // related sym loop
        for (int i = 0; i < message.getRelatedSymsCount(); i++) {
            MarketDataRequest.RelatedSymGroup relatedSym = message.getRelatedSym(i);

            // symbol
            cursor.setTag(FixFields.SYMBOL);
            cursor.setStrValue(relatedSym.getSymbol());
            EncodingUtil.encodeTag(cursor);
            EncodingUtil.encodeStringValue(cursor);
        }
    }

    static void encodeMarketDataRequestReject(Cursor cursor, MarketDataRequestReject message) {
        // TODO: implement market data request reject encoding
    }
}
