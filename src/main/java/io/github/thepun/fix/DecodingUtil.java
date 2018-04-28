package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

final class DecodingUtil {

    private static final int DELIMITER = 1;
    private static final int EQUAL_SIGN = (int) '=';
    private static final int DIGIT_OFFSET = (int) '0';
    private static final int FLOAT_PART_SIGN = (int) '.';

    static void start(DecodingCursor cursor, ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        cursor.setBuffer(buffer);
        cursor.setIndex(readerIndex);
        //cursor.setBefore(readerIndex);
        cursor.setCount(buffer.readableBytes());
        cursor.setNativeAddress(buffer.memoryAddress());
    }

    static void ensureTag(DecodingCursor cursor, int tag) {
        if (cursor.getTag() != tag) {
            throw new DecoderException("Expected tag " + tag);
        }
    }

    static void decodeTagAndSkipHeader(DecodingCursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getCount();
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

    static void decodeTag(DecodingCursor cursor) {
        ByteBuf in = cursor.getBuffer();
        int count = cursor.getCount();
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

    static void decodeIntValue(DecodingCursor cursor) {
        ByteBuf in = cursor.getBuffer();
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

    static void decodeDoubleValue(DecodingCursor cursor) {
        ByteBuf in = cursor.getBuffer();
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
        double value = intValue + floatValue / (double) floatSizePowered;
        cursor.setIndex(index);
        cursor.setDoubleValue(value);
    }

    static void decodeStrValue(DecodingCursor cursor) {
        ByteBuf in = cursor.getBuffer();
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

    static void decodeStrValueAsInt(DecodingCursor cursor) {
        ByteBuf in = cursor.getBuffer();
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

    static void decodeLogon(DecodingCursor cursor, Logon logon) {

    }

    static void decodeLogout(DecodingCursor cursor, Logout logout) {

    }

    static void decodeMarketDataSnapshotFullRefresh(DecodingCursor cursor, MarketDataSnapshotFullRefresh message) {
        message.initBuffer(cursor.getBuffer());

        // req id
        decodeTag(cursor);
        ensureTag(cursor, FixFields.MD_REQ_ID);
        decodeStrValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, FixFields.SYMBOL);
        decodeStrValue(cursor);
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
            decodeStrValue(cursor);
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

    static void decodeMassQuote(DecodingCursor cursor, MassQuote message) {
        message.initBuffer(cursor.getBuffer());

        // one tag in future
        decodeTag(cursor);

        // optional quote id
        int tag = cursor.getTag();
        if (tag == FixFields.QUOTE_ID) {
            decodeStrValue(cursor);
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
            decodeStrValue(cursor);
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
                decodeStrValue(cursor);
                entry.getQuoteEntryId().setAddress(cursor.getStrStart(), cursor.getStrLength());

                // one tag in future
                decodeTag(cursor);

                // optional issuer
                tag = cursor.getTag();
                if (tag == FixFields.ISSUER) {
                    decodeStrValue(cursor);
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

    static void decodeMarketDataReject(DecodingCursor cursor, MarketDataRequestReject message) {
        // req id
        decodeTag(cursor);
        ensureTag(cursor, FixFields.MD_REQ_ID);
        decodeStrValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        decodeTag(cursor);
        ensureTag(cursor, FixFields.TEXT);
        decodeStrValue(cursor);
        message.getText().setAddress(cursor.getStrStart(), cursor.getStrLength());
    }
}
