package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;

import java.util.concurrent.CountDownLatch;

public class App {

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
                .logger(new ConsoleLogger("client"))
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
                .primeXmClient();

        server = new MarketDataSessionBuilder()
                .logger(new ConsoleLogger("server"))
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
        long quotSetIdIndex = index;
        index += quoteSetId.length();
        String quoteEntryId = "quoteEntry";
        buffer.writeCharSequence(quoteEntryId, CharsetUtil.US_ASCII);
        long quoteEntryIdIndex = index;

        for (int k = 0; k < 100; k++) {
            quoteLatch = new CountDownLatch(quoteCount);
            start = System.currentTimeMillis();
            for (int i = 0; i < quoteCount; i++) {
                MassQuote quote = MassQuote.reuseOrCreate();
                quote.initBuffer(buffer);
                quote.initQuoteSets(1);
                quote.getQuoteId().setAddress(quoteIdIndex, quoteId.length());
                quote.setQuoteIdDefined(true);

                MassQuote.QuoteSet quoteSet = quote.getQuoteSet(0);
                quoteSet.initEntries(1);
                quoteSet.getQuoteSetId().setAddress(quotSetIdIndex, quoteSetId.length());
                quoteSet.setQuoteSetIdDefined(true);

                MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
                entry.getQuoteEntryId().setAddress(quoteEntryIdIndex, quoteEntryId.length());
                entry.setQuoteEntryIdDefined(true);
                entry.setBidSize(1000);
                entry.setOfferSize(2000);
                entry.setBidSpotRate(11234);
                entry.setOfferSpotRate(11235);

                server.send(quote);
            }
            quoteLatch.await();
            finish = System.currentTimeMillis();

            System.out.println("Finished: " + (finish - start) + "ms");
            System.gc();
            Thread.sleep(3000);
        }
    }


}
