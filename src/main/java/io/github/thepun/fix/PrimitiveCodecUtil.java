package io.github.thepun.fix;

import io.github.thepun.unsafe.OffHeapMemory;
import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.util.CharsetUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// TODO: try to use byte processors to iterate over bytes
// TODO: remove count checks
// TODO: add overflow protections
// TODO: inline cursor
final class PrimitiveCodecUtil {

    private static final ZoneId GMT = ZoneId.of("GMT");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("YYYYMMdd-HH:mm:ss.SSS");

    private static final int MAX_INT_DIGITS = 10;
    private static final int MAX_INT_DIGITS_PLUS_ONE = MAX_INT_DIGITS + 1;
    private static final int MAX_INT_DIGITS_PLUS_TWO = MAX_INT_DIGITS + 2;
    private static final String DUMMY_DATE_STRING = "20180511-11:24:33.855";
    private static final int DUMMY_DATE_STRING_LENGTH = 21;

    static final byte DELIMITER = 1;
    static final byte EQUAL_SIGN = (byte) '=';
    static final byte BOOLEAN_TRUE_VALUE = (int) 'Y';
    static final byte BOOLEAN_FALSE_VALUE = (int) 'N';
    static final byte MINUS_SIGN = (byte) '-';
    static final byte DOT_SIGN = (byte) '.';
    static final byte DIGIT_OFFSET = (byte) '0';
    static final byte FLOAT_PART_SIGN = (byte) '.';
    static final int EQUAL_SIGN_SHIFTED = EQUAL_SIGN << 24;

    static void ensureTag(int tag, int expectedTag) {
        if (tag != expectedTag) {
            throw new DecoderException("Expected tag " + expectedTag);
        }
    }

