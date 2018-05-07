package io.github.thepun.fix;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;

public final class PrimeXmClientMarketDataSession {

    private final FixConnectListener connectListener;
    private final FixDisconnectListener disconnectListener;
    private final PrimeXMClientInitializer initializer;
    private final NioEventLoopGroup executor;
    private final FixLogger fixLogger;
    private final String host;
    private final int port;
    private final int reconnectInterval;

    private boolean active;
    private Channel lastChannel;

    PrimeXmClientMarketDataSession(NioEventLoopGroup executor, FixSessionInfo fixSessionInfo, FixLogger fixLogger, MarketDataQuotesListener quotesListener,
                                   MarketDataReadyListener readyListener, FixConnectListener connectListener, FixDisconnectListener disconnectListener,
                                   String host, int port, int reconnectInterval, int heartbeatInterval) {
        this.connectListener = connectListener;
        this.disconnectListener = disconnectListener;
        this.reconnectInterval = reconnectInterval;
        this.fixLogger = fixLogger;
        this.executor = executor;
        this.host = host;
        this.port = port;

        initializer = new PrimeXMClientInitializer(fixSessionInfo, fixLogger, readyListener, quotesListener, heartbeatInterval);
    }

    public synchronized PrimeXmClientMarketDataSession start() {
        if (active) {
            throw new IllegalStateException("Already started");
        }

        active = true;

        fixLogger.status("Start session with " + host + ":" + port);

        reconnect();

        return this;
    }

    public synchronized PrimeXmClientMarketDataSession stop() {
        if (!active) {
            throw new IllegalStateException("Not started");
        }

        active = false;

        fixLogger.status("Stop session with " + host + ":" + port);

        if (lastChannel != null) {
            lastChannel.close();
            lastChannel = null;
        }

        return this;
    }

    // TODO: implement normal wait for stop
    public PrimeXmClientMarketDataSession waitUntilStop() {
        try {
            Thread.sleep(100000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this;
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
            fixLogger.status("Scheduling reconnect to " + host + ":" + port);
            executor.schedule(this::reconnect, reconnectInterval, TimeUnit.SECONDS);

            if (disconnectListener != null) {
                disconnectListener.onDisconnect();
            }
        }
    }

    private synchronized void onConnect(Future<? super Void> f) {
        if (active) {
            if (f.isSuccess()) {
                fixLogger.status("Connection to " + host + ":" + port + " opened");

                if (connectListener != null) {
                    connectListener.onConnnect();
                }
            } else {
                fixLogger.status("Connection to " + host + ":" + port + " failed: " + f.cause().getMessage());
            }
        }
    }


    private static final class PrimeXMClientInitializer extends ChannelInitializer<NioSocketChannel> {

        private final FixLogger fixLogger;
        private final FixSessionInfo fixSessionInfo;
        private final MarketDataReadyListener readyListener;
        private final MarketDataQuotesListener quotesListener;
        private final int heartbeatInterval;

        PrimeXMClientInitializer(FixSessionInfo fixSessionInfo, FixLogger fixLogger,
                                 MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener, int heartbeatInterval) {
            this.fixLogger = fixLogger;
            this.fixSessionInfo = fixSessionInfo;
            this.readyListener = readyListener;
            this.quotesListener = quotesListener;
            this.heartbeatInterval = heartbeatInterval;
        }

        @Override
        protected void initChannel(NioSocketChannel ch) {
            ch.pipeline().addLast(new PrimeXmClientHandler(fixSessionInfo, fixLogger, readyListener, quotesListener, heartbeatInterval));
        }
    }
}
