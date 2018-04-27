package io.github.thepun.fix;

import io.netty.util.concurrent.Future;

public interface MarketDataSubscriber {

    Future<Void> subscribe(String symbol, String id);

}
