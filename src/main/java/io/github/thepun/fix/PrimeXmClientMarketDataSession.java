package io.github.thepun.fix;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;

// TODO: check for deadlocks
public final class PrimeXmClientMarketDataSession {

    private final FixConnectListener connectListener;
    private final FixDisconnectListener disconnectListener;
    private final PrimeXmClientInitializer initializer;
    private final NioEventLoopGroup executor;
    private final FixLogger fixLogger;
    private final String host;
    private final int port;
    private final int reconnectInterval;

    private boolean active;
    private boolean connected;
    private Channel lastChannel;

    PrimeXmClientMarketDataSession(NioEventLoopGroup executor, FixSessionInfo fixSessionInfo, FixLogger fixLogger,
                                   MarketDataQuotesListener quotesListener, MarketDataSnapshotListener snapshotListener,
                                   MarketDataReadyListener readyListener, FixConnectListener connectListener, FixDisconnectListener disconnectListener,
                                   String host, int port, int reconnectInterval, int heartbeatInterval) {
        this.connectListener = connectListener;
        this.disconnectListener = disconnectListener;
        this.reconnectInterval = reconnectInterval;
        this.fixLogger = fixLogger;
        this.executor = executor;
        this.host = host;
        this.port = port;

        initializer = new PrimeXmClientInitializer(fixSessionInfo, fixLogger, readyListener, quotesListener, snapshotListener, heartbeatInterval);
    }

    public synchronized void start() {
        if (active) {
            throw new IllegalStateException("Already started");
        }

        fixLogger.status("Start session with " + host + ":" + port);

        active = true;
        connected = false;

        reconnect();
    }

    public synchronized void stop() {
        if (!active) {
            throw new IllegalStateException("Not started");
        }

        fixLogger.status("Stop session with " + host + ":" + port);

        active = false;

        if (lastChannel != null) {
            lastChannel.close().awaitUninterruptibly();
            lastChannel = null;
        }

        connected = false;
    }

    public void send(MarketDataRequest request) {
        Channel channel = lastChannel;
        if (channel == null) {
            fixLogger.status("MarketDataRequest message is dropped: session is not connected");
            return;
        }

        channel.writeAndFlush(request, channel.voidPromise());
    }

    public void send(MassQuoteAcknowledgement massQuoteAcknowledgement) {
        Channel channel = lastChannel;
        if (channel == null) {
            fixLogger.status("MassQuoteAcknowledgement message is dropped: session is not connected");
            return;
        }

        channel.writeAndFlush(massQuoteAcknowledgement, channel.voidPromise());
    }

    private synchronized void reconnect() {
        if (!active) {
            return;
        }

        fixLogger.status("Opening connection to " + host + ":" + port);

        ChannelFuture future = new Bootstrap()
                .group(executor)
                .channel(NioSocketChannel.class)
                .remoteAddress(host, port)
                .handler(initializer)
                .connect();
        future.addListener(this::onConnect);

        Channel channel = future.channel();
        channel.closeFuture().addListener(this::onDisconnect);

        lastChannel = channel;
    }

    private synchronized void onDisconnect(Future<? super Void> f) {
        if (active) {
            if (f.cause() != null) {
                fixLogger.status("Disconnected from " + host + ":" + port + ": " + f.cause().getMessage());
            } else {
                fixLogger.status("Disconnected from " + host + ":" + port);
            }

            executor.schedule(this::reconnect, reconnectInterval, TimeUnit.SECONDS);

            if (connected) {
                connected = false;

                if (disconnectListener != null) {
                    disconnectListener.onDisconnect();
                }
            }
        }
    }

    private synchronized void onConnect(Future<? super Void> f) {
        if (active) {
            if (f.isSuccess()) {
                fixLogger.status("Connection to " + host + ":" + port + " opened");

                connected = true;

                if (connectListener != null) {
                    connectListener.onConnnect();
                }
            } else if (f.cause() != null) {
                fixLogger.status("Connection to " + host + ":" + port + " failed: " + f.cause().getMessage());
            } else {
                fixLogger.status("Connection to " + host + ":" + port + " failed");
            }
        }
    }


    private final class PrimeXmClientInitializer extends ChannelInitializer<NioSocketChannel> {

        private final FixLogger fixLogger;
        private final FixSessionInfo fixSessionInfo;
        private final MarketDataReadyListener readyListener;
        private final MarketDataQuotesListener quotesListener;
        private final MarketDataSnapshotListener snapshotListener;
        private final int heartbeatInterval;

        PrimeXmClientInitializer(FixSessionInfo fixSessionInfo, FixLogger fixLogger, MarketDataReadyListener readyListener,
                                 MarketDataQuotesListener quotesListener, MarketDataSnapshotListener snapshotListener,
                                 int heartbeatInterval) {
            this.fixLogger = fixLogger;
            this.fixSessionInfo = fixSessionInfo;
            this.readyListener = readyListener;
            this.quotesListener = quotesListener;
            this.snapshotListener = snapshotListener;
            this.heartbeatInterval = heartbeatInterval;
        }

        @Override
        protected void initChannel(NioSocketChannel ch) {
            ch.pipeline().addLast(new PrimeXmClientMarketDataHandler(fixSessionInfo, fixLogger, readyListener, quotesListener, snapshotListener, heartbeatInterval));
        }
    }
}
