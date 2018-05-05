package io.github.thepun.fix;

public class App {

    public static void main(String[] args) throws InterruptedException {
        new MarketDataSessionBuilder()
                .port(9599)
                .senderCompId("local")
                .senderSubId("local")
                .targetCompId("local")
                .targetSubId("local")
                .username("local")
                .password("local")
                .connectListener(() -> {
                    System.out.println("Connected");
                })
                .disconnectListener(() -> {
                    System.out.println("Disconnect");
                })
                .readyListener(subscriber -> {
                    System.out.println("Ready");

                    MarketDataRequest request = new MarketDataRequest();
                    request.setMarketDepth(0);
                    request.setMdReqId("sub");
                    request.setRelatedSymCount(1);
                    request.getRelatedSym(0).setSymbol("EURUSD");
                    request.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE );
                    subscriber.subscribe(request);
                })
                .quotesListener(quotes -> {
                    MassQuote.QuoteSet quoteSet = quotes.getQuoteSet(0);
                    MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
                    System.out.println("Quote(" + quoteSet.getQuoteSetId().toString() + "): bid=" + entry.getBidSpotRate() + " ask=" + entry.getOfferSpotRate());
                })
                .primeXmClient()
                .start();

        Thread.sleep(100000000);
    }

}
