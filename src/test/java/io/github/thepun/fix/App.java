package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class App {

    public static void main(String[] args) throws InterruptedException {
        new MarketDataSessionBuilder()
                .logger(new ConsoleLogger())
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


    private static final class ConsoleLogger implements FixLogger {

        @Override
        public void status(String status) {
            System.out.println("Status: " + status);
        }

        @Override
        public void incoming(ByteBuf buffer, int offset, int length) {
            CharSequence text = buffer.getCharSequence(offset, length, CharsetUtil.US_ASCII);
            System.out.println("Incoming: " + text);
        }

        @Override
        public void outgoing(ByteBuf first, int firstOffset, int firstLength, ByteBuf second, int secondOffset, int secondLength) {
            CharSequence text1 = first.getCharSequence(firstOffset, firstLength, CharsetUtil.US_ASCII);
            CharSequence text2 = second.getCharSequence(secondOffset, secondLength, CharsetUtil.US_ASCII);
            System.out.println("Outgoing: " + text1 + text2);
        }
    }
}
