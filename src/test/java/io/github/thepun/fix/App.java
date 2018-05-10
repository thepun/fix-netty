package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
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
        int quoteCount = 1000;

        //System.setProperty("io.netty.allocator.type", "unpooled");
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

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer();
        long index = buffer.memoryAddress() + buffer.readerIndex();

        MassQuote quote = MassQuote.reuseOrCreate();
        quote.initBuffer(buffer);
        quote.initQuoteSets(1);
        String quoteId = "quote";
        buffer.writeCharSequence(quoteId, CharsetUtil.US_ASCII);
        quote.getQuoteId().setAddress(index, quoteId.length());
        quote.setQuoteIdDefined(true);
        index += quoteId.length();

        MassQuote.QuoteSet quoteSet = quote.getQuoteSet(0);
        quoteSet.initEntries(1);
        String quoteSetId = "quoteSet";
        buffer.writeCharSequence(quoteSetId, CharsetUtil.US_ASCII);
        quoteSet.getQuoteSetId().setAddress(index, quoteSetId.length());
        quoteSet.setQuoteSetIdDefined(true);
        index += quoteSetId.length();

        MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
        String quoteEntrytId = "quoteEntry";
        buffer.writeCharSequence(quoteEntrytId, CharsetUtil.US_ASCII);
        entry.getQuoteEntryId().setAddress(index, quoteEntrytId.length());
        entry.setQuoteEntryIdDefined(true);
        entry.setBidSize(1000);
        entry.setOfferSize(2000);
        entry.setBidSpotRate(11234);
        entry.setOfferSpotRate(11235);

        // first
        quoteLatch = new CountDownLatch(quoteCount);
        for (int i = 0; i < quoteCount; i++) {
            quote.retain();
            server.send(quote);
        }
        quoteLatch.await();

        Thread.sleep(1000);

        // second
        quoteLatch = new CountDownLatch(quoteCount);
        start = System.currentTimeMillis();
        for (int i = 0; i < quoteCount; i++) {
            quote.retain();
            server.send(quote);
        }
        quoteLatch.await();
        finish = System.currentTimeMillis();

        System.out.println("Finished: " + (finish - start) + "ms");

        System.gc();

    }


}
