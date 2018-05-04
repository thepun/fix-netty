package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.util.CharsetUtil;

// TODO: add overflow protections
// TODO: inline cursor
final class DecodingUtil {

    private static final int DELIMITER = 1;
    private static final int EQUAL_SIGN = (int) '=';
    private static final int DIGIT_OFFSET = (int) '0';
    private static final byte BOOLEAN_TRUE_VALUE = (int) 'Y';
    private static final int FLOAT_PART_SIGN = (int) '.';

    static void startDecoding(Cursor cursor, ByteBuf buffer, byte[] temp) {
        int readerIndex = buffer.readerIndex();
        cursor.setBuffer(buffer);
        cursor.setIndex(readerIndex);
        //cursor.setBefore(readerIndex);
        cursor.setPoint(buffer.readableBytes());
        cursor.setNativeAddress(buffer.memoryAddress());
        cursor.setTemp(temp);
    }

    static void ensureTag(Cursor cursor, int tag) {
        if (cursor.getTag() != tag) {
            throw new DecoderException("Expected tag " + tag);
        }
    }

    static void decodeTagAndSkipHeader(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
        int index = cursor.getIndex();

        int tagNum = 0;

        for (;;) {
            // read tag
            for (; index < count; index++) {
                byte nextByte = in.getByte(index);
                if (nextByte == EQUAL_SIGN) {
                    index++;
                    break;
                }

                tagNum = tagNum * 10 + (nextByte - DIGIT_OFFSET);
            }

            // if one of headers continue skipping
            if (tagNum == FixFields.SENDER_COMP_ID ||
                    tagNum == FixFields.TARGET_COMP_ID ||
                    tagNum == FixFields.SENDER_SUB_ID ||
                    tagNum == FixFields.TARGET_SUB_ID ||
                    tagNum == FixFields.MSG_SEQ_NUM ||
                    tagNum == FixFields.SENDING_TIME) {
                continue;
            }

            cursor.setTag(tagNum);
            cursor.setIndex(index);
            break;
        }
    }

    /*static void revert(DecodingCursor cursor) {
        int before = cursor.getBefore();
        cursor.setIndex(before);
    }*/

    static void decodeTag(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
        int index = cursor.getIndex();
        //cursor.setBefore(index);

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

    static void decodeIntValue(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
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

    static void decodeBooleanValue(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int index = cursor.getIndex();

        byte value = in.getByte(index);
        cursor.setBooleanValue(value == BOOLEAN_TRUE_VALUE);

        index += 2;
        cursor.setIndex(index);
    }

    static void decodeDoubleValue(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
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
        double value = intValue + floatValue / (double) floatSizePowered;
        cursor.setIndex(index);
        cursor.setDoubleValue(value);
    }

    static void decodeStringValue(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
        int index = cursor.getIndex();
        byte[] temp = cursor.getTemp();

        int length = 0;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == DELIMITER) {
                index++;
                break;
            }

            temp[length] = nextByte;
            length++;
        }

        cursor.setStrValue(new String(temp, 0, length, CharsetUtil.US_ASCII));
        cursor.setIndex(index);
    }

    static void decodeNativeStringValue(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
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

    static void decodeStringValueAsInt(Cursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getPoint();
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

        cursor.setIntValue(value);
        cursor.setIndex(index);
    }

    static void decodeLogon(Cursor cursor, Logon logon) {
        // encrypt method
        decodeTag(cursor);
        ensureTag(cursor, FixFields.ENCRYPT_METHOD);
        decodeIntValue(cursor);
        logon.setEncryptMethod(cursor.getIntValue());

        // heart beat interval
        decodeTag(cursor);
        ensureTag(cursor, FixFields.HEART_BT_INT);
        decodeIntValue(cursor);
        logon.setHeartbeatInterval(cursor.getIntValue());

        // reset sequence number flag
        decodeTag(cursor);
        ensureTag(cursor, FixFields.RESET_SEQ_NUM_FLAG);
        decodeBooleanValue(cursor);
        logon.setResetSqNumFlag(cursor.getBooleanValue());

        // username
        decodeTag(cursor);
        ensureTag(cursor, FixFields.USERNAME);
        decodeStringValue(cursor);
        logon.setUsername(cursor.getStrValue());

        // password
        decodeTag(cursor);
        ensureTag(cursor, FixFields.PASSWORD);
        decodeStringValue(cursor);
        logon.setPassword(cursor.getStrValue());
    }

    static void decodeLogout(Cursor cursor, Logout logout) {
        // password
        decodeTag(cursor);
        ensureTag(cursor, FixFields.TEXT);
        decodeStringValue(cursor);
        logout.setText(cursor.getStrValue());
    }

    static void decodeMarketDataSnapshotFullRefresh(Cursor cursor, MarketDataSnapshotFullRefresh message) {
        message.initBuffer(cursor.getBuffer());

        // req id
        decodeTag(cursor);
        ensureTag(cursor, FixFields.MD_REQ_ID);
        decodeNativeStringValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, FixFields.SYMBOL);
        decodeNativeStringValue(cursor);
        message.getSymbol().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // count of MD entries
        decodeTag(cursor);
        ensureTag(cursor, FixFields.NO_MD_ENTRIES);
        decodeIntValue(cursor);
        int mdEntriesCount = cursor.getIntValue();
        message.initEntries(mdEntriesCount);

        // MD entry loop
        for (int i = 0; i < mdEntriesCount; i++) {
            MarketDataSnapshotFullRefresh.MDEntry entry = message.getEntry(i);

            // type
            decodeTag(cursor);
            ensureTag(cursor, FixFields.MD_ENTRY_TYPE);
            decodeIntValue(cursor);
            entry.setMdEntryType(cursor.getIntValue());

            // id
            decodeTag(cursor);
            ensureTag(cursor, FixFields.MD_ENTRY_ID);
            decodeNativeStringValue(cursor);
            entry.getId().setAddress(cursor.getStrStart(), cursor.getStrLength());

            // price
            decodeTag(cursor);
            ensureTag(cursor, FixFields.MD_ENTRY_PX);
            decodeDoubleValue(cursor);
            entry.setMdEntryPX(cursor.getDoubleValue());

            // volume
            decodeTag(cursor);
            ensureTag(cursor, FixFields.MD_ENTRY_SIZE);
            decodeDoubleValue(cursor);
            entry.setMdEntrySize(cursor.getDoubleValue());
        }
    }

