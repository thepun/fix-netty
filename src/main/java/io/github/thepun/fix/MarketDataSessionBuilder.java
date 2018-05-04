package io.github.thepun.fix;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.FastThreadLocalThread;

import java.util.concurrent.atomic.AtomicInteger;

public final class MarketDataSessionBuilder {

    private static final AtomicInteger DEFAULT_THREAD_COUNTER = new AtomicInteger(1);


    private String host;
    private int port;
    private int reconnectInterval;
    private FixLogger fixLogger;
    private String senderCompId;
    private String senderSubId;
    private String targetCompId;
    private String targetSubId;
    private String username;
    private String password;
    private FixConnectListener connectListener;
    private FixDisconnectListener disconnectListener;
    private MarketDataReadyListener readyListener;
    private MarketDataQuotesListener quotesListener;
    private MarketDataSnapshotListener snapshotListener;
    private NioEventLoopGroup executor;

    public MarketDataSessionBuilder() {
        host = "localhost";
        port = 9999;
        reconnectInterval = 1000;
    }

    public MarketDataSessionBuilder senderCompId(String senderCompId) {
        this.senderCompId = senderCompId;
        return this;
    }

    public MarketDataSessionBuilder senderSubId(String senderSubId) {
        this.senderSubId = senderSubId;
        return this;
    }

    public MarketDataSessionBuilder targetCompId(String targetCompId) {
        this.targetCompId = targetCompId;
        return this;
    }

    public MarketDataSessionBuilder targetSubId(String targetSubId) {
        this.targetSubId = targetSubId;
        return this;
    }

    public MarketDataSessionBuilder username(String username) {
        this.username = username;
        return this;
    }

    public MarketDataSessionBuilder password(String password) {
        this.password = password;
        return this;
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

    public PrimeXMClientMarketDataSession primeXmClient() {
        if (readyListener == null) {
            throw new IllegalStateException("Empty ready listener");
        }

        if (quotesListener == null) {
            throw new IllegalStateException("Empty quote listener");
        }

        FixLogger localFixLogger = fixLogger;
        if (localFixLogger == null) {
            localFixLogger = NoOpFixLogger.INSTANCE;
        }

        NioEventLoopGroup localExecutor = executor;
        if (localExecutor == null) {
            localExecutor = new NioEventLoopGroup(1, r -> {
                FastThreadLocalThread thread = new FastThreadLocalThread(r);
                thread.setName("market-data-" + DEFAULT_THREAD_COUNTER.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            });
        }

        FixSessionInfo fixSessionInfo = new FixSessionInfo(senderCompId, senderSubId, targetCompId, targetSubId, username, password);

        return new PrimeXMClientMarketDataSession(localExecutor, fixSessionInfo, localFixLogger, quotesListener, readyListener, connectListener, disconnectListener, host, port, reconnectInterval);
    }

    public PrimeXMServerMarketDataSession primeXmServer() {
        return null;
    }
}
