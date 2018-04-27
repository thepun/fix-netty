package io.github.thepun.fix.md;

import io.github.thepun.fix.Fields;
import io.github.thepun.fix.FixLogger;
import io.github.thepun.fix.MsgTypes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;

final class ClientHandler extends ChannelDuplexHandler {

    private static final class Cursor {
        private ByteBuf in;
        private int count;
        private int index;
        private int tag;
        private int intValue;
        private int strAsInt;
        private long strStart;
        private int strLength;
        private long nativeAddress;
        private double doubleValue;
    }


    private final FixLogger fixLogger;
    private final MarketDataSnapshotListener snapshotListener;
    private final MarketDataConnectListener connectListener;
    private final MarketDataDisconnectListener disconnectListener;

    private ByteBuf buffer;

    ClientHandler(FixLogger fixLogger, MarketDataSnapshotListener snapshotListener, MarketDataConnectListener connectListener, MarketDataDisconnectListener disconnectListener) {
        this.fixLogger = fixLogger;
        this.snapshotListener = snapshotListener;
        this.connectListener = connectListener;
        this.disconnectListener = disconnectListener;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf msgByteBuf = (ByteBuf) msg;

            // check if we should read from buffer first
            ByteBuf in;
            boolean readFromMsg;
            if (buffer.isReadable()) {
                buffer.writeBytes(msgByteBuf);
                in = buffer;
                readFromMsg = false;
            } else {
                in = msgByteBuf;
                readFromMsg = true;
            }

            int length;
            int msgType;

            // prepare cursor object
            Cursor cursor = new Cursor();
            cursor.in = in;
            cursor.index = in.readerIndex();
            cursor.count = in.readableBytes();

            // read until the end
            while (cursor.index < cursor.count) {
                // read fix
                parseTag(cursor);
                ensureTag(cursor, Fields.BEGIN_STRING);
                cursor.index += 8; // just skip begin string - it is useless

                // read length
                parseTag(cursor);
                ensureTag(cursor, Fields.BODY_LENGTH);
                parseIntValue(cursor);
                length = cursor.intValue;

                // check we have enough bytes
                if (cursor.index + length >= cursor.count) {
                    // if we processing original incoming message
                    if (readFromMsg) {
                        // save message to local buffer
                        buffer.writeBytes(msgByteBuf);
                    }

                    return;
                }

                // read message type
                parseTag(cursor);
                ensureTag(cursor, Fields.MSG_TYPE);
                parseStrValueAsInt(cursor);
                msgType = cursor.strAsInt;

                // read session info (SenderCompId | TargetCompId | SenderSubId | TargetSubId | MsgSeqNum | SendingTime)
                for (int i = 0; i < 6; i++) {
                    skipTagAndValue(cursor);
                }

                // read message content
                switch (msgType) {
                    case MsgTypes.MARKET_DATA_SNAPSHOT_FULL_REFRESH:
                        MarketDataSnapshotFullRefresh snapshot = MarketDataSnapshotFullRefresh.newInstance();
                        parseMarketDataSnapshotFullRefresh(cursor, snapshot);
                        snapshot.setMessageBuffer(in);
                        snapshotListener.onMarketData(snapshot);
                        break;

                    case MsgTypes.MARKET_DATA_REJECT:
                        MarketDataReject reject = new MarketDataReject();
                        parseMarketDataReject(cursor, reject);
                        break;

                    case MsgTypes.LOGOUT:

                        ctx.close();
                        break;

                    case MsgTypes.LOGON:
                        break;
                }
            }
        } else {
            // error
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        super.write(ctx, msg, promise);
    }

    private static void parseMarketDataSnapshotFullRefresh(Cursor cursor, MarketDataSnapshotFullRefresh message) {
        // req id
        parseTag(cursor);
        ensureTag(cursor, Fields.MD_REQ_ID);
        parseStrValue(cursor);
        message.getMdReqID().setAddress(cursor.strStart, cursor.strLength);

        // symbol
        parseTag(cursor);
        ensureTag(cursor, Fields.SYMBOL);
        parseStrValue(cursor);
        message.getSymbol().setAddress(cursor.strStart, cursor.strLength);

        // count of MD entries
        parseTag(cursor);
        ensureTag(cursor, Fields.NO_MD_ENTRIES);
        parseIntValue(cursor);
        int mdEntriesCount = cursor.intValue;
        message.setEntryCount(mdEntriesCount);

        // MD entry loop
        for (int i = 0; i < mdEntriesCount; i++) {
            MDEntryGroup entry = message.getEntry(i);

            // type
            parseTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_TYPE);
            parseIntValue(cursor);
            entry.setMdEntryType(cursor.intValue);

            // id
            parseTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_ID);
            parseStrValue(cursor);
            entry.getId().setAddress(cursor.strStart, cursor.strLength);

            // price
            parseTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_PX);
            parseDoubleValue(cursor);
            entry.setMdEntryPX(cursor.doubleValue);

            // volume
            parseTag(cursor);
            ensureTag(cursor, Fields.MD_ENTRY_SIZE);
            parseDoubleValue(cursor);
            entry.setMdEntrySize(cursor.doubleValue);
        }
    }

    private static void parseMarketDataReject(Cursor cursor, MarketDataReject message) {

    }

    private static void parseTag(Cursor cursor) {
        int tagNum = 0;

        int index = cursor.index;
        for (; index < count; index++) {
            byte nextByte = in.getByte(index);
            if (nextByte == '=') {
                index++;
                break;
            }

            tagNum = tagNum * 10 + (nextByte - '0');
        }

        cursor.tag = tagNum;
        cursor.index = index;
    }

    private static void parseIntValue(Cursor cursor) {

    }

    private static void parseDoubleValue(Cursor cursor) {

    }

    private static void parseStrValue(Cursor cursor) {

    }

    private static void parseStrValueAsInt(Cursor cursor) {

    }

    private static void skipTagAndValue(Cursor cursor) {

    }

    private static void ensureTag(Cursor cursor, int tag) {
        if (cursor.tag != tag) {
            throw new DecoderException("Expected tag " + tag);
        }
    }

}
