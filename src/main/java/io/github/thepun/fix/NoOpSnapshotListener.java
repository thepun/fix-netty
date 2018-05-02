package io.github.thepun.fix;

final class NoOpSnapshotListener implements MarketDataSnapshotListener {

    static final NoOpSnapshotListener INSTANCE = new NoOpSnapshotListener();

    @Override
    public void onMarketData(MarketDataSnapshotFullRefresh snapshot) {

    }
}
