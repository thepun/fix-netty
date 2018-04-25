package io.github.thepun.fix.md.codec;

import io.github.thepun.fix.Fields;
import io.github.thepun.fix.MsgTypes;
import io.github.thepun.fix.md.domain.MarketDataSnapshotFullRefresh;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.util.List;

final class MarketDataDecoder extends ChannelInboundHandlerAdapter {

    private static final class Cursor {
        private ByteBuf in;
        private int count;
        private int index;
        private int tag;
        private int intValue;
        private int strAsInt;
        private int strStart;
        private int strLength;
        private int nativeAddress;
    }


    private ByteBuf buffer;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        buffer = ctx.alloc().directBuffer(1024 * 1024);

        super.channelRegistered(ctx);
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
                        MarketDataSnapshotFullRefresh message = MarketDataSnapshotFullRefresh.newInstance();
                        message.setMessageBuffer(in);
                        parseMarketDataSnapshotFullRefresh(cursor, message);

                    case MsgTypes.LOGON_MSG_TYPE:
                        break;
                }
            }
        } else {
            // error
        }
    }

    private static void parseMarketDataSnapshotFullRefresh(Cursor cursor, MarketDataSnapshotFullRefresh message) {
        // MDReqID
        parseTag(cursor);
        ensureTag(cursor, Fields.MD_REQ_ID);
        parseStrValue(cursor);
        message.getMdReqID().setAddress(cursor.strStart, cursor.strLength);

        // Symbol
        parseTag(cursor);
        ensureTag(cursor, Fields.SYMBOL);
        parseStrValue(cursor);
        message.getSymbol().setAddress(cursor.strStart, cursor.strLength);

        // MD entries
        parseTag(cursor);
        ensureTag(cursor, Fields.NO_MD_ENTRIES);
        parseIntValue(cursor);
        int mdEntriesCount = cursor.intValue;
        for (int i = 0; i < mdEntriesCount; i++) {
            // MD entry
            parseTag(cursor);
            ensureTag(cursor, Fields.NO_MD_ENTRIES);

        }
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
