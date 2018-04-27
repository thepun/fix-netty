package io.github.thepun.fix;

public interface MarketDataSnapshotListener {
    
    void onMarketData(MarketDataSnapshotFullRefresh snapshot);

}
