package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;

import static io.github.thepun.fix.DecodingUtil.*;
import static io.github.thepun.fix.EncodingUtil.*;

final class PrimeXMClientHandler extends ChannelDuplexHandler {

    private static final int BEGIN_HEADER_LENGTH = 10;


    private final Logon logon;
    private final Logout logout;
    private final MarketDataRequestReject reject;

    private final FixLogger fixLogger;
    private final FixSessionInfo sessionInfo;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;

    private ByteBuf buffer;
    private ByteBuf sessionHeader;
    private ByteBuf beginHeader;
    private ByteBuf bodyLengthHeader;
    private String sessionName;
    private byte[] heapBuffer;

    private int sequenceNumber;
    private int sessionHeaderLength;

    PrimeXMClientHandler(FixSessionInfo sessionInfo, FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener) {
        this.fixLogger = fixLogger;
        this.sessionInfo = sessionInfo;
        this.readyListener = readyListener;
        this.quotesListener = quotesListener;

        logon = new Logon();
        logout = new Logout();
        reject = new MarketDataRequestReject();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // temp heap buffer
        heapBuffer = new byte[1024];

        // begin string buffer is always the same
        ByteBuf newBeginHeader = ctx.alloc().directBuffer();
        newBeginHeader.writeCharSequence("8=FIX.4.4", CharsetUtil.US_ASCII);
        newBeginHeader.writeByte(1);
        beginHeader = newBeginHeader;

        // body length prefix
        ByteBuf newBodyLengthHeader = ctx.alloc().directBuffer();
        newBodyLengthHeader.writeCharSequence("9=", CharsetUtil.US_ASCII);
        bodyLengthHeader = newBeginHeader;

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
        sessionHeaderLength = newSessionHeader.readableBytes();

        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        beginHeader.release();
        beginHeader = null;

        sessionHeader.release();
        sessionHeader = null;

        super.handlerRemoved(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sessionName = ctx.channel().toString();

        super.channelActive(ctx);

        fixLogger.status("Sending logon in session " + sessionName);

        Logon logon = new Logon();
        logon.setEncryptMethod(0);
        logon.setHeartbeatInterval(10);
        logon.setResetSqNumFlag(true);
        logon.setUsername(sessionInfo.getUsername());
        logon.setPassword(sessionInfo.getPassword());
        write(ctx, logon, ctx.voidPromise());
        flush(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelHeartbeats();

        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
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
        startDecoding(cursor, in, heapBuffer);

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
            decodeStringValueAsInt(cursor);
            msgType = cursor.getIntValue();

            // skip rest header fields
            decodeTagAndSkipHeader(cursor);

            // read message content
            if (msgType == FixMsgTypes.MASS_QUOTE) {
                // fast path
                MassQuote quotes = MassQuote.newInstance();
                decodeMassQuote(cursor, quotes);
                quotesListener.onMarketData(quotes);
            } else {
                // slow path
                switch (msgType) {
                    case FixMsgTypes.HEARTBEAT:
                        Heartbeat heartbeat = Heartbeat.newInstance();
                        decodeHeartbeat(cursor, heartbeat);
                        // just decode it and not do anything else
                        break;

                    case FixMsgTypes.TEST:
                        Test test = Test.newInstance();
                        decodeTest(cursor, test);
                        // send heartbeat on test message
                        Heartbeat heartbeatForTest = Heartbeat.newInstance();
                        if (test.isTestIdDefined()) {
                            heartbeatForTest.initBuffer(msgByteBuf);
                            heartbeatForTest.getTestId().setAddress(test.getTestId());
                            heartbeatForTest.setTestIdDefined(true);
                        } else {
                            heartbeatForTest.setTestIdDefined(false);
                        }
                        write(ctx, heartbeatForTest, ctx.voidPromise());
                        flush(ctx);
                        break;

                    case FixMsgTypes.MARKET_DATA_REJECT:
                        decodeMarketDataRequestReject(cursor, reject);
                        fixLogger.status("Market data request with id " + reject.getMdReqID() + " in session " + sessionName + " was rejected: " + reject.getText());
                        break;

                    case FixMsgTypes.LOGON:
                        decodeLogon(cursor, logon);
                        fixLogger.status("Received logon in session " + sessionName);
                        scheduleHeartbeats();
                        SubscriptionSender subscriptionSender = new SubscriptionSender(ctx.channel());
                        readyListener.onReady(subscriptionSender);
                        subscriptionSender.disable();
                        break;

                    case FixMsgTypes.LOGOUT:
                        decodeLogout(cursor, logout);
                        fixLogger.status("Received logout in session " + sessionName);
                        cancelHeartbeats();
                        ctx.close();
                        break;

                    default:
                        fixLogger.status("Unknown message type in session " + sessionName + ": " + msgType);
                }
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
        int index;

        // write begin string + body length mock
        ByteBuf headerBuf = ctx.alloc().directBuffer();
        beginHeader.getBytes(0, headerBuf);
        bodyLengthHeader.getBytes(BEGIN_HEADER_LENGTH, headerBuf);

        // prepare cursor
        ByteBuf msgByteBuf = ctx.alloc().directBuffer();
        Cursor cursor = new Cursor();
        startEncoding(cursor, msgByteBuf);
        int start = cursor.getIndex();

        // write msg type
        cursor.setTag(FixFields.MSG_TYPE);
        encodeTag(cursor);
        int msgTypeIndex = cursor.getIndex();
        cursor.setIndex(msgTypeIndex + 2);
        msgByteBuf.setByte(msgTypeIndex + 1, 1);

        // write sequence number
        cursor.setTag(FixFields.MSG_SEQ_NUM);
        cursor.setIntValue(sequenceNumber);
        encodeTag(cursor);
        encodeIntValue(cursor);
        sequenceNumber++;

        // write session info
        index = cursor.getIndex();
        sessionHeader.getBytes(index, msgByteBuf);
        cursor.setIndex(index + sessionHeaderLength);

        // write body
        if (msg instanceof MarketDataRequest) {
            msgByteBuf.setByte(msgTypeIndex, FixMsgTypes.MARKET_DATA_REQUEST);

            MarketDataRequest marketDataRequest = (MarketDataRequest) msg;
            encodeMarketDataRequest(cursor, marketDataRequest);
        } else if (msg instanceof Logon) {
            msgByteBuf.setByte(msgTypeIndex, FixMsgTypes.LOGON);

            Logon logon = (Logon) msg;
            encodeLogon(cursor, logon);
        } else {
            fixLogger.status("Unknown message: " + msg.getClass().getName());
        }

        // remember body length and calculate checksum
        index = cursor.getIndex();
        int bodyLength = index - start;
        int sum = 0;
        for (int i = start; i < index; i++) {
            byte b = msgByteBuf.getByte(i);
            sum += b;
        }
        sum %= 256;

        // write checksum and finish
        cursor.setTag(FixFields.CHECK_SUM);
        cursor.setIntValue(sum);
        encodeTag(cursor);
        index = cursor.getIndex();
        index = encodeThreeDigitInt(msgByteBuf, index, sum);
        index = encodeDelimiter(msgByteBuf, index);
        msgByteBuf.writerIndex(index);

        // write actual body length
        startEncoding(cursor, headerBuf);
        cursor.setIntValue(bodyLength);
        encodeIntValue(cursor);

        // remember indexes and send to channel
        int firstOffset = headerBuf.readerIndex();
        int firstLength = headerBuf.readableBytes();
        ctx.write(headerBuf, promise);
        int secondOffset = msgByteBuf.readerIndex();
        int secondLength = msgByteBuf.readableBytes();
        ctx.write(msgByteBuf, promise);

        // log outgoing message
        fixLogger.outgoing(headerBuf, firstOffset, firstLength, msgByteBuf, secondOffset, secondLength);
    }

    private void scheduleHeartbeats() {
        // TODO: scheduling of heartbeats
    }

    private void cancelHeartbeats() {
        // TODO: cancellation of heartbeats
    }
}
