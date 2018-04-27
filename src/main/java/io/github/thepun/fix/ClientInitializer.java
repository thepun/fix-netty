package io.github.thepun.fix;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

final class ClientInitializer extends ChannelInitializer<NioSocketChannel> {

    private final FixLogger fixLogger;
    private final MarketDataReadyListener readyListener;
    private final MarketDataSnapshotListener snapshotListener;

    ClientInitializer(FixLogger fixLogger, MarketDataReadyListener readyListener, MarketDataSnapshotListener snapshotListener) {
        this.fixLogger = fixLogger;
        this.snapshotListener = snapshotListener;
        this.readyListener = readyListener;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        ch.pipeline().addLast(new ClientHandler(fixLogger, readyListener, snapshotListener));
    }
}