    static int skipTag(ByteBuf in, int index) {
        long address = in.memoryAddress();

        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == EQUAL_SIGN) {
                index++;
                break;
            }
        }

        return index;
    }

    static int skipValue(ByteBuf in, int index) {
        long address = in.memoryAddress();

        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte =  OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }
        }
        return index;
    }

    static int skipUntilChecksum(ByteBuf in, int index, Value value) {
        int tag;

        for (;;) {
            index = decodeTag(in, index, value);
            tag = value.getIntValue();

            switch (tag) {
                case FixFields.CHECK_SUM:
                    return skipValue(in, index);

                default:
                    index = skipValue(in, index);
            }
        }
    }

    static int decodeTag(ByteBuf in, int index, Value value) {
        long address = in.memoryAddress();

        int tagNum = 0;
        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == EQUAL_SIGN) {
                index++;
                break;
            }

            tagNum = tagNum * 10 + (nextByte - DIGIT_OFFSET);
        }

        value.setIntValue(tagNum);
        return index;
    }

    static int decodeIntValue(ByteBuf in, int index, Value valueStore) {
        long address = in.memoryAddress();

        int value = 0;
        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            value = value * 10 + (nextByte - DIGIT_OFFSET);
        }

        valueStore.setIntValue(value);
        return index;
    }

    static int decodeBooleanValue(ByteBuf in, int index, Value valueStore) {
        byte value = in.getByte(index);
        valueStore.setBooleanValue(value == BOOLEAN_TRUE_VALUE);

        index += 2;
        return index;
    }

    static int decodeDoubleValue(ByteBuf in, int index, Value valueStore) {
        long address = in.memoryAddress();
        long addressWithIndex;

        // calculate integer part
        int intValue = 0;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == FLOAT_PART_SIGN) {
                index++;
                break;
            } else if (nextByte == DELIMITER) {
                valueStore.setDoubleValue(intValue);
                return index;
            }

            intValue = intValue * 10 + (nextByte - DIGIT_OFFSET);
        }

        // calculate floating part
        int floatValue = 0;
        int floatSizePowered = 1;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == 1) {
                index++;
                break;
            }

            floatValue = floatValue * 10 + (nextByte - DIGIT_OFFSET);
            floatSizePowered = floatSizePowered * 10;
        }

        // value = int + float
        double value = intValue + floatValue / (double) floatSizePowered;
        valueStore.setDoubleValue(value);
        return index;
    }

    static int decodeStringValue(ByteBuf in, int index, byte[] temp, Value value) {
        long address = in.memoryAddress();

        int length = 0;
        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            temp[length] = nextByte;
            length++;
        }

        String strValue = new String(temp, 0, length, CharsetUtil.US_ASCII);
        value.setStrValue(strValue);
        return index;
    }

    static int decodeStringNativeValue(ByteBuf in, int index, OffHeapCharSequence strValue) {
        long address = in.memoryAddress();
        long start = address + index;

        int length = 0;
        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            length++;
        }

        strValue.setAddress(start, length);
        return index;
    }

    static int decodeStringValueAsInt(ByteBuf in, int index, Value valueStore) {
        long address = in.memoryAddress();

        int value = 0;
        long addressWithIndex;
        for (; ; index++) {
            addressWithIndex = address + index;

            byte nextByte = OffHeapMemory.getByte(addressWithIndex);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            value = (value << 8) + nextByte;
        }

        valueStore.setIntValue(value);
        return index;
    }

    static int encodeEqualSign(ByteBuf out, int index) {
        out.setByte(index++, EQUAL_SIGN);
        return index;
    }

    static int encodeDelimiter(ByteBuf out, int index) {
        out.setByte(index++, DELIMITER);
        return index;
    }

    static int encodeThreeDigitInt(ByteBuf out, int index, int value) {
        // three digits
        int low = value % 10;
        int high = value / 100;
        int mid = value / 10 - high * 10;
        long address = out.memoryAddress() + index;
        OffHeapMemory.setByte(address, (byte) (high + DIGIT_OFFSET));
        OffHeapMemory.setByte(address + 1, (byte) (mid + DIGIT_OFFSET));
        OffHeapMemory.setByte(address + 2, (byte) (low + DIGIT_OFFSET));

        /*byte low = (byte)(value % 10);
        byte high = (byte)(value / 100);
        int mid = value / 10 - high * 10;
        int val = (short)(((low + DIGIT_OFFSET) << 24) | ((mid + DIGIT_OFFSET) << 16) | ((high + DIGIT_OFFSET) << 8) | ());*/

        return index + 3;
    }

    // expected tag to be less than 1000 and greater then 0
    static int encodeTag(ByteBuf out, int index, int tag) {
        long address = out.memoryAddress() + index;

        if (tag < 10) {
            // one digit
            OffHeapMemory.setByte(address, (byte) (tag + DIGIT_OFFSET));
            OffHeapMemory.setByte(address + 1, EQUAL_SIGN);
            return index + 2;
        } else if (tag < 100) {
            // two digits
           /* int low = tag % 10;
            int high = tag / 10;
            long address = out.memoryAddress() + index;
            OffHeapMemory.setByte(address, (byte) (high + DIGIT_OFFSET));
            OffHeapMemory.setByte(address + 1, (byte) (low + DIGIT_OFFSET));
            */

            byte low = (byte)(tag % 10);
            byte high = (byte)(tag / 10);
            short val = (short)(((low + DIGIT_OFFSET) << 8) | (high + DIGIT_OFFSET));
            OffHeapMemory.setShort(address, val);
            OffHeapMemory.setByte(address + 2, EQUAL_SIGN);
            return index + 3;
        } else {

            byte low = (byte)(tag % 10);
            byte high = (byte)(tag / 100);
            byte mid = (byte)(tag / 10 - high * 10);
            int val = (EQUAL_SIGN_SHIFTED | ((low + DIGIT_OFFSET) << 16) | ((mid + DIGIT_OFFSET) << 8) | (high + DIGIT_OFFSET));

            OffHeapMemory.setInt(address, val);
            return index + 4;
        }
    }

    // TODO: optimize
    // value from Integer.MIN_VALUE + 1 to Integer.MAX_VALUE
    static int encodeIntValue(ByteBuf out, int index, byte[] temp, int value) {
        long address = out.memoryAddress() + index;

        int digit;
        int pos = MAX_INT_DIGITS_PLUS_ONE;

        // check for sign
        boolean sign = false;
        if (value < 0) {
            temp[0] = MINUS_SIGN;
            sign = true;
            value = -value;
        }

        // read digits to temp
        do {
            digit = value % 10 + DIGIT_OFFSET;
            value = value / 10;
            temp[--pos] = (byte) digit;
        } while (value != 0);

        // add sign
        if (sign) {
            temp[--pos] = MINUS_SIGN;
        }

        // set delimiter
        temp[MAX_INT_DIGITS_PLUS_ONE] = DELIMITER;

        // copy temp to buffer
        int length = MAX_INT_DIGITS_PLUS_TWO - pos;
        OffHeapMemory.copyFromArray(address, temp, pos, length);
        index += length;

        return index;
    }

    static int encodeBooleanValue(ByteBuf out, int index, boolean value) {
        byte b = value ? BOOLEAN_TRUE_VALUE : BOOLEAN_FALSE_VALUE;
        out.setByte(index++, b);

        index = encodeDelimiter(out, index);
        return index;
    }

    // TODO: optimize
    static int encodeDoubleValue(ByteBuf out, int index, byte[] temp, double value) {
        long address = out.memoryAddress() + index;

        int digit;
        int pos;

        // check for sign
        boolean sign = false;
        if (value < 0) {
            temp[0] = MINUS_SIGN;
            sign = true;
            value = -value;
        }

        // convert integer part
        pos = MAX_INT_DIGITS_PLUS_ONE;
        int intPart = (int) value;
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
        double floatPart = value - ((int) value);
        do {
            floatPart = floatPart * 10;
            digit = ((int) floatPart) + DIGIT_OFFSET;
            temp[pos++] = (byte) digit;
        } while (floatPart != 0);

        // set delimiter
        temp[pos] = DELIMITER;

        // copy temp to buffer
        int length = pos - intPartPos + 1;
        OffHeapMemory.copyFromArray(address, temp, intPartPos, length);
        index += length;

        return index;
    }

    static int encodeStringValue(ByteBuf out, int index, String value) {
        // copy text to buffer by characters
        out.setCharSequence(index, value, CharsetUtil.US_ASCII);
        index = value.length() + index;

        // set delimiter
        out.setByte(index++, DELIMITER);
        return index;
    }

    static int encodeStringNativeValue(ByteBuf out, int index, OffHeapCharSequence value) {
        // copy text to buffer
        long addressTo = out.memoryAddress() + index;
        long addressFrom = value.getOffheapAddress();
        int length = value.getOffheapLength();
        OffHeapMemory.copy(addressFrom, addressTo, length);
        OffHeapMemory.setByte(addressTo + length, DELIMITER);
        index += length + 1;
        return index;
    }

    // TODO: optimize
    static int encodeDateTime(ByteBuf out, int index, long dateTime, StringBuilder sb) {
/*        DATE_TIME.formatTo(Instant.ofEpochMilli(dateTime).atZone(GMT), sb);
        index += out.setCharSequence(index, sb, CharsetUtil.US_ASCII);
        sb.setLength(0);*/
        out.setCharSequence(index, DUMMY_DATE_STRING, CharsetUtil.US_ASCII);
        index += DUMMY_DATE_STRING_LENGTH;
        // set delimiter
        index = encodeDelimiter(out, index);
        return index;
    }

}
