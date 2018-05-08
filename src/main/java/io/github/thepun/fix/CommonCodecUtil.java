package io.github.thepun.fix;

import io.github.thepun.unsafe.OffHeapMemory;
import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.util.CharsetUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// TODO: remove count checks
// TODO: add overflow protections
// TODO: inline cursor
final class CommonCodecUtil {

    private static final ZoneId GMT = ZoneId.of("GMT");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("YYYYMMdd-HH:mm:ss.SSS");

    private static final int MAX_INT_DIGITS = 10;
    private static final int MAX_INT_DIGITS_PLUS_ONE = MAX_INT_DIGITS + 1;
    private static final int MAX_INT_DIGITS_PLUS_TWO = MAX_INT_DIGITS + 2;

    static final byte DELIMITER = 1;
    static final byte EQUAL_SIGN = (byte) '=';
    static final byte BOOLEAN_TRUE_VALUE = (int) 'Y';
    static final byte BOOLEAN_FALSE_VALUE = (int) 'N';
    static final byte MINUS_SIGN = (byte) '-';
    static final byte DOT_SIGN = (byte) '.';
    static final byte DIGIT_OFFSET = (byte) '0';
    static final byte FLOAT_PART_SIGN = (byte) '.';

    static void ensureTag(int tag, int expectedTag) {
        if (tag != expectedTag) {
            throw new DecoderException("Expected tag " + expectedTag);
        }
    }

    static int skipTag(ByteBuf in, int index) {
        for (; ; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == EQUAL_SIGN) {
                index++;
                break;
            }
        }

        return index;
    }

    static int skipValue(ByteBuf in, int index) {
        for (; ; index++) {
            byte nextByte = in.getByte(index);
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
        int tagNum = 0;
        for (; ; index++) {
            byte nextByte = in.getByte(index);
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
        int value = 0;
        for (; ; index++) {
            byte nextByte = in.getByte(index);
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
        // calculate integer part
        int intValue = 0;
        for (; ; index++) {
            byte nextByte = in.getByte(index);
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
            byte nextByte = in.getByte(index);
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
        int length = 0;
        for (; ; index++) {
            byte nextByte = in.getByte(index);
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

    static int decodeNativeStringValue(ByteBuf in, int index, OffHeapCharSequence strValue) {
        long start = in.memoryAddress() + index;

        int length = 0;
        for (; ; index++) {
            byte nextByte = in.getByte(index);
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
        int value = 0;
        for (; ; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            value = (value << 8) + nextByte;
        }

        valueStore.setIntValue(value);
        return index;
    }

    static int decodeLogon(ByteBuf in, int index, byte[] temp, Value value, Logon logon) {
        int tag;

        for (;;) {
            index = decodeTag(in, index, value);
            tag = value.getIntValue();

            switch (tag) {
                case FixFields.ENCRYPT_METHOD:
                    index = decodeIntValue(in, index, value);
                    logon.setEncryptMethod(value.getIntValue());
                    break;

                case FixFields.HEART_BT_INT:
                    index = decodeIntValue(in, index, value);
                    logon.setHeartbeatInterval(value.getIntValue());
                    break;

                case FixFields.RESET_SEQ_NUM_FLAG:
                    index = decodeBooleanValue(in, index, value);
                    logon.setResetSqNumFlag(value.getBooleanValue());
                    break;

                case FixFields.USERNAME:
                    index = decodeStringValue(in, index, temp, value);
                    logon.setUsername(value.getStrValue());
                    break;

                case FixFields.PASSWORD:
                    index = decodeStringValue(in, index, temp, value);
                    logon.setPassword(value.getStrValue());
                    break;

                case FixFields.CHECK_SUM:
                    return skipValue(in, index);

                default:
                    index = skipValue(in, index);
            }
        }
    }

    static int decodeLogout(ByteBuf in, int index, byte[] temp, Value value, Logout logout) {
        int tag;

        do {
            index = decodeTag(in, index, value);
            tag = value.getIntValue();

            switch (tag) {
                case FixFields.TEXT:
                    decodeStringValue(in, index, temp, value);
                    logout.setText(value.getStrValue());
                    break;

                default:
                    index = skipValue(in, index);
            }
        } while (tag != FixFields.CHECK_SUM);

        return skipValue(in, index);
    }

    static int decodeHeartbeat(ByteBuf in, int index, Heartbeat message) {
        // TODO: implements decoding of heartbeat
        return index;
    }

    static int decodeTest(ByteBuf in, int index, Test message) {
        // TODO: implements decoding of test
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
        out.setByte(index++, high + DIGIT_OFFSET);
        out.setByte(index++, mid + DIGIT_OFFSET);
        out.setByte(index++, low + DIGIT_OFFSET);
        return index;
    }

    // expected tag to be less than 1000 and greater then 0
    static int encodeTag(ByteBuf out, int index, int tag) {
        if (tag < 10) {
            // one digit
            out.setByte(index++, tag + DIGIT_OFFSET);
        } else if (tag < 100) {
            // two digits
            int low = tag % 10;
            int high = tag / 10;
            out.setByte(index++, high + DIGIT_OFFSET);
            out.setByte(index++, low + DIGIT_OFFSET);
        } else {
            index = encodeThreeDigitInt(out, index, tag);
        }

        index = encodeEqualSign(out, index);
        return index;
    }

    // TODO: optimize
    // value from Integer.MIN_VALUE + 1 to Integer.MAX_VALUE
    static int encodeIntValue(ByteBuf out, int index, byte[] temp, int value) {
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
        out.setBytes(index, temp, pos, length);
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
        out.setBytes(index, temp, intPartPos, length);
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
        index += length;

        // set delimiter
        index = encodeDelimiter(out, index);
        return index;
    }

    // TODO: optimize
    static int encodeDateTime(ByteBuf out, int index, long dateTime, StringBuilder sb) {
        DATE_TIME.formatTo(Instant.ofEpochMilli(dateTime).atZone(GMT), sb);
        index += out.setCharSequence(index, sb, CharsetUtil.US_ASCII);
        sb.setLength(0);

        // set delimiter
        index = encodeDelimiter(out, index);
        return index;
    }

    static int encodeLogon(ByteBuf out, int index, byte[] temp, Logon message) {
        // encrypt method
        index = encodeTag(out, index, FixFields.ENCRYPT_METHOD);
        index = encodeIntValue(out, index, temp, message.getEncryptMethod());

        // heartbeat interval
        index = encodeTag(out, index, FixFields.HEART_BT_INT);
        index = encodeIntValue(out, index, temp, message.getHeartbeatInterval());

        // reset seq num flag
        index = encodeTag(out, index, FixFields.RESET_SEQ_NUM_FLAG);
        index = encodeBooleanValue(out, index, message.isResetSqNumFlag());

        // username
        if (message.getUsername() != null) {
            index = encodeTag(out, index, FixFields.USERNAME);
            index = encodeStringValue(out, index, message.getUsername());
        }

        // password
        if (message.getPassword() != null) {
            index = encodeTag(out, index, FixFields.PASSWORD);
            index = encodeStringValue(out, index, message.getPassword());
        }

        return index;
    }

    static int encodeLogout(ByteBuf out, int index, Logout message) {
        index = encodeTag(out, index, FixFields.TEXT);
        index = encodeStringValue(out, index, message.getText());
        return index;
    }

    static int encodeHeartbeat(ByteBuf out, int index, Heartbeat message) {
        // TODO: implements decoding of heartbeat
        return index;
    }

    static int encodeTest(ByteBuf out, int index, Test message) {
        // TODO: implements decoding of test
        return index;
    }
}
