package io.github.thepun.fix;

public class App {

    private static PrimeXmClientMarketDataSession client;
    private static PrimeXmServerMarketDataSession server;

    public static void main(String[] args) {
        client = new MarketDataSessionBuilder()
                .logger(new ConsoleLogger())
                .host("localhost")
                .port(12345)
                .senderCompId("local")
                .targetCompId("local")
                .username("local")
                .password("local")
                .connectListener(() -> {
                    System.out.println("Connected client");
                })
                .disconnectListener(() -> {
                    System.out.println("Disconnect client");
                })
                .readyListener(() -> {
                    System.out.println("Ready");

                    MarketDataRequest request = new MarketDataRequest();
                    request.setMarketDepth(0);
                    request.setMdReqId("sub");
                    request.setRelatedSymCount(1);
                    request.getRelatedSym(0).setSymbol("EUR/USD");
                    request.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE);

                    client.send(request);
                })
                .quotesListener(quotes -> {
                    MassQuote.QuoteSet quoteSet = quotes.getQuoteSet(0);
                    MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
                    System.out.println("Quote(" + quoteSet.getQuoteSetId().toString() + "): bid=" + entry.getBidSpotRate() + " ask=" + entry.getOfferSpotRate());
                    quotes.release();
                })
                .primeXmClient();

        server = new MarketDataSessionBuilder()
                .logger(new ConsoleLogger())
                .host("localhost")
                .port(12345)
                .senderCompId("local")
                .targetCompId("local")
                .username("local")
                .password("local")
                .connectListener(() -> {
                    System.out.println("Connected server");
                })
                .disconnectListener(() -> {
                    System.out.println("Disconnect server");
                })
                .subscribeListener(subscribe -> {
                    Object o = null;
                })
                .primeXmServer();

        server.start();
        client.start();
    }


}