    static void decodeMassQuote(Cursor cursor, MassQuote message) {
        message.initBuffer(cursor.getBuffer());

        // one tag in future
        decodeTag(cursor);

        // optional quote id
        int tag = cursor.getTag();
        if (tag == FixFields.QUOTE_ID) {
            decodeNativeStringValue(cursor);
            message.getQuoteId().setAddress(cursor.getStrStart(), cursor.getStrLength());
            message.setQuoteIdDefined(true);

            decodeTag(cursor);
        }

        // count of quote sets
        ensureTag(cursor, FixFields.NO_QUOTE_SETS);
        decodeIntValue(cursor);
        int quoteSetsCount = cursor.getIntValue();
        message.initQuoteSets(quoteSetsCount);

        // we going to decode one tag in future and
        // after each loop iteration we will have first tag already decoded
        decodeTag(cursor);

        // quote set loop
        for (int i = 0; i < quoteSetsCount; i++) {
            MassQuote.QuoteSet quoteSet = message.getQuoteSet(i);

            // quote set id
            ensureTag(cursor, FixFields.QUOTE_SET_ID);
            decodeNativeStringValue(cursor);
            quoteSet.getQuoteSetId().setAddress(cursor.getStrStart(), cursor.getStrLength());

            // count of quote entries
            decodeTag(cursor);
            ensureTag(cursor, FixFields.NO_QUOTE_ENTRIES);
            decodeIntValue(cursor);
            int quoteEntriesCount = cursor.getIntValue();
            quoteSet.initEntries(quoteEntriesCount);

            // quote entry loop
            for (int j = 0; j < quoteEntriesCount; j++) {
                MassQuote.QuoteEntry entry = quoteSet.getEntry(j);

                // quote entry id
                decodeTag(cursor);
                ensureTag(cursor, FixFields.QUOTE_ENTRY_ID);
                decodeNativeStringValue(cursor);
                entry.getQuoteEntryId().setAddress(cursor.getStrStart(), cursor.getStrLength());

                // one tag in future
                decodeTag(cursor);

                // optional issuer
                tag = cursor.getTag();
                if (tag == FixFields.ISSUER) {
                    decodeNativeStringValue(cursor);
                    entry.getIssuer().setAddress(cursor.getStrStart(), cursor.getStrLength());
                    entry.setIssuerIsDefined(true);

                    decodeTag(cursor);
                    tag = cursor.getTag();
                }

                // values
                entryTags:
                for (;;) {
                    switch (tag) {
                        case FixFields.BID_SIZE:
                            decodeDoubleValue(cursor);
                            entry.setBidSize(cursor.getDoubleValue());
                            break;

                        case FixFields.BID_SPOT_RATE:
                            decodeDoubleValue(cursor);
                            entry.setBidSpotRate(cursor.getDoubleValue());
                            break;

                        case FixFields.OFFER_SIZE:
                            decodeDoubleValue(cursor);
                            entry.setOfferSize(cursor.getDoubleValue());
                            break;

                        case FixFields.OFFER_SPOT_RATE:
                            decodeDoubleValue(cursor);
                            entry.setOfferSpotRate(cursor.getDoubleValue());
                            break;

                        default:
                            break entryTags;
                    }

                    decodeTag(cursor);
                    tag = cursor.getTag();
                }
            }
        }
    }

    static void decodeMarketDataRequest(Cursor cursor, MarketDataRequest message) {
       // TODO: implements decoding of market data request
    }

    static void decodeMarketDataRequestReject(Cursor cursor, MarketDataRequestReject message) {
        // req id
        decodeTag(cursor);
        ensureTag(cursor, FixFields.MD_REQ_ID);
        decodeNativeStringValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, FixFields.TEXT);
        decodeNativeStringValue(cursor);
        message.getText().setAddress(cursor.getStrStart(), cursor.getStrLength());
    }

    static void decodeHeartbeat(Cursor cursor, Heartbeat message) {
        // TODO: implements decoding of heartbeat
    }

    static void decodeTest(Cursor cursor, Test message) {
        // TODO: implements decoding of test
    }
}
