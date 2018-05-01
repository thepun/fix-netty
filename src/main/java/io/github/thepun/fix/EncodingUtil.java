package io.github.thepun.fix;

import io.github.thepun.unsafe.OffHeapMemory;
import io.netty.buffer.ByteBuf;

// TODO: inline cursor
final class EncodingUtil {

    private static final int MAX_INT_DIGITS = 10;
    private static final int EQUAL_SIGN = (int) '=';
    private static final byte MINUS_SIGN = (byte) '-';
    private static final int DIGIT_OFFSET = (int) '0';

    static void start(Cursor cursor, ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        cursor.setBuffer(buffer);
        cursor.setIndex(readerIndex);
        //cursor.setBefore(readerIndex);
        cursor.setPoint(buffer.readableBytes());
        cursor.setNativeAddress(buffer.memoryAddress());
    }

    // expected tag to be less than 1000
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
            // three digits
            int low = tagNum % 10;
            int high = tagNum / 100;
            int mid = tagNum / 10 - high;
            out.setByte(index++, high + DIGIT_OFFSET);
            out.setByte(index++, mid + DIGIT_OFFSET);
            out.setByte(index++, low + DIGIT_OFFSET);
        }

        out.setByte(index++, EQUAL_SIGN);

        cursor.setIndex(index);
    }

    // TODO: optimize
    static void encodeIntValue(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        byte[] temp = cursor.getTemp();
        int index = cursor.getIndex();

        int digit;
        int pos = MAX_INT_DIGITS;
        int intValue = cursor.getIntValue();

        // check for sign
        boolean sign = false;
        if (intValue < 0) {
            temp[0] = MINUS_SIGN;
            sign = true;
            intValue = -intValue;
        }

        // read digits to temp
        while (intValue != 0) {
            digit = intValue % 10 + DIGIT_OFFSET;
            intValue = intValue / 10;
            temp[pos--] = (byte) digit;
        }

        // add sign
        if (sign) {
            temp[pos--] = MINUS_SIGN;
        }

        // copy temp to buffer
        int length = MAX_INT_DIGITS - pos;
        out.setBytes(index, temp, pos, length);
        index += length;

        cursor.setIndex(index);
    }

    static void encodeStringValue(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        int index = cursor.getIndex();

        // copy text to buffer
        long addressTo = cursor.getNativeAddress() + index;
        long addressFrom = cursor.getStrStart();
        int length = cursor.getStrLength();
        OffHeapMemory.copy(addressFrom, addressTo, length);

        cursor.setIndex(index);
    }
}
