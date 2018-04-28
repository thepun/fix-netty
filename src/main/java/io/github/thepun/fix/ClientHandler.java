package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;

import static io.github.thepun.fix.DecodingUtil.*;

final class ClientHandler extends ChannelDuplexHandler {

    private final Logon logon;
    private final Logout logout;
    private final MarketDataRequestReject reject;

    private final FixLogger fixLogger;
    private final FixSessionInfo sessionInfo;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;
    private final MarketDataSnapshotListener snapshotListener;

    private ByteBuf buffer;
    private ByteBuf bginHeader;
    private ByteBuf sessionHeader;
    private String sessionName;

    ClientHandler(FixSessionInfo sessionInfo, FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener, MarketDataSnapshotListener snapshotListener) {
        this.fixLogger = fixLogger;
        this.sessionInfo = sessionInfo;
        this.readyListener = readyListener;
        this.quotesListener = quotesListener;
        this.snapshotListener = snapshotListener;

        logon = new Logon();
        logout = new Logout();
        reject = new MarketDataRequestReject();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // begin string buffer is alwas the same
        ByteBuf newBeginHeader = ctx.alloc().directBuffer();
        newBeginHeader.writeCharSequence("8=FIX.4.4", CharsetUtil.US_ASCII);
        newBeginHeader.writeByte(1);
        bginHeader = newBeginHeader;

        // session header is also always the same
        ByteBuf newSessionHeader = ctx.alloc().directBuffer();
        newSessionHeader.writeCharSequence(FixFields.SENDER_COMP_ID + "=" + sessionInfo.getSenderCompId(), CharsetUtil.US_ASCII);
        newSessionHeader.writeByte(1);
        newSessionHeader.writeCharSequence(FixFields.TARGET_COMP_ID + "=" + sessionInfo.getTargetCompId(), CharsetUtil.US_ASCII);
        newSessionHeader.writeByte(1);
        if (sessionInfo.getSenderSubId() != null) {
            newSessionHeader.writeCharSequence(FixFields.SENDER_SUB_ID + "=" + sessionInfo.getSenderSubId(), CharsetUtil.US_ASCII);
            newSessionHeader.writeByte(1);
        }
        if (sessionInfo.getTargetSubId() != null) {
            newSessionHeader.writeCharSequence(FixFields.TARGET_SUB_ID + "=" + sessionInfo.getTargetSubId(), CharsetUtil.US_ASCII);
            newSessionHeader.writeByte(1);
        }
        sessionHeader = newSessionHeader;

        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        bginHeader.release();
        bginHeader = null;

        sessionHeader.release();
        sessionHeader = null;

        super.handlerRemoved(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sessionName = ctx.channel().toString();
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf msgByteBuf = (ByteBuf) msg;

        int index, start, length, msgType;

        // check if we should read from buffer first
        ByteBuf in;
        ByteBuf localBuffer = buffer;
        if (localBuffer != null) {
            // allocate new buffer and copy everything if we don't have enough capacity to store new message
            int msgReadableBytes = msgByteBuf.readableBytes();
            if (!localBuffer.isWritable(msgReadableBytes)) {
                int bufferReadableBytes = localBuffer.readableBytes();

                ByteBuf newBuffer = ctx.alloc().directBuffer(bufferReadableBytes + msgReadableBytes);
                newBuffer.writeBytes(localBuffer);
                localBuffer.release();
                localBuffer = newBuffer;
                buffer = newBuffer;
            }

            localBuffer.writeBytes(msgByteBuf);
            msgByteBuf.release();

            in = localBuffer;
        } else {
            in = msgByteBuf;
        }

        // prepare cursor object
        Cursor cursor = new Cursor();
        start(cursor, in);

        // read until the end
        index = cursor.getIndex();
        while (index < cursor.getPoint()) {
            // remember message start
            start = index;

            // read fix
            decodeTag(cursor);
            ensureTag(cursor, FixFields.BEGIN_STRING);
            cursor.setIndex(cursor.getIndex() + 8);

            // read length
            decodeTag(cursor);
            ensureTag(cursor, FixFields.BODY_LENGTH);
            decodeIntValue(cursor);
            length = cursor.getIntValue();
            length += 7; // include checksum

            // check we have enough bytes
            if (cursor.getIndex() + length >= cursor.getPoint()) {
                buffer = in;
                return;
            }

            // log message
            fixLogger.incoming(in, start, length);

            // read message type
            decodeTag(cursor);
            ensureTag(cursor, FixFields.MSG_TYPE);
            decodeStrValueAsInt(cursor);
            msgType = cursor.getStrAsInt();

            // skip rest header fields
            decodeTagAndSkipHeader(cursor);

            // read message content
            switch (msgType) {
                case FixMsgTypes.MASS_QUOTE:
                    MassQuote quotes = MassQuote.newInstance();
                    decodeMassQuote(cursor, quotes);
                    quotesListener.onMarketData(quotes);
                    break;

                case FixMsgTypes.MARKET_DATA_SNAPSHOT_FULL_REFRESH:
                    MarketDataSnapshotFullRefresh snapshot = MarketDataSnapshotFullRefresh.newInstance();
                    decodeMarketDataSnapshotFullRefresh(cursor, snapshot);
                    snapshotListener.onMarketData(snapshot);
                    break;

                case FixMsgTypes.MARKET_DATA_REJECT:
                    decodeMarketDataReject(cursor, reject);
                    fixLogger.status("Market data request with id " + reject.getMdReqID() + " in session " + sessionName + " was rejected: " + reject.getText());
                    break;

                case FixMsgTypes.LOGON:
                    decodeLogon(cursor, logon);
                    fixLogger.status("Logon in session " + sessionName);
                    SubscriptionSender subscriptionSender = new SubscriptionSender(ctx.channel());
                    readyListener.onReady(subscriptionSender);
                    subscriptionSender.disable();
                    break;

                case FixMsgTypes.LOGOUT:
                    decodeLogout(cursor, logout);
                    fixLogger.status("Logout in session " + sessionName);
                    ctx.close();
                    break;

                default:
                    fixLogger.status("Unknown message type in session " + sessionName + ": " + msgType);
            }

            // skip checksum and change buffer position
            index = cursor.getIndex();
            in.readerIndex(index);
        }

        // we finished reading from message or buffer fully
        in.release();
        buffer = null;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ByteBuf msgByteBuf = ctx.alloc().directBuffer();

        // write begin string
        bginHeader.getBytes(0, msgByteBuf);

        // TODO: write body length mock

        // TODO: write msg type

        if (msg instanceof MarketDataRequest) {
            MarketDataRequest marketDataRequest = (MarketDataRequest) msg;
            // TODO: do encoding
        } else {
            fixLogger.status("Unknown message: " + msg.getClass().getName());
        }

        // TODO: write body length

        // TODO: write checksum

        ctx.writeAndFlush(msgByteBuf, promise);
    }
}
