package io.github.thepun.fix;

import io.netty.channel.nio.NioEventLoopGroup;

public final class MarketDataSessionBuilder {

    private NioEventLoopGroup executor;
    private FixLogger fixLogger;
    private String host;
    private int port;
    private int reconnectInterval;
    private FixConnectListener connectListener;
    private FixDisconnectListener disconnectListener;
    private MarketDataReadyListener readyListener;
    private MarketDataQuotesListener quotesListener;
    private MarketDataSnapshotListener snapshotListener;

    MarketDataSessionBuilder() {
        host = "localhost";
        port = 9999;
        reconnectInterval = 1000;
    }

    public MarketDataSessionBuilder executor(NioEventLoopGroup executor) {
        this.executor = executor;
        return this;
    }

    public MarketDataSessionBuilder logger(FixLogger fixLogger) {
        this.fixLogger = fixLogger;
        return this;
    }

    public MarketDataSessionBuilder host(String host) {
        this.host = host;
        return this;
    }

    public MarketDataSessionBuilder port(int port) {
        this.port = port;
        return this;
    }

    public MarketDataSessionBuilder reconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
        return this;
    }

    public MarketDataSessionBuilder quotesListener(MarketDataQuotesListener quotesListener) {
        this.quotesListener = quotesListener;
        return this;
    }

    public MarketDataSessionBuilder snapshotListener(MarketDataSnapshotListener snapshotListener) {
        this.snapshotListener = snapshotListener;
        return this;
    }

    public MarketDataSessionBuilder readyListener(MarketDataReadyListener readyListener) {
        this.readyListener = readyListener;
        return this;
    }

    public MarketDataSessionBuilder connectListener(FixConnectListener connectListener) {
        this.connectListener = connectListener;
        return this;
    }

    public MarketDataSessionBuilder disconnectListener(FixDisconnectListener disconnectListener) {
        this.disconnectListener = disconnectListener;
        return this;
    }

    public ClientMarketDataSession client() {
        MarketDataQuotesListener localQuotesListener = quotesListener;
        if (localQuotesListener == null) {
            localQuotesListener = e -> {};
        }

        MarketDataSnapshotListener localSnapshotListener = snapshotListener;
        if (localSnapshotListener == null) {
            localSnapshotListener = e -> {};
        }

        MarketDataReadyListener localReadyListener = readyListener;
        if (localReadyListener == null) {
            localReadyListener = e -> {};
        }

        FixConnectListener localConnectListener = connectListener;
        if (localConnectListener == null) {
            localConnectListener = () -> {};
        }

        FixDisconnectListener localDisconnectListener = disconnectListener;
        if (localDisconnectListener == null) {
            localDisconnectListener = () -> {};
        }

        FixLogger localFixLogger = fixLogger;
        if (localFixLogger == null) {
            localFixLogger = NoOpFixLogger.INSTANCE;
        }

        NioEventLoopGroup localExecutor = executor;
        if (localExecutor == null) {
            localExecutor = new NioEventLoopGroup(1);
        }

        return new ClientMarketDataSession(localExecutor, localFixLogger, localQuotesListener, localSnapshotListener, localReadyListener, localConnectListener, localDisconnectListener, host, port, reconnectInterval);
    }
}
