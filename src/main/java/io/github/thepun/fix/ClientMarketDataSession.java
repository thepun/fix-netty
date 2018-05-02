package io.github.thepun.fix;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;

public final class ClientMarketDataSession {

    public static MarketDataSessionBuilder newBuilder() {
        return new MarketDataSessionBuilder();
    }


    private final FixConnectListener connectListener;
    private final FixDisconnectListener disconnectListener;
    private final ClientInitializer initializer;
    private final NioEventLoopGroup executor;
    private final FixLogger fixLogger;
    private final String host;
    private final int port;
    private final int reconnectInterval;

    private boolean active;
    private Channel lastChannel;

    ClientMarketDataSession(NioEventLoopGroup executor, FixSessionInfo fixSessionInfo, FixLogger fixLogger, MarketDataQuotesListener quotesListener, MarketDataSnapshotListener snapshotListener,
                            MarketDataReadyListener readyListener, FixConnectListener connectListener, FixDisconnectListener disconnectListener, String host, int port, int reconnectInterval) {
        this.connectListener = connectListener;
        this.disconnectListener = disconnectListener;
        this.reconnectInterval = reconnectInterval;
        this.fixLogger = fixLogger;
        this.executor = executor;
        this.host = host;
        this.port = port;

        initializer = new ClientInitializer(fixSessionInfo, fixLogger, readyListener, quotesListener, snapshotListener);
    }

    public synchronized void start() {
        if (active) {
            throw new IllegalStateException("Already started");
        }

        active = true;

        fixLogger.status("Start session with " + host + ":" + port);

        reconnect();
    }

    public synchronized void stop() {
        if (!active) {
            throw new IllegalStateException("Not started");
        }

        active = false;

        fixLogger.status("Stop session with " + host + ":" + port);

        if (lastChannel != null) {
            lastChannel.close();
            lastChannel = null;
        }
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
            executor.schedule(this::reconnect, reconnectInterval, TimeUnit.MILLISECONDS);

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
}
