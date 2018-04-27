package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

final class FixDecodingUtils {

    private static final int DELIMITER = 1;
    private static final int EQUAL_SIGN = (int) '=';
    private static final int DIGIT_OFFSET = (int) '0';
    private static final int FLOAT_PART_SIGN = (int) '.';

    static void ensureTag(FixDecodingCursor cursor, int tag) {
        if (cursor.getTag() != tag) {
            throw new DecoderException("Expected tag " + tag);
        }
    }

    static void skipTagAndValue(FixDecodingCursor cursor) {
        ByteBuf in = cursor.getIn();
        int count = cursor.getCount();
        int index = cursor.getIndex();

        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }
        }

        cursor.setIndex(index);
    }

    static void decodeTag(FixDecodingCursor cursor) {
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

    static void decodeIntValue(FixDecodingCursor cursor) {
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

    static void decodeDoubleValue(FixDecodingCursor cursor) {
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

    static void decodeStrValue(FixDecodingCursor cursor) {
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

    static void decodeStrValueAsInt(FixDecodingCursor cursor) {
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

    static void decodeLogon(FixDecodingCursor cursor, Logon logon) {

    }

    static void decodeLogout(FixDecodingCursor cursor, Logout logout) {

    }

    static void decodeMarketDataSnapshotFullRefresh(FixDecodingCursor cursor, MarketDataSnapshotFullRefresh message) {
        // req id
        decodeTag(cursor);
        ensureTag(cursor, Fields.MD_REQ_ID);
        FixDecodingUtils.decodeStrValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, Fields.SYMBOL);
        FixDecodingUtils.decodeStrValue(cursor);
        message.getSymbol().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // count of MD entries
        decodeTag(cursor);
        ensureTag(cursor, Fields.NO_MD_ENTRIES);
        decodeIntValue(cursor);
        int mdEntriesCount = cursor.getIntValue();
        message.setEntryCount(mdEntriesCount);

        // MD entry loop
        for (int i = 0; i < mdEntriesCount; i++) {
            MarketDataSnapshotFullRefresh.MDEntryGroup entry = message.getEntry(i);

            // type
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_TYPE);
            decodeIntValue(cursor);
            entry.setMdEntryType(cursor.getIntValue());

            // id
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_ID);
            FixDecodingUtils.decodeStrValue(cursor);
            entry.getId().setAddress(cursor.getStrStart(), cursor.getStrLength());

            // price
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_PX);
            FixDecodingUtils.decodeDoubleValue(cursor);
            entry.setMdEntryPX(cursor.getDoubleValue());

            // volume
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_SIZE);
            FixDecodingUtils.decodeDoubleValue(cursor);
            entry.setMdEntrySize(cursor.getDoubleValue());
        }
    }

    static void decodeMassQuote(FixDecodingCursor cursor, MassQuote message) {
        // req id
        decodeTag(cursor);
        ensureTag(cursor, Fields.MD_REQ_ID);
        FixDecodingUtils.decodeStrValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, Fields.SYMBOL);
        FixDecodingUtils.decodeStrValue(cursor);
        message.getSymbol().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // count of MD entries
        decodeTag(cursor);
        ensureTag(cursor, Fields.NO_MD_ENTRIES);
        decodeIntValue(cursor);
        int mdEntriesCount = cursor.getIntValue();
        message.setEntryCount(mdEntriesCount);

        // MD entry loop
        for (int i = 0; i < mdEntriesCount; i++) {
            MarketDataSnapshotFullRefresh.MDEntryGroup entry = message.getEntry(i);

            // type
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_TYPE);
            decodeIntValue(cursor);
            entry.setMdEntryType(cursor.getIntValue());

            // id
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_ID);
            FixDecodingUtils.decodeStrValue(cursor);
            entry.getId().setAddress(cursor.getStrStart(), cursor.getStrLength());

            // price
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_PX);
            FixDecodingUtils.decodeDoubleValue(cursor);
            entry.setMdEntryPX(cursor.getDoubleValue());

            // volume
            decodeTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_SIZE);
            FixDecodingUtils.decodeDoubleValue(cursor);
            entry.setMdEntrySize(cursor.getDoubleValue());
        }
    }

    static void decodeMarketDataReject(FixDecodingCursor cursor, MarketDataRequestReject message) {
        // req id
        decodeTag(cursor);
        ensureTag(cursor, Fields.MD_REQ_ID);
        FixDecodingUtils.decodeStrValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, Fields.TEXT);
        FixDecodingUtils.decodeStrValue(cursor);
        message.getText().setAddress(cursor.getStrStart(), cursor.getStrLength());
    }
}
