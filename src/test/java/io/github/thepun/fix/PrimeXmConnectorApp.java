package io.github.thepun.fix;

import io.netty.util.ResourceLeakDetector;

import java.util.concurrent.CountDownLatch;

public class PrimeXmConnectorApp {

    private static PrimeXmClientMarketDataSession client;
    private static PrimeXmServerMarketDataSession server;
    private static CountDownLatch quoteLatch;

    public static void main(String[] args) throws InterruptedException {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);


        client = new MarketDataSessionBuilder()
                .logger(new ConsoleLogger("client"))
                .host("localhost")
                .port(31744)
                .senderCompId("Q156")
                .targetCompId("XCD1")
                .username("primexm_scope_uat_2_q")
                .password("TGes7daACUgROxvA")
                .readyListener(() -> {
                    System.out.println("Ready");

                    MarketDataRequest request = new MarketDataRequest();
                    request.setMarketDepth(0);
                    request.setMdReqId("e/u");
                    request.setRelatedSymCount(1);
                    request.getRelatedSym(0).setSymbol("EUR/USD");
                    request.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE);

                    client.send(request);
                })
                .quotesListener(quotes -> {
                    MassQuote.QuoteSet quoteSet = quotes.getQuoteSet(0);
                    MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
                    System.out.println("MassQuote(" + quoteSet.getQuoteSetId().toString() + "): bid=" + entry.getBidSpotRate() + " ask=" + entry.getOfferSpotRate());

                    if (quotes.isQuoteIdDefined()) {
                        MassQuoteAcknowledgement massQuoteAcknowledgement = MassQuoteAcknowledgement.reuseOrCreate();
                        massQuoteAcknowledgement.initBuffer(quotes.getBuffer());
                        massQuoteAcknowledgement.getQuoteId().setAddress(quotes.getQuoteId());
                        massQuoteAcknowledgement.setQuoteIdDefined(true);
                        client.send(massQuoteAcknowledgement);
                    }
                })
                .snapshotListener(snapshot -> {
                    MarketDataSnapshotFullRefresh.MDEntry entry1 = snapshot.getEntry(0);
                    MarketDataSnapshotFullRefresh.MDEntry entry2 = snapshot.getEntry(1);
                    System.out.println("MarketDataSnapshotFullRefresh(" + snapshot.getMdReqId().toString() + "): bid=" + entry1.getMdEntryPX() + " ask=" + entry2.getMdEntryPX());

                })
                .primeXmClient();

        client.start();

        Thread.sleep(1000000);
    }

}
