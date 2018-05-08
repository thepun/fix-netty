package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

import static io.github.thepun.fix.CommonCodecUtil.*;
import static io.github.thepun.fix.PrimeXmCodecUtil.*;

final class PrimeXmClientHandler extends ChannelDuplexHandler {

    private static final String START_HEADER = "8=FIX.4.4" + ((char)1) + "9=";
    private static final int START_HEADER_LENGTH = START_HEADER.length();
    private static final int BEGIN_STRING_LENGTH = 10;
    private static final int CHECKSUM_LENGTH = 7;


    private final int heartbeatInterval;

    private final FixLogger fixLogger;
    private final FixSessionInfo sessionInfo;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;

    private Value value;
    private byte[] temp;
    private ByteBuf buffer;
    private ByteBuf sessionHeader;
    private ByteBuf beginHeader;
    private ByteBuf bodyLengthHeader;
    private String sessionName;

    private int sequenceNumber;
    private int sessionHeaderLength;
    private ScheduledFuture<?> heartbeatSchedule;

    PrimeXmClientHandler(FixSessionInfo sessionInfo, FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener, int heartbeatInterval) {
        this.fixLogger = fixLogger;
        this.sessionInfo = sessionInfo;
        this.readyListener = readyListener;
        this.quotesListener = quotesListener;
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        sequenceNumber = 1;
        value = new Value();
        temp = new byte[1024];

        // begin string buffer is always the same
        ByteBuf newBeginHeader = ctx.alloc().directBuffer();
        newBeginHeader.writeCharSequence(START_HEADER, CharsetUtil.US_ASCII);
        beginHeader = newBeginHeader;

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
        newSessionHeader.writeCharSequence(FixFields.SENDING_TIME + "=", CharsetUtil.US_ASCII);
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
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelHeartbeats();

        super.channelInactive(ctx);
    }

    // TODO: ensure on error buffers will not leak
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Value value = this.value;
        byte[] temp = this.temp;

        ByteBuf msgByteBuf = (ByteBuf) msg;

        int index, start, count, length, msgType;

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

        // read until the end
        int readableBytes = in.readableBytes();
        while (readableBytes > 0) {
            // remember message start
            index = in.readerIndex();
            start = index;

            // skip the beginning
            index += BEGIN_STRING_LENGTH;

            // read length
            index = skipTag(in, index);
            index = decodeIntValue(in, index, value);
            length = value.getIntValue() + index + CHECKSUM_LENGTH; // include checksum

            // check we have enough bytes
            if (length > readableBytes) {
                buffer = in;
                return;
            }

            // log message
            fixLogger.incoming(in, start, length);

            // read message type
            index = skipTag(in, index);
            index = decodeStringValueAsInt(in, index, value);
            msgType = value.getIntValue();

            // TODO: read header in fixed order
            // skip rest header fields
            index = skipHeader(in, index);

            // read message content
            if (msgType == FixMsgTypes.MASS_QUOTE) {
                // fast path
                MassQuote quotes = MassQuote.reuseOrCreate();
                index = decodeMassQuote(in, index, value, quotes);
                quotesListener.onMarketData(quotes);
            } else {
                // slow path
                switch (msgType) {
                    case FixMsgTypes.HEARTBEAT:
                        Heartbeat heartbeat = Heartbeat.reuseOrCreate();
                        index = decodeHeartbeat(in, index, heartbeat);
                        // just decode it and not do anything else
                        break;

                    case FixMsgTypes.TEST:
                        Test test = Test.newInstance();
                        index = decodeTest(in, index, test);
                        // send heartbeat on test message
                        Heartbeat heartbeatForTest = Heartbeat.reuseOrCreate();
                        if (test.isTestIdDefined()) {
                            heartbeatForTest.initBuffer(msgByteBuf);
                            heartbeatForTest.getTestId().setAddress(test.getTestId());
                            heartbeatForTest.setTestIdDefined(true);
                        } else {
                            heartbeatForTest.setTestIdDefined(false);
                        }
                        write(ctx, heartbeatForTest, ctx.voidPromise());
                        ctx.flush();
                        break;

                    case FixMsgTypes.MARKET_DATA_REJECT:
                        MarketDataRequestReject reject = new MarketDataRequestReject();
                        index = decodeMarketDataRequestReject(in, index, temp, value, reject);
                        fixLogger.status("Market data request with id " + reject.getMdReqID() + " in session " + sessionName + " was rejected: " + reject.getText());
                        break;

                    case FixMsgTypes.LOGON:
                        Logon logon = new Logon();
                        index = decodeLogon(in, index, temp, value, logon);
                        fixLogger.status("Received logon in session " + sessionName);
                        scheduleHeartbeats(ctx);
                        SubscriptionSender subscriptionSender = new SubscriptionSender(ctx.channel());
                        readyListener.onReady(subscriptionSender);
                        subscriptionSender.disable();
                        break;

                    case FixMsgTypes.LOGOUT:
                        Logout logout = new Logout();
                        index = decodeLogout(in, index, temp, value, logout);
                        fixLogger.status("Received logout in session " + sessionName);
                        cancelHeartbeats();
                        ctx.close();
                        break;

                    default:
                        index = skipUntilChecksum(in, index);
                        fixLogger.status("Unknown message type in session " + sessionName + ": " + msgType);
                }
            }

            // change buffer position
            in.readerIndex(index);
            readableBytes = in.readableBytes();
        }

