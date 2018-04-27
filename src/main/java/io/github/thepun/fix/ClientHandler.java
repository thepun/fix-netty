package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static io.github.thepun.fix.FixDecodingUtils.*;

final class ClientHandler extends ChannelInboundHandlerAdapter {

    private final Logon logon;
    private final Logout logout;
    private final MarketDataRequestReject reject;

    private final FixLogger fixLogger;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;

    private ByteBuf buffer;
    private String sessionName;

    ClientHandler(FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener) {
        this.fixLogger = fixLogger;
        this.readyListener = readyListener;
        this.quotesListener = quotesListener;

        logon = new Logon();
        logout = new Logout();
        reject = new MarketDataRequestReject();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        sessionName = ctx.channel().toString();
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf msgByteBuf = (ByteBuf) msg;
            readMessage(ctx, msgByteBuf);
        } else {
            fixLogger.status("Unknown message: " + msg.getClass().getName());
        }
    }

    private void readMessage(ChannelHandlerContext ctx, ByteBuf msgByteBuf) {
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

        int start;
        int length;
        int msgType;

        // prepare cursor object
        FixDecodingCursor cursor = new FixDecodingCursor();
        cursor.setIn(in);
        cursor.setIndex(in.readerIndex());
        cursor.setCount(in.readableBytes());

        // read until the end
        while (cursor.getIndex() < cursor.getCount()) {
            // remember message start
            start = cursor.getIndex();

            // read fix
            decodeTag(cursor);
            ensureTag(cursor, Fields.BEGIN_STRING);
            cursor.setIndex(cursor.getIndex() + 8);

            // read length
            decodeTag(cursor);
            ensureTag(cursor, Fields.BODY_LENGTH);
            decodeIntValue(cursor);
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

            // log message
            fixLogger.incoming(in, start, length);

            // read message type
            decodeTag(cursor);
            ensureTag(cursor, Fields.MSG_TYPE);
            decodeStrValueAsInt(cursor);
            msgType = cursor.getStrAsInt();

            // read session info (SenderCompId | TargetCompId | SenderSubId | TargetSubId | MsgSeqNum | SendingTime)
            for (int i = 0; i < 6; i++) {
                skipTagAndValue(cursor);
            }

            // read message content
            switch (msgType) {
                case MsgTypes.MASS_QUOTE:
                    MassQuote quotes = MassQuote.newInstance();
                    decodeMassQuote(cursor, quotes);
                    //quotes.setMessageBuffer(in);
                    quotesListener.onMarketData(quotes);
                    break;

                /*case MsgTypes.MARKET_DATA_SNAPSHOT_FULL_REFRESH:
                    MarketDataSnapshotFullRefresh snapshot = MarketDataSnapshotFullRefresh.newInstance();
                    decodeMarketDataSnapshotFullRefresh(cursor, snapshot);
                    snapshot.setMessageBuffer(in);
                    snapshotListener.onMarketData(snapshot);
                    break;*/

                case MsgTypes.MARKET_DATA_REJECT:
                    decodeMarketDataReject(cursor, reject);
                    fixLogger.status("Market data request with id " + reject.getMdReqID() + " in session " + sessionName + " was rejected: " + reject.getText());
                    break;

                case MsgTypes.LOGON:
                    decodeLogon(cursor, logon);
                    fixLogger.status("Logon in session " + sessionName);
                    SubscriptionSender subscriptionSender = new SubscriptionSender(ctx);
                    readyListener.onReady(subscriptionSender);
                    subscriptionSender.disable();
                    break;

                case MsgTypes.LOGOUT:
                    decodeLogout(cursor, logout);
                    fixLogger.status("Logout in session " + sessionName);
                    ctx.close();
                    break;

                default:
                    fixLogger.status("Unknown message type in session " + sessionName + ": " + msgType);
            }
        }
    }
}
