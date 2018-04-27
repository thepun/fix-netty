package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public final class FixParser {

    private static final int DELIMITER = 1;
    private static final int EQUAL_SIGN = (int) '=';
    private static final int DIGIT_OFFSET = (int) '0';
    private static final int FLOAT_PART_SIGN = (int) '.';

    public static void parseTag(FixParserCursor cursor) {
        ByteBuf in = cursor.getIn();
        int count = cursor.getCount();
        int index = cursor.getIndex();

        int tagNum = 0;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == EQUAL_SIGN) {
                index++;
                break;
            }

            tagNum = tagNum * 10 + (nextByte - DIGIT_OFFSET);
        }

        cursor.setTag(tagNum);
        cursor.setIndex(index);
    }

    public static void parseIntValue(FixParserCursor cursor) {
        ByteBuf in = cursor.getIn();
        int count = cursor.getCount();
        int index = cursor.getIndex();

        int value = 0;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            value = value * 10 + (nextByte - DIGIT_OFFSET);
        }

        cursor.setIntValue(value);
        cursor.setIndex(index);
    }

    public static void parseDoubleValue(FixParserCursor cursor) {
        ByteBuf in = cursor.getIn();
        int count = cursor.getCount();
        int index = cursor.getIndex();

        // calculate integer part
        int intValue = 0;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == FLOAT_PART_SIGN) {
                index++;
                break;
            } else if (nextByte == DELIMITER) {
                break;
            }

            intValue = intValue * 10 + (nextByte - DIGIT_OFFSET);
        }

        // calculate floating part
        int floatValue = 0;
        int floatSizePowered = 1;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == 1) {
                index++;
                break;
            }

            floatValue = floatValue * 10 + (nextByte - DIGIT_OFFSET);
            floatSizePowered = floatSizePowered * 10;
        }

        // value = int + float
        double value = intValue + floatValue / floatSizePowered;
        cursor.setDoubleValue(value);
        cursor.setIndex(index);
    }

    public static void parseStrValue(FixParserCursor cursor) {
        ByteBuf in = cursor.getIn();
        int count = cursor.getCount();
        int index = cursor.getIndex();
        long start = cursor.getNativeAddress() + index;

        int length = 0;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            length++;
        }

        cursor.setStrStart(start);
        cursor.setStrLength(length);
        cursor.setIndex(index);
    }

    public static void parseStrValueAsInt(FixParserCursor cursor) {
        ByteBuf in = cursor.getIn();
        int count = cursor.getCount();
        int index = cursor.getIndex();

        int value = 0;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            value = value << 1 + nextByte;
        }

        cursor.setStrAsInt(value);
        cursor.setIndex(index);
    }

    public static void skipTagAndValue(FixParserCursor cursor) {

    }

    public static void ensureTag(FixParserCursor cursor, int tag) {
        if (cursor.getTag() != tag) {
            throw new DecoderException("Expected tag " + tag);
        }
    }
}
