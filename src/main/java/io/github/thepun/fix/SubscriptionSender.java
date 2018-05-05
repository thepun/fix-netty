package io.github.thepun.fix;

import io.netty.channel.Channel;

final class SubscriptionSender implements MarketDataSubscriber {

    private final Channel channel;

    private volatile boolean enabled;

    SubscriptionSender(Channel channel) {
        this.channel = channel;

        enabled = true;
    }

    @Override
    public void subscribe(MarketDataRequest request) {
        if (!enabled) {
            throw new IllegalStateException("Subscriber is not active");
        }

        channel.writeAndFlush(request, channel.voidPromise());
    }

    void disable() {
        enabled = false;
    }
}
