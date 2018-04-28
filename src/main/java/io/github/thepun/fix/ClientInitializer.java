package io.github.thepun.fix;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

final class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final FixLogger fixLogger;
    private final MarketDataReadyListener readyListener;
    private final MarketDataQuotesListener quotesListener;
    private final MarketDataSnapshotListener snapshotListener;

    ClientInitializer(FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataQuotesListener quotesListener, MarketDataSnapshotListener snapshotListener) {
        this.fixLogger = fixLogger;
        this.readyListener = readyListener;
        this.quotesListener = quotesListener;
        this.snapshotListener = snapshotListener;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        ch.pipeline().addLast(new ClientHandler(fixLogger, readyListener, quotesListener, snapshotListener));
    }
}
