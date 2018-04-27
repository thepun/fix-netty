package io.github.thepun.fix;

import io.netty.util.concurrent.Future;

final class SubscriptionSender implements MarketDataSubscriber {

    @Override
    public Future<Void> subscribe(String symbol, String id) {
        return null;
    }

    void processFirstSnapshot() {

    }

    void processReject() {

    }
}
