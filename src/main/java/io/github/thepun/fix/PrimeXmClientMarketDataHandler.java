package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

import static io.github.thepun.fix.PrimitiveCodecUtil.*;
import static io.github.thepun.fix.PrimeXmCodecUtil.*;

final class PrimeXmClientMarketDataHandler extends ChannelDuplexHandler {

    private static final String START_HEADER_1 = "8=FIX.4.4";
    private static final String START_HEADER_2 = "9=";
    private static final int START_HEADER_LENGTH = START_HEADER_1.length() + START_HEADER_2.length() + 1;
    private static final int BEGIN_STRING_LENGTH = 10;
    private static final int CHECKSUM_LENGTH = 7;


    private final int heartbeatInterval;

    private final FixLogger fixLogger;
    private final FixSessionInfo sessionInfo;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;

    private Value value;
    private byte[] temp;
    private StringBuilder sb;
    private ByteBuf buffer;
    private ByteBuf sessionHeader;
    private ByteBuf beginHeader;
    private ByteBuf bodyLengthHeader;
    private String sessionName;

    private int sequenceNumber;
    private int sessionHeaderLength;
    private ScheduledFuture<?> heartbeatSchedule;

    PrimeXmClientMarketDataHandler(FixSessionInfo sessionInfo, FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener, int heartbeatInterval) {
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
        sb = new StringBuilder(1024);

        // begin string buffer is always the same
        ByteBuf newBeginHeader = ctx.alloc().directBuffer();
        newBeginHeader.writeCharSequence(START_HEADER_1, CharsetUtil.US_ASCII);
        newBeginHeader.writeByte(1);
        newBeginHeader.writeCharSequence(START_HEADER_2, CharsetUtil.US_ASCII);
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

        // check if we should read from buffer first
        ByteBuf in;
        ByteBuf localBuffer = buffer;
        if (localBuffer != null) {
            buffer = null;

            // allocate new buffer and copy everything if we don't have enough capacity to store new message
            int msgReadableBytes = msgByteBuf.readableBytes();
            if (!localBuffer.isWritable(msgReadableBytes)) {
                int bufferReadableBytes = localBuffer.readableBytes();

                ByteBuf newBuffer = ctx.alloc().directBuffer(bufferReadableBytes + msgReadableBytes);
                newBuffer.writeBytes(localBuffer);
                localBuffer.release();
                localBuffer = newBuffer;
            }

            localBuffer.writeBytes(msgByteBuf);
            msgByteBuf.release();

            in = localBuffer;
        } else {
            in = msgByteBuf;
        }

        // read until the end
        int index, start, length, msgType;
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
            length = index - start + value.getIntValue() + CHECKSUM_LENGTH; // include checksum

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
                if (quotes.isQuoteIdDefined()) {
                    // TODO: implement Mass Quote Acknowledge
                }
                quotes.release();
            } else {
                // slow path
                switch (msgType) {
                    case FixMsgTypes.HEARTBEAT:
                        Heartbeat heartbeat = Heartbeat.reuseOrCreate();
                        index = GenericCodecUtil.decodeHeartbeat(in, index, value, heartbeat);
                        // just decode it and not do anything else
                        heartbeat.release();
                        break;

                    case FixMsgTypes.TEST:
                        Test test = Test.newInstance();
                        index = GenericCodecUtil.decodeTest(in, index, value, test);
                        // send heartbeat on test message
                        Heartbeat heartbeatForTest = Heartbeat.reuseOrCreate();
                        heartbeatForTest.initBuffer(msgByteBuf);
                        heartbeatForTest.getTestId().setAddress(test.getTestId());
                        heartbeatForTest.setTestIdDefined(true);
                        write(ctx, heartbeatForTest, ctx.voidPromise());
                        ctx.flush();
                        break;

                    case FixMsgTypes.MARKET_DATA_REJECT:
                        MarketDataRequestReject reject = new MarketDataRequestReject();
                        index = GenericCodecUtil.decodeMarketDataRequestReject(in, index, temp, value, reject);
                        fixLogger.status("Market data request with id " + reject.getMdReqId() + " in session " + sessionName + " was rejected: " + reject.getText());
                        break;

                    case FixMsgTypes.LOGON:
                        Logon logon = new Logon();
                        index = GenericCodecUtil.decodeLogon(in, index, temp, value, logon);
                        fixLogger.status("Received logon in session " + sessionName);
                        scheduleHeartbeats(ctx);
                        readyListener.onReady();
                        break;

                    case FixMsgTypes.LOGOUT:
                        Logout logout = new Logout();
                        index = GenericCodecUtil.decodeLogout(in, index, temp, value, logout);
                        fixLogger.status("Received logout in session " + sessionName);
                        cancelHeartbeats();
                        ctx.close();
                        break;

                    default:
                        index = skipUntilChecksum(in, index, value);
                        fixLogger.status("Unknown message type in session " + sessionName + ": " + ((char) msgType));
                }
            }

