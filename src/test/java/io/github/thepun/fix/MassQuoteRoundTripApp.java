package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;

import java.util.concurrent.CountDownLatch;

public class MassQuoteRoundTripApp {

    private static long start;
    private static long finish;
    private static PrimeXmClientMarketDataSession client;
    private static PrimeXmServerMarketDataSession server;
    private static CountDownLatch quoteLatch;

    public static void main(String[] args) throws InterruptedException {
        int quoteCount = 1000000;

        System.setProperty("io.netty.recycler.linkCapacity", "10000000");
        System.setProperty("io.netty.recycler.ratio", "0");
        //System.setProperty("io.netty.leakDetection.targetRecords", "100");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        CountDownLatch subscriptionLatch = new CountDownLatch(1);

        client = new MarketDataSessionBuilder()
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
                    quoteLatch.countDown();
                    /*MassQuote.QuoteSet quoteSet = quotes.getQuoteSet(0);
                    MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
                    System.out.println("Quote(" + quoteSet.getQuoteSetId().toString() + "): bid=" + entry.getBidSpotRate() + " ask=" + entry.getOfferSpotRate());
                    quotes.release();*/
                })
                .snapshotListener(snapshot -> {
                    quoteLatch.countDown();
                })
                .primeXmClient();

        server = new MarketDataSessionBuilder()
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
                    System.out.println("Subscription");
                    subscriptionLatch.countDown();
                })
                .primeXmServer();

        server.start();
        client.start();
        subscriptionLatch.await();

        ByteBuf buffer = Unpooled.directBuffer();
        long index = buffer.memoryAddress() + buffer.readerIndex();
        String quoteId = "quote";
        buffer.writeCharSequence(quoteId, CharsetUtil.US_ASCII);
        long quoteIdIndex = index;
        index += quoteId.length();
        String quoteSetId = "quoteSet";
        buffer.writeCharSequence(quoteSetId, CharsetUtil.US_ASCII);
        long quoteSetIdIndex = index;
        index += quoteSetId.length();
        String quoteEntryId = "quoteEntry";
        buffer.writeCharSequence(quoteEntryId, CharsetUtil.US_ASCII);
        long quoteEntryIdIndex = index;
        index += quoteEntryId.length();
        String symbol = "EUR/USD";
        buffer.writeCharSequence(symbol, CharsetUtil.US_ASCII);
        long symbolIndex = index;

        for (int k = 0; k < 100; k++) {
            quoteLatch = new CountDownLatch(quoteCount);
            start = System.currentTimeMillis();

            for (int i = 0; i < quoteCount; i++) {
                /*if (i % 2 == 0) {

                    MassQuote quote = MassQuote.reuseOrCreate();
                    quote.initBuffer(buffer);
                    quote.initQuoteSets(1);
                    quote.getQuoteId().setAddress(quoteIdIndex, quoteId.length());
                    quote.setQuoteIdDefined(true);

                    MassQuote.QuoteSet quoteSet = quote.getQuoteSet(0);
                    quoteSet.initEntries(1);
                    quoteSet.getQuoteSetId().setAddress(quoteSetIdIndex, quoteSetId.length());
                    quoteSet.setQuoteSetIdDefined(true);

                    MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
                    entry.getQuoteEntryId().setAddress(quoteEntryIdIndex, quoteEntryId.length());
                    entry.setQuoteEntryIdDefined(true);
                    entry.setBidSize(1000);
                    entry.setOfferSize(2000);
                    entry.setBidSpotRate(11234);
                    entry.setOfferSpotRate(11235);

                    server.send(quote);
                } else {*/
                    MarketDataSnapshotFullRefresh quote = MarketDataSnapshotFullRefresh.reuseOrCreate();
                    quote.initBuffer(buffer);
                    quote.initEntries(2);
                    quote.getMdReqId().setAddress(quoteIdIndex, quoteId.length());
                    quote.setMdReqIdDefined(true);
                    quote.getSymbol().setAddress(symbolIndex, symbol.length());
                    quote.setSymbolDefined(true);

                    MarketDataSnapshotFullRefresh.MDEntry bid = quote.getEntry(0);
                    bid.setMdEntryType(FixEnums.MD_ENTRY_TYPE_BID);
                    bid.setMdEntrySize(1000);
                    bid.setMdEntryPX(11234);
                    bid.getId().setAddress(quoteSetIdIndex, quoteSetId.length());
                    bid.setIdDefined(true);
                    bid.getIssuer().setAddress(quoteEntryIdIndex, quoteEntryId.length());
                    bid.setIssuerDefined(true);

                    MarketDataSnapshotFullRefresh.MDEntry ask = quote.getEntry(1);
                    ask.setMdEntryType(FixEnums.MD_ENTRY_TYPE_BID);
                    ask.setMdEntrySize(2000);
                    ask.setMdEntryPX(11235);
                    ask.getId().setAddress(quoteSetIdIndex, quoteSetId.length());
                    ask.setIdDefined(true);
                    ask.getIssuer().setAddress(quoteEntryIdIndex, quoteEntryId.length());
                    ask.setIssuerDefined(true);

                    server.send(quote);
               // }
            }

            quoteLatch.await();
            finish = System.currentTimeMillis();

            System.out.println("Finished: " + (finish - start) + "ms");
            System.gc();
            Thread.sleep(3000);
        }
    }


}
