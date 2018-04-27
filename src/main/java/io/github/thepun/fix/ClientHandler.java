package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

final class ClientHandler extends ChannelDuplexHandler {


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
            FixParserCursor cursor = new FixParserCursor();
            cursor.setIn(in);
            cursor.setIndex(in.readerIndex());
            cursor.setCount(in.readableBytes());

            // read until the end
            while (cursor.getIndex() < cursor.getCount()) {
                // read fix
                FixParser.parseTag(cursor);
                FixParser.ensureTag(cursor, Fields.BEGIN_STRING);
                cursor.setIndex(cursor.getIndex() + 8);

                // read length
                FixParser.parseTag(cursor);
                FixParser.ensureTag(cursor, Fields.BODY_LENGTH);
                FixParser.parseIntValue(cursor);
                length = cursor.getIntValue();

                // check we have enough bytes
                if (cursor.getIndex() + length >= cursor.getCount()) {
                    // if we processing original incoming message
                    if (readFromMsg) {
                        // save message to local buffer
                        buffer.writeBytes(msgByteBuf);
                    }

                    return;
                }

                // read message type
                FixParser.parseTag(cursor);
                FixParser.ensureTag(cursor, Fields.MSG_TYPE);
                FixParser.parseStrValueAsInt(cursor);
                msgType = cursor.getStrAsInt();

                // read session info (SenderCompId | TargetCompId | SenderSubId | TargetSubId | MsgSeqNum | SendingTime)
                for (int i = 0; i < 6; i++) {
                    FixParser.skipTagAndValue(cursor);
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

    private static void parseMarketDataSnapshotFullRefresh(FixParserCursor cursor, MarketDataSnapshotFullRefresh message) {
        // req id
        FixParser.parseTag(cursor);
        FixParser.ensureTag(cursor, Fields.MD_REQ_ID);
        FixParser.parseStrValue(cursor);
        message.getMdReqID().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // symbol
        FixParser.parseTag(cursor);
        FixParser.ensureTag(cursor, Fields.SYMBOL);
        FixParser.parseStrValue(cursor);
        message.getSymbol().setAddress(cursor.getStrStart(), cursor.getStrLength());

        // count of MD entries
        FixParser.parseTag(cursor);
        FixParser.ensureTag(cursor, Fields.NO_MD_ENTRIES);
        FixParser.parseIntValue(cursor);
        int mdEntriesCount = cursor.getIntValue();
        message.setEntryCount(mdEntriesCount);

        // MD entry loop
        for (int i = 0; i < mdEntriesCount; i++) {
            MarketDataSnapshotFullRefresh.MDEntryGroup entry = message.getEntry(i);

            // type
            FixParser.parseTag(cursor);
            FixParser.ensureTag(cursor, Fields.MD_ENTRY_TYPE);
            FixParser.parseIntValue(cursor);
            entry.setMdEntryType(cursor.getIntValue());

            // id
            FixParser.parseTag(cursor);
            FixParser.ensureTag(cursor, Fields.MD_ENTRY_ID);
            FixParser.parseStrValue(cursor);
            entry.getId().setAddress(cursor.getStrStart(), cursor.getStrLength());

            // price
            FixParser.parseTag(cursor);
            FixParser.ensureTag(cursor, Fields.MD_ENTRY_PX);
            FixParser.parseDoubleValue(cursor);
            entry.setMdEntryPX(cursor.getDoubleValue());

            // volume
            FixParser.parseTag(cursor);
            FixParser.ensureTag(cursor, Fields.MD_ENTRY_SIZE);
            FixParser.parseDoubleValue(cursor);
            entry.setMdEntrySize(cursor.getDoubleValue());
        }
    }

    private static void parseMarketDataReject(FixParserCursor cursor, MarketDataReject message) {

    }

}
