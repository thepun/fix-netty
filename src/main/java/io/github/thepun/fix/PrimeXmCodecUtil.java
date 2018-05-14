package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

import static io.github.thepun.fix.PrimitiveCodecUtil.*;

final class PrimeXmCodecUtil {

    // TODO: introduce fixed field order
    static int skipHeader(ByteBuf in, int index) {
        int lastIndex;
        int tagNum = 0;
        for (;;) {
            lastIndex = index;

            // read tag
            for (; ; index++) {
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
                // reset tag calculation
                tagNum = 0;

                // skip until next delimiter
                for (; ; index++) {
                    byte nextByte = in.getByte(index);
                    if (nextByte == DELIMITER) {
                        index++;
                        break;
                    }
                }
                continue;
            }

            return lastIndex;
        }
    }

    static int decodeMassQuote(ByteBuf in, int index, Value value, MassQuote message) {
        message.initBuffer(in);

        // one tag in future
        index = decodeTag(in, index, value);
        int tag = value.getIntValue();

        // optional quote id
        if (tag == FixFields.QUOTE_ID) {
            index = decodeStringNativeValue(in, index, message.getQuoteId());
            message.setQuoteIdDefined(true);

            index = decodeTag(in, index, value);
            tag = value.getIntValue();
        }

        // count of quote sets
        ensureTag(tag, FixFields.NO_QUOTE_SETS);
        index = decodeIntValue(in, index, value);
        int quoteSetsCount = value.getIntValue();
        message.initQuoteSets(quoteSetsCount);

        // we going to decode one tag in future and
        // after each loop iteration we will have first tag already decoded
        index = decodeTag(in, index, value);
        tag = value.getIntValue();

        // quote set loop
        for (int i = 0; i < quoteSetsCount; i++) {
            MassQuote.QuoteSet quoteSet = message.getQuoteSet(i);

            // quote set id
            ensureTag(tag, FixFields.QUOTE_SET_ID);
            index = decodeStringNativeValue(in, index, quoteSet.getQuoteSetId());
            quoteSet.setQuoteSetIdDefined(true);

            // count of quote entries
            index = decodeTag(in, index, value);
            tag = value.getIntValue();
            ensureTag(tag, FixFields.NO_QUOTE_ENTRIES);
            index = decodeIntValue(in, index, value);
            int quoteEntriesCount = value.getIntValue();
            quoteSet.initEntries(quoteEntriesCount);

            // one tag in future
            index = decodeTag(in, index, value);
            tag = value.getIntValue();

            // quote entry loop
            for (int j = 0; j < quoteEntriesCount; j++) {
                MassQuote.QuoteEntry entry = quoteSet.getEntry(j);

                // quote entry id
                ensureTag(tag, FixFields.QUOTE_ENTRY_ID);
                index = decodeStringNativeValue(in, index, entry.getQuoteEntryId());
                entry.setQuoteEntryIdDefined(true);

                // one tag in future
                index = decodeTag(in, index, value);
                tag = value.getIntValue();

                // optional issuer
                if (tag == FixFields.ISSUER) {
                    index = decodeStringNativeValue(in, index, entry.getIssuer());
                    entry.setIssuerDefined(true);

                    index = decodeTag(in, index, value);
                    tag = value.getIntValue();
                }

                // TODO: check if possible to do fixed field order in quote entry
                // values
                entryTags:
                for (;;) {
                    switch (tag) {
                        case FixFields.BID_SIZE:
                            index = decodeDoubleValue(in, index, value);
                            entry.setBidSize(value.getDoubleValue());
                            break;

                        case FixFields.BID_SPOT_RATE:
                            index = decodeDoubleValue(in, index, value);
                            entry.setBidSpotRate(value.getDoubleValue());
                            break;

                        case FixFields.OFFER_SIZE:
                            index = decodeDoubleValue(in, index, value);
                            entry.setOfferSize(value.getDoubleValue());
                            break;

                        case FixFields.OFFER_SPOT_RATE:
                            index = decodeDoubleValue(in, index, value);
                            entry.setOfferSpotRate(value.getDoubleValue());
                            break;

                        case FixFields.CHECK_SUM:
                        case FixFields.QUOTE_ENTRY_ID:
                            break entryTags;

                        default:
                            index = skipValue(in, index);
                    }

                    index = decodeTag(in, index, value);
                    tag = value.getIntValue();
                }
            }
        }

        return skipValue(in, index);
    }