            // change buffer position
            in.readerIndex(index);
            readableBytes = in.readableBytes();
        }

        // we finished reading from message or buffer fully
        in.release();
    }

    // TODO: ensure on error buffers will not leak
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        byte[] temp = this.temp;

        // prepare buffers
        ByteBuf headBuf = ctx.alloc().directBuffer(64);
        ByteBuf bodyBuf = ctx.alloc().directBuffer(1024);
        int headIndex = headBuf.writerIndex();
        int headStart = headIndex;
        int bodyIndex = bodyBuf.writerIndex();
        int bodyStart = bodyIndex;

        // write static header
        headBuf.writeBytes(beginHeader, 0, START_HEADER_LENGTH);
        headIndex += START_HEADER_LENGTH;

        // write msg type
        bodyIndex = encodeTag(bodyBuf, bodyIndex, FixFields.MSG_TYPE);
        int msgTypeIndex = bodyIndex;
        bodyIndex = encodeDelimiter(bodyBuf, bodyIndex + 1);

        // write sequence number
        bodyIndex = encodeTag(bodyBuf, bodyIndex, FixFields.MSG_SEQ_NUM);
        bodyIndex = encodeIntValue(bodyBuf, bodyIndex, temp, sequenceNumber++);

        // write session info
        sessionHeader.getBytes(0, bodyBuf, bodyIndex, sessionHeaderLength);
        bodyIndex += sessionHeaderLength;

        // write sending time value
        long currentTime = System.currentTimeMillis();
        bodyIndex = encodeDateTime(bodyBuf, bodyIndex, currentTime, sb);

        // write body
        if (msg instanceof Heartbeat) {
            bodyBuf.setByte(msgTypeIndex, FixMsgTypes.HEARTBEAT);

            Heartbeat heartbeat = (Heartbeat) msg;
            bodyIndex = GenericCodecUtil.encodeHeartbeat(bodyBuf, bodyIndex, heartbeat);
            heartbeat.release();
        } else if (msg instanceof MarketDataRequest) {
            bodyBuf.setByte(msgTypeIndex, FixMsgTypes.MARKET_DATA_REQUEST);

            MarketDataRequest marketDataRequest = (MarketDataRequest) msg;
            bodyIndex = GenericCodecUtil.encodeMarketDataRequest(bodyBuf, bodyIndex, temp, marketDataRequest);
        } else if (msg instanceof Logon) {
            bodyBuf.setByte(msgTypeIndex, FixMsgTypes.LOGON);

            Logon logon = (Logon) msg;
            bodyIndex = GenericCodecUtil.encodeLogon(bodyBuf, bodyIndex, temp, logon);
        } else {
            fixLogger.status("Unknown message: " + msg.getClass().getName());
            ReferenceCountUtil.release(msg);
            headBuf.release();
            bodyBuf.release();
            return;
        }

        // write actual body length and finish with head
        int bodyLength = bodyIndex - bodyStart;
        headIndex = encodeIntValue(headBuf, headIndex, temp, bodyLength);
        headBuf.writerIndex(headIndex);

        // calculate checksum
        int sum = 0;
        for (int i = headStart; i < headIndex; i++) {
            byte b = headBuf.getByte(i);
            sum += b;
        }
        for (int i = bodyStart; i < bodyIndex; i++) {
            byte b = bodyBuf.getByte(i);
            sum += b;
        }
        sum %= 256;

        // write checksum and finish with body
        bodyIndex = encodeTag(bodyBuf, bodyIndex, FixFields.CHECK_SUM);
        bodyIndex = encodeThreeDigitInt(bodyBuf, bodyIndex, sum);
        bodyIndex = encodeDelimiter(bodyBuf, bodyIndex);
        bodyBuf.writerIndex(bodyIndex);

        // send to channel
        headBuf.retain();
        bodyBuf.retain();
        ctx.write(headBuf, promise);
        ctx.write(bodyBuf, promise);
        ctx.flush();

        // log outgoing message
        fixLogger.outgoing(headBuf, headStart, headIndex, bodyBuf, bodyStart, bodyIndex);
        headBuf.release();
        bodyBuf.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        fixLogger.status("Error during ");

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
