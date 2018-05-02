package io.github.thepun.fix;

final class NoOpQuotesListener implements MarketDataQuotesListener {

    static final NoOpQuotesListener INSTANCE = new NoOpQuotesListener();


    @Override
    public void onMarketData(MassQuote quotes) {

    }
}
