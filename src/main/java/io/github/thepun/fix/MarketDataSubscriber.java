package io.github.thepun.fix;

public interface MarketDataSubscriber {

    void subscribe(String id, String symbol, int marketDepth);

}
