package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

final class DummyListener implements FixLogger, FixConnectListener, FixDisconnectListener, MarketDataReadyListener, MarketDataSnapshotListener {

    static final DummyListener INSTANCE = new DummyListener();


    private DummyListener() {
    }

    @Override
    public void onConnnect() {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void status(String message) {

    }

    @Override
    public void incoming(ByteBuf buffer, int offset, int length) {

    }

    @Override
    public void outgoing(ByteBuf buffer, int offset, int length) {

    }

    @Override
    public void onReady(MarketDataSubscriber subscriber) {

    }

    @Override
    public void onMarketData(MarketDataSnapshotFullRefresh snapshot) {

    }
}