        // we finished reading from message or buffer fully
        in.release();
        buffer = null;
    }

    // TODO: ensure on error buffers will not leak
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        Value value = this.value;
        byte[] temp = this.temp;

        // prepare buffers
        ByteBuf headBuf = ctx.alloc().directBuffer();
        ByteBuf bodyBuf = ctx.alloc().directBuffer();
        int headIndex = headBuf.writerIndex();
        int bodyIndex = bodyBuf.writerIndex();

        // write static header
        headBuf.writeBytes(beginHeader, 0, START_HEADER_LENGTH);

        // write msg type
        encodeTag(bodyBuf, bodyIndex, FixFields.MSG_TYPE)
        int msgTypeIndex = bodyIndex;
        encodeDelimiter()
        cursor.setIndex(msgTypeIndex + 2);
        bodyBuf.setByte(msgTypeIndex + 1, 1);

        // write sequence number
        cursor.setTag(FixFields.MSG_SEQ_NUM);
        cursor.setIntValue(sequenceNumber);
        encodeTag(cursor);
        encodeIntValue(cursor);
        sequenceNumber++;

        // write session info
        index = cursor.getIndex();
        sessionHeader.getBytes(0, bodyBuf, index, sessionHeaderLength);
        cursor.setIndex(index + sessionHeaderLength);

        // write sending time value
        cursor.setLongValue(System.currentTimeMillis());
        encodeDateTime(cursor);

        // write body
        if (msg instanceof MarketDataRequest) {
            bodyBuf.setByte(msgTypeIndex, FixMsgTypes.MARKET_DATA_REQUEST);

            MarketDataRequest marketDataRequest = (MarketDataRequest) msg;
            PrimeXmCodecUtil.encodeMarketDataRequest(cursor, marketDataRequest);
        } else if (msg instanceof Heartbeat) {
            bodyBuf.setByte(msgTypeIndex, FixMsgTypes.HEARTBEAT);

            Heartbeat heartbeat = (Heartbeat) msg;
            encodeHeartbeat(cursor, heartbeat);
        } else if (msg instanceof Logon) {
            bodyBuf.setByte(msgTypeIndex, FixMsgTypes.LOGON);

            Logon logon = (Logon) msg;
            encodeLogon(cursor, logon);
        } else {
            fixLogger.status("Unknown message: " + msg.getClass().getName());
            return;
        }

        // write actual body length
        int bodyLength = cursor.getIndex();
        startEncoding(cursor, headBuf, temp);
        cursor.setIndex(headBuf.writerIndex());
        cursor.setIntValue(bodyLength);
        encodeIntValue(cursor);
        int headLength = cursor.getIndex();
        headBuf.writerIndex(headLength);

        // calculate checksum
        int sum = 0;
        for (int i = 0; i < headLength; i++) {
            byte b = headBuf.getByte(i);
            sum += b;
        }
        for (int i = 0; i < bodyLength; i++) {
            byte b = bodyBuf.getByte(i);
            sum += b;
        }
        sum %= 256;

        // write checksum and finish
        startEncoding(cursor, bodyBuf, temp);
        cursor.setIndex(bodyLength);
        cursor.setTag(FixFields.CHECK_SUM);
        cursor.setIntValue(sum);
        encodeTag(cursor);
        index = cursor.getIndex();
        index = encodeThreeDigitInt(bodyBuf, index, sum);
        index = encodeDelimiter(bodyBuf, index);
        bodyBuf.writerIndex(index);

        // send to channel
        headBuf.retain();
        bodyBuf.retain();
        ctx.write(headBuf, promise);
        ctx.write(bodyBuf, promise);
        ctx.flush();

        // log outgoing message
        fixLogger.outgoing(headBuf, 0, headLength, bodyBuf, 0, bodyLength + 7);
        headBuf.release();
        bodyBuf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);

        // TODO: process exception in channel
    }

    private void scheduleHeartbeats(ChannelHandlerContext ctx) {
        heartbeatSchedule = ctx.executor().schedule(() -> {
            Heartbeat heartbeatForTest = Heartbeat.reuseOrCreate();
            heartbeatForTest.setTestIdDefined(false);
            write(ctx, heartbeatForTest, ctx.voidPromise());
            ctx.flush();
        }, heartbeatInterval, TimeUnit.SECONDS);
    }

    private void cancelHeartbeats() {
        if (heartbeatSchedule != null) {
            heartbeatSchedule.cancel(false);
            heartbeatSchedule = null;
        }
    }
}