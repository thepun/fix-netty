package io.github.thepun.fix;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

// TODO: check for deadlocks
public final class PrimeXmServerMarketDataSession {

    private final FixConnectListener connectListener;
    private final FixDisconnectListener disconnectListener;
    private final PrimeXmServerInitializer initializer;
    private final NioEventLoopGroup executor;
    private final FixLogger fixLogger;
    private final String host;
    private final int port;

    private boolean active;
    private Channel serverChannel;
    private Channel lastClientChannel;

    PrimeXmServerMarketDataSession(NioEventLoopGroup executor, FixSessionInfo fixSessionInfo, FixLogger fixLogger,
                                   MarketDataRequestListener requestListener, FixConnectListener connectListener, FixDisconnectListener disconnectListener,
                                   String host, int port) {
        this.connectListener = connectListener;

        this.disconnectListener = disconnectListener;
        this.executor = executor;
        this.fixLogger = fixLogger;
        this.host = host;
        this.port = port;

        initializer = new PrimeXmServerInitializer(fixSessionInfo, fixLogger, requestListener);
    }

    public synchronized void start() {
        if (active) {
            throw new IllegalStateException("Already started");
        }

        fixLogger.status("Waiting for session on " + host + ":" + port);

        active = true;

        ChannelFuture future = new ServerBootstrap()
                .group(executor)
                .channel(NioServerSocketChannel.class)
                .localAddress(host, port)
                .childHandler(initializer)
                .bind();
        future.addListener(this::onBind);

        serverChannel = future.channel();
    }

    public synchronized void stop() {
        if (!active) {
            throw new IllegalStateException("Not started");
        }

        fixLogger.status("Dropping session on " + host + ":" + port);

        active = false;

        Channel channel = serverChannel;
        serverChannel = null;

        channel.close().awaitUninterruptibly();

        if (lastClientChannel != null) {
            lastClientChannel.close().awaitUninterruptibly();
        }
    }

    public void send(MassQuote massQuote) {
        Channel channel = lastClientChannel;
        if (channel == null) {
            fixLogger.status("MassQuote message is dropped: session is not connected");
            return;
        }

        channel.writeAndFlush(massQuote, channel.voidPromise());
    }

    public void send(MarketDataRequestReject reject) {
        Channel channel = lastClientChannel;
        if (channel == null) {
            fixLogger.status("MarketDataRequestReject message is dropped: session is not connected");
            return;
        }

        channel.writeAndFlush(reject, channel.voidPromise());
    }

    private synchronized void onBind(Future<? super Void> f) {
        if (!active) {
            return;
        }

        if (f.cause() != null) {
            fixLogger.status("Failed to bind to " + host + ":" + port + ": " + f.cause().getMessage());
        } else {
            fixLogger.status("Failed to bind to " + host + ":" + port);
        }
    }

    private synchronized void onDisconnect(Future<? super Void> f) {
        if (f.cause() != null) {
            fixLogger.status("Disconnected from " + host + ":" + port + ": " + f.cause().getMessage());
        } else {
            fixLogger.status("Disconnected from " + host + ":" + port);
        }

        lastClientChannel = null;

        if (disconnectListener != null) {
            disconnectListener.onDisconnect();
        }
    }


    private final class PrimeXmServerInitializer extends ChannelInitializer<NioSocketChannel> {

        private final FixLogger fixLogger;
        private final FixSessionInfo fixSessionInfo;
        private final MarketDataRequestListener requestListener;

        PrimeXmServerInitializer(FixSessionInfo fixSessionInfo, FixLogger fixLogger, MarketDataRequestListener requestListener) {
            this.fixLogger = fixLogger;
            this.fixSessionInfo = fixSessionInfo;
            this.requestListener = requestListener;
        }

        @Override
        protected void initChannel(NioSocketChannel channel) {
            channel.pipeline().addLast(new PrimeXmServerMarketDataHandler(fixSessionInfo, fixLogger, requestListener));

            synchronized (this) {
                if (!active) {
                    fixLogger.status("Connection to " + host + ":" + port + " ignored from " + channel.remoteAddress() + ": server stopped");
                    channel.close();
                    return;
                } else if (lastClientChannel != null) {
                    fixLogger.status("Connection to " + host + ":" + port + " ignored from " + channel.remoteAddress() + ": session is already connected");
                    channel.close();
                    return;
                }

                fixLogger.status("Connection to " + host + ":" + port + " accepted from " + channel.remoteAddress());

                channel.closeFuture().addListener(PrimeXmServerMarketDataSession.this::onDisconnect);
                lastClientChannel = channel;

                if (connectListener != null) {
                    connectListener.onConnnect();
                }
            }
        }
    }
}
