package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

import static io.github.thepun.fix.PrimitiveCodecUtil.*;

class GenericCodecUtil {

    static int decodeMarketDataRequest(ByteBuf in, int index, byte[] temp, Value value, MarketDataRequest message) {
        int tag;

        index = decodeTag(in, index, value);
        do {
            tag = value.getIntValue();

            switch (tag) {
                case FixFields.MD_REQ_ID:
                    index = decodeStringValue(in, index, temp, value);
                    message.setMdReqId(value.getStrValue());
                    index = decodeTag(in, index, value);
                    break;

                case FixFields.SUBSCRIPTION_REQUEST_TYPE:
                    index = decodeIntValue(in, index, value);
                    message.setSubscriptionRequestType(value.getIntValue());
                    index = decodeTag(in, index, value);
                    break;

                case FixFields.MARKET_DEPTH:
                    index = decodeIntValue(in, index, value);
                    message.setMarketDepth(value.getIntValue());
                    index = decodeTag(in, index, value);
                    break;

                case FixFields.NO_RELATED_SYM:
                    index = decodeIntValue(in, index, value);
                    int relatedSymCount = value.getIntValue();
                    message.setRelatedSymCount(relatedSymCount);

                    symbols:
                    for (int i = 0; i < relatedSymCount; i++) {
                        MarketDataRequest.RelatedSymGroup relatedSym = message.getRelatedSym(i);

                        index = decodeTag(in, index, value);
                        tag = value.getIntValue();

                        switch (tag) {
                            case FixFields.SYMBOL:
                                index = decodeStringValue(in, index, temp, value);
                                relatedSym.setSymbol(value.getStrValue());
                                break;

                            default:
                                break symbols;
                        }
                    }
                    index = decodeTag(in, index, value);

                    break;

                case FixFields.CHECK_SUM:
                    return skipValue(in, index);

                default:
                    index = skipValue(in, index);
                    index = decodeTag(in, index, value);
            }
        } while (tag != FixFields.CHECK_SUM);

        return skipValue(in, index);
    }

    static int decodeMarketDataRequestReject(ByteBuf in, int index, byte[] temp, Value value, MarketDataRequestReject message) {
        int tag;

        do {
            index = decodeTag(in, index, value);
            tag = value.getIntValue();

            switch (tag) {
                case FixFields.MD_REQ_ID:
                    index = decodeStringValue(in, index, temp, value);
                    message.setMdReqId(value.getStrValue());
                    break;

                case FixFields.TEXT:
                    index = decodeStringValue(in, index, temp, value);
                    message.setText(value.getStrValue());
                    break;

                default:
                    index = skipValue(in, index);
            }
        } while (tag != FixFields.CHECK_SUM);

        return skipValue(in, index);
    }

    static int encodeMarketDataRequest(ByteBuf out, int index, byte[] temp, MarketDataRequest message) {
        // md req id
        if (message.getMdReqId() != null) {
            index = encodeTag(out, index, FixFields.MD_REQ_ID);
            index = encodeStringValue(out, index, message.getMdReqId());
        }

        // subscription request type
        index = encodeTag(out, index, FixFields.SUBSCRIPTION_REQUEST_TYPE);
        index = encodeIntValue(out, index, temp, message.getSubscriptionRequestType());

        // market depth
        if (message.getMarketDepth() >= 0) {
            index = encodeTag(out, index, FixFields.MARKET_DEPTH);
            index = encodeIntValue(out, index, temp, message.getMarketDepth());
        }

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

    static int encodeMarketDataRequestReject(ByteBuf out, int index, Value value, MarketDataRequestReject message) {
        // md req id
        if (message.getMdReqId() != null) {
            index = encodeTag(out, index, FixFields.MD_REQ_ID);
            index = encodeStringValue(out, index, message.getMdReqId());
        }

        // test
        if (message.getMdReqId() != null) {
            index = encodeTag(out, index, FixFields.TEXT);
            index = encodeStringValue(out, index, message.getText());
        }

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

    static int decodeHeartbeat(ByteBuf in, int index, Value value, Heartbeat message) {
        index = decodeTag(in, index, value);
        int tag = value.getIntValue();
        if (tag == FixFields.TEST_REQ_ID) {
            decodeStringNativeValue(in, index, message.getTestId());
            message.setTestIdDefined(true);

            index = decodeTag(in, index, value);
        }

        return skipValue(in, index);
    }

    static int decodeTest(ByteBuf in, int index, Value value, Test message) {
        index = decodeTag(in, index, value);
        decodeStringNativeValue(in, index, message.getTestId());

        index = decodeTag(in, index, value);
        return skipValue(in, index);
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
        if (message.isTestIdDefined()) {
            index = encodeTag(out, index, FixFields.TEST_REQ_ID);
            index = encodeStringNativeValue(out, index, message.getTestId());
        }
        return index;
    }

    static int encodeTest(ByteBuf out, int index, Test message) {
        index = encodeTag(out, index, FixFields.TEST_REQ_ID);
        index = encodeStringNativeValue(out, index, message.getTestId());
        return index;
    }
}
