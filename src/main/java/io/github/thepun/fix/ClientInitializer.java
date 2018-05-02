package io.github.thepun.fix;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

final class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final FixLogger fixLogger;
    private final FixSessionInfo fixSessionInfo;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;
    private final MarketDataSnapshotListener snapshotListener;

    ClientInitializer(FixSessionInfo fixSessionInfo, FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener, MarketDataSnapshotListener snapshotListener) {
        this.fixLogger = fixLogger;
        this.fixSessionInfo = fixSessionInfo;
        this.readyListener = readyListener;
        this.quotesListener = quotesListener;
        this.snapshotListener = snapshotListener;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        ch.pipeline().addLast(new ClientHandler(fixSessionInfo, fixLogger, readyListener, quotesListener, snapshotListener));
    }
}
