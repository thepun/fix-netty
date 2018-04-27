package io.github.thepun.fix;

import io.netty.channel.nio.NioEventLoopGroup;

public final class MarketDataSessionBuilder {

    private NioEventLoopGroup executor;
    private FixLogger fixLogger;
    private String host;
    private int port;
    private int reconnectInterval;
    private MarketDataSnapshotListener snapshotListener;
    private MarketDataReadyListener readyListener;
    private FixConnectListener connectListener;
    private FixDisconnectListener disconnectListener;

    MarketDataSessionBuilder() {
        host = "localhost";
        port = 9999;
        reconnectInterval = 1000;
    }

    public MarketDataSessionBuilder setExecutor(NioEventLoopGroup executor) {
        this.executor = executor;
        return this;
    }

    public MarketDataSessionBuilder setFixLogger(FixLogger fixLogger) {
        this.fixLogger = fixLogger;
        return this;
    }

    public MarketDataSessionBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public MarketDataSessionBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public MarketDataSessionBuilder setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
        return this;
    }

    public MarketDataSessionBuilder setSnapshotListener(MarketDataSnapshotListener snapshotListener) {
        this.snapshotListener = snapshotListener;
        return this;
    }

    public MarketDataSessionBuilder setReadyListener(MarketDataReadyListener readyListener) {
        this.readyListener = readyListener;
        return this;
    }

    public MarketDataSessionBuilder setConnectListener(FixConnectListener connectListener) {
        this.connectListener = connectListener;
        return this;
    }

    public MarketDataSessionBuilder setDisconnectListener(FixDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
        return this;
    }

    public MarketDataSession build() {
        if (snapshotListener == null) {
            snapshotListener = DummyListener.INSTANCE;
        }

        if (readyListener == null) {
            readyListener = DummyListener.INSTANCE;
        }

        if (connectListener == null) {
            connectListener = DummyListener.INSTANCE;
        }

        if (disconnectListener == null) {
            disconnectListener = DummyListener.INSTANCE;
        }

        if (fixLogger == null) {
            fixLogger = DummyListener.INSTANCE;
        }

        if (executor == null) {
            executor = new NioEventLoopGroup(1);
        }

        return new MarketDataSession(snapshotListener, readyListener, connectListener, disconnectListener, executor, host, port, reconnectInterval, fixLogger);
    }
}
