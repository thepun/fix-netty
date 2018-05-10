package io.github.thepun.fix;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.github.thepun.fix.FixHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PrimeXmClientHandlerTest {

    private FixSessionInfo fixSessionInfo;
    private MarketDataReadyListener readyListener;
    private MarketDataQuotesListener quotesListener;
    private PrimeXmClientMarketDataHandler handler;

    @BeforeEach
    void prepareMocks() {
        readyListener = mock(MarketDataReadyListener.class);
        quotesListener = mock(MarketDataQuotesListener.class);
        fixSessionInfo = new FixSessionInfo("qwe_", "1asd", "+--341", "sdf", "user", "pass");
        handler = new PrimeXmClientMarketDataHandler(fixSessionInfo, NoOpFixLogger.INSTANCE, readyListener, quotesListener, 30);
    }

    @Test
    void logonOnChannelActivation() {
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        String fix = readFixMessageFromChannel(channel);
        assertFixMathes("8=FIX.4.4|9=<ANY>|35=A|34=1|49=qwe_|56=+--341|50=1asd|57=sdf|52=<ANY>|98=0|108=10|141=Y|553=user|554=pass|10=<ANY>|", fix);

        assertNull(channel.readOutbound());
    }

    @Test
    void subscribeOnSuccessfulLogon() {
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        readFixMessageFromChannel(channel);

        // TODO: add time to fix message
        writeFixMessageToChannel(channel, "8=FIX.4.4|9=104|35=A|34=0|49=qwe_|56=+--341|50=1asd|57=sdf|52=20180505-10:24:40.406|98=0|108=10|141=Y|553=user|554=pass|10=064|");
        channel.checkException();

        verify(readyListener, times(1)).onReady();
    }

    /*@Test
    void sendSubscriptions() {
        doAnswer(a -> {
            MarketDataRequest marketDataRequest1 = new MarketDataRequest();
            marketDataRequest1.setMdReqId("req1");
            marketDataRequest1.setMarketDepth(3);
            marketDataRequest1.setRelatedSymCount(1);
            marketDataRequest1.setStreamReference("p1");
            marketDataRequest1.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE);
            marketDataRequest1.getRelatedSym(0).setSymbol("EURUSD");

            MarketDataRequest marketDataRequest2 = new MarketDataRequest();
            marketDataRequest2.setMdReqId("req1");
            marketDataRequest2.setMarketDepth(4);
            marketDataRequest2.setRelatedSymCount(1);
            marketDataRequest2.setStreamReference("p2");
            marketDataRequest2.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE);
            marketDataRequest2.getRelatedSym(0).setSymbol("EURCAD");

            MarketDataSubscriber subscriber = a.getArgument(0);
            subscriber.subscribe(marketDataRequest1);
            subscriber.subscribe(marketDataRequest2);
            return null;
        }).when(readyListener).onReady(any());

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        readFixMessageFromChannel(channel);
        writeFixMessageToChannel(channel, "8=FIX.4.4|9=104|35=A|34=0|49=qwe_|56=+--341|50=1asd|57=sdf|52=20180505-10:24:40.406|98=0|108=10|141=Y|553=user|554=pass|10=064|");
        verify(readyListener, times(1)).onReady(any());

        String fix1 = readFixMessageFromChannel(channel);
        assertFixMathes("8=FIX.4.4|9=<ANY>|35=V|34=2|49=qwe_|56=+--341|50=1asd|57=sdf|52=<ANY>|262=req1|263=1|264=3|146=1|55=EURUSD|10=<ANY>|", fix1);

        String fix2 = readFixMessageFromChannel(channel);
        assertFixMathes("8=FIX.4.4|9=<ANY>|35=V|34=3|49=qwe_|56=+--341|50=1asd|57=sdf|52=<ANY>|262=req1|263=1|264=4|146=1|55=EURCAD|10=<ANY>|", fix2);
    }*/

    @Test
    void receiveQuotes() {
        ArgumentCaptor<MassQuote> captor = ArgumentCaptor.forClass(MassQuote.class);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        readFixMessageFromChannel(channel);
        writeFixMessageToChannel(channel, "8=FIX.4.4|9=104|35=A|34=0|49=qwe_|56=+--341|50=1asd|57=sdf|52=20180505-10:24:40.406|98=0|108=10|141=Y|553=user|554=pass|10=064|");
        writeFixMessageToChannel(channel, "8=FIX.4.4|9=144|35=i|34=1|49=qwe_|56=+--341|50=1asd|57=sdf|52=20151105-12:30:53.466|296=1|302=43|295=1|299=0|106=1|134=1000000|135=50000|188=186.129|190=186.14|10=132|");
        verify(quotesListener, times(1)).onMarketData(captor.capture());

        MassQuote quotes = captor.getValue();
        assertFalse(quotes.isQuoteIdDefined());
        assertEquals(1, quotes.getQuoteSetCount());
        MassQuote.QuoteSet quoteSet = quotes.getQuoteSet(0);
        assertTrue(quoteSet.isQuoteSetIdDefined());
        assertEquals("43", quoteSet.getQuoteSetId().toString());
        assertEquals(1, quoteSet.getEntryCount());
        MassQuote.QuoteEntry entry = quoteSet.getEntry(0);
        assertTrue(entry.isIssuerDefined());
        assertEquals("1", entry.getIssuer().toString());
        assertTrue(entry.isQuoteEntryIdDefined());
        assertEquals("0", entry.getQuoteEntryId().toString());
    }
}
