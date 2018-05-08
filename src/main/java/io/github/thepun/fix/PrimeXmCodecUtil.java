package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

import static io.github.thepun.fix.CommonCodecUtil.*;

final class PrimeXmCodecUtil {

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
            index = decodeNativeStringValue(in, index, message.getQuoteId());
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
            index = decodeNativeStringValue(in, index, quoteSet.getQuoteSetId());
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
                index = decodeNativeStringValue(in, index, entry.getQuoteEntryId());
                entry.setQuoteEntryIdDefined(true);

                // one tag in future
                index = decodeTag(in, index, value);
                tag = value.getIntValue();

                // optional issuer
                if (tag == FixFields.ISSUER) {
                    index = decodeNativeStringValue(in, index, entry.getIssuer());
                    entry.setIssuerDefined(true);

                    index = decodeTag(in, index, value);
                    tag = value.getIntValue();
                }

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

    static int decodeMarketDataRequest(ByteBuf in, int index, Value value, MarketDataRequest message) {
       // TODO: implements decoding of market data request
        return index;
    }

    static int decodeMarketDataRequestReject(ByteBuf in, int index, byte[] temp, Value value, MarketDataRequestReject message) {
        int tag;

        do {
            index = decodeTag(in, index, value);
            tag = value.getIntValue();

            switch (tag) {
                case FixFields.MD_REQ_ID:
                    decodeStringValue(in, index, temp, value);
                    message.setMdReqID(value.getStrValue());
                    break;

                case FixFields.TEXT:
                    decodeStringValue(in, index, temp, value);
                    message.setText(value.getStrValue());
                    break;

                default:
                    index = skipValue(in, index);
            }
        } while (tag != FixFields.CHECK_SUM);

        return skipValue(in, index);
    }

    static int encodeMassQuote(ByteBuf out, int index, Value value, MassQuote message) {
        // TODO: implement mass quote encoding
        return index;
    }

    static int encodeMarketDataRequest(ByteBuf out, int index, byte[] temp, Value value, MarketDataRequest message) {
        // md req id
        index = encodeTag(out, index, FixFields.MD_REQ_ID);
        index = encodeStringValue(out, index, message.getMdReqId());

        // subscription request type
        index = encodeTag(out, index, FixFields.SUBSCRIPTION_REQUEST_TYPE);
        index = encodeIntValue(out, index, temp, message.getSubscriptionRequestType());

        // market depth
        index = encodeTag(out, index, FixFields.MARKET_DEPTH);
        index = encodeIntValue(out, index, temp, message.getMarketDepth());

        // no related sym
        index = encodeTag(out, index, FixFields.NO_RELATED_SYM);
        index = encodeIntValue(out, index, temp, message.getRelatedSymsCount());

        // related sym loop
        for (int i = 0; i < message.getRelatedSymsCount(); i++) {
            MarketDataRequest.RelatedSymGroup relatedSym = message.getRelatedSym(i);

            // symbol
            index = encodeTag(out, index, FixFields.SYMBOL);
            index = encodeStringValue(out, index, relatedSym.getSymbol());
        }

        return index;
    }

    static void encodeMarketDataRequestReject(ByteBuf in, int index, Value value, MarketDataRequestReject message) {
        // TODO: implement market data request reject encoding
    }


}