    static int decodeMarketDataSnapshotFullRefresh(ByteBuf in, int index, Value value, MarketDataSnapshotFullRefresh message) {
        message.initBuffer(in);

        // symbol
        index = decodeTag(in, index, value);
        ensureTag(value.getIntValue(), FixFields.SYMBOL);
        index = decodeStringNativeValue(in, index, message.getSymbol());
        message.setSymbolDefined(true);

        // req id
        index = decodeTag(in, index, value);
        ensureTag(value.getIntValue(), FixFields.MD_REQ_ID);
        index = decodeStringNativeValue(in, index, message.getMdReqId());
        message.setMdReqIdDefined(true);

        // count of MD entries
        index = decodeTag(in, index, value);
        ensureTag(value.getIntValue(), FixFields.NO_MD_ENTRIES);
        index = decodeIntValue(in, index, value);
        int mdEntriesCount = value.getIntValue();
        message.initEntries(mdEntriesCount);

        // MD entry loop
        for (int i = 0; i < mdEntriesCount; i++) {
            MarketDataSnapshotFullRefresh.MDEntry entry = message.getEntry(i);
            entry.setIdDefined(true);
            entry.setIssuerDefined(true);
            entry.setSymbolDefined(false);
            entry.setCurrencyDefined(false);

            // type
            index = decodeTag(in, index, value);
            ensureTag(value.getIntValue(), FixFields.MD_ENTRY_TYPE);
            index = decodeIntValue(in, index, value);
            entry.setMdEntryType(value.getIntValue());

            // price
            index = decodeTag(in, index, value);
            ensureTag(value.getIntValue(), FixFields.MD_ENTRY_PX);
            index = decodeDoubleValue(in, index, value);
            entry.setMdEntryPX(value.getDoubleValue());

            // volume
            index = decodeTag(in, index, value);
            ensureTag(value.getIntValue(), FixFields.MD_ENTRY_SIZE);
            index = decodeDoubleValue(in, index, value);
            entry.setMdEntrySize(value.getDoubleValue());

            // quote id
            index = decodeTag(in, index, value);
            ensureTag(value.getIntValue(), FixFields.QUOTE_ENTRY_ID);
            index = decodeStringNativeValue(in, index, entry.getId());

            // issuer
            index = decodeTag(in, index, value);
            ensureTag(value.getIntValue(), FixFields.ISSUER);
            index = decodeStringNativeValue(in, index, entry.getIssuer());
        }

        return index;
    }

    static int encodeMassQuote(ByteBuf out, int index, byte[] temp, MassQuote message) {
        // quote id
        if (message.isQuoteIdDefined()) {
            index = encodeTag(out, index, FixFields.QUOTE_ID);
            index = encodeStringNativeValue(out, index, message.getQuoteId());
        }

        // number of quote sets
        index = encodeTag(out, index, FixFields.NO_QUOTE_SETS);
        index = encodeIntValue(out, index, temp, message.getQuoteSetCount());

        // quote sets
        for (int i = 0; i < message.getQuoteSetCount(); i++) {
            MassQuote.QuoteSet quoteSet = message.getQuoteSet(i);

            // quote set id
            if (quoteSet.isQuoteSetIdDefined()) {
                index = encodeTag(out, index, FixFields.QUOTE_SET_ID);
                index = encodeStringNativeValue(out, index, quoteSet.getQuoteSetId());
            }

            // number of quot entries
            index = encodeTag(out, index, FixFields.NO_QUOTE_ENTRIES);
            index = encodeIntValue(out, index, temp, quoteSet.getEntryCount());

            // quote entries
            for (int j = 0; j < quoteSet.getEntryCount(); j++) {
                MassQuote.QuoteEntry entry = quoteSet.getEntry(j);

                // quote entry id
                if (entry.isQuoteEntryIdDefined()) {
                    index = encodeTag(out, index, FixFields.QUOTE_ENTRY_ID);
                    index = encodeStringNativeValue(out, index, entry.getQuoteEntryId());
                }

                // issuer
                if (entry.isIssuerDefined()) {
                    index = encodeTag(out, index, FixFields.ISSUER);
                    index = encodeStringNativeValue(out, index, entry.getIssuer());
                }

                // bid size
                index = encodeTag(out, index, FixFields.BID_SIZE);
                index = encodeDoubleValue(out, index, temp, entry.getBidSize());

                // bid price
                index = encodeTag(out, index, FixFields.BID_SPOT_RATE);
                index = encodeDoubleValue(out, index, temp, entry.getBidSpotRate());

                // offer size
                index = encodeTag(out, index, FixFields.OFFER_SIZE);
                index = encodeDoubleValue(out, index, temp, entry.getOfferSize());

                // offer price
                index = encodeTag(out, index, FixFields.OFFER_SPOT_RATE);
                index = encodeDoubleValue(out, index, temp, entry.getOfferSpotRate());
            }
        }

        return index;
    }

    public static int encodeMassQuoteAcknowledgement(ByteBuf out, int index, MassQuoteAcknowledgement message) {
        // quote id
        if (message.isQuoteIdDefined()) {
            index = encodeTag(out, index, FixFields.QUOTE_ID);
            index = encodeStringNativeValue(out, index, message.getQuoteId());
        }

        return index;
    }
}
