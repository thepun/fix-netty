package io.github.thepun.fix.md;

public interface MarketDataSnapshotListener {
    
    void onMarketData(MarketDataSnapshotFullRefresh snapshot);

}
