package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

final class SubscriptionSender implements MarketDataSubscriber {

    private final ChannelHandlerContext ctx;

    private volatile boolean enabled;

    SubscriptionSender(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void subscribe(String id, String symbol, int marketDepth) {
        if (!enabled) {
            throw new IllegalStateException("Subscriber is not active");
        }

        ByteBuf buffer = ctx.alloc().directBuffer();
        // TODO: write encoding for market data request

        ctx.writeAndFlush(buffer, ctx.voidPromise());
    }

    void disable() {
        enabled = false;
    }
}
