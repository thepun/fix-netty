package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static org.junit.jupiter.api.Assertions.*;

class PrimeXmCodecUtilTest {

    /* @BeforeEach
    void prepare() {
        byte[] temp = new byte[1024];
        ByteBuf buffer = Unpooled.directBuffer(1024);
        cursor = new Cursor();
        cursor.setTemp(new byte[1024]);
        CommonCodecUtil.startDecoding(cursor, buffer, temp);
    }*/

    /*@Test
    void decodeMassQuote() {
        String fix = "296=1|302=43|295=1|299=0|106=1|134=1000000|135=50000|188=186.129|190=186.14|299=1|10=132|";

        MassQuote massQuote = MassQuote.reuseOrCreate();
        PrimeXmCodecUtil.decodeMassQuote(prepareCursor(fix), massQuote);

        assertFalse(massQuote.isQuoteIdDefined());
        assertEquals(1, massQuote.getQuoteSetCount());

        MassQuote.QuoteSet quoteSet = massQuote.getQuoteSet(0);
        assertNotNull(quoteSet);
        assertEquals(1, quoteSet.getEntryCount());
        assertEquals("43", quoteSet.getQuoteSetId().toString());

        MassQuote.QuoteEntry quoteSetEntry = quoteSet.getEntry(0);
        assertTrue(quoteSetEntry.isIssuerDefined());
        assertEquals("1", quoteSetEntry.getIssuer().toString());
        assertEquals("0", quoteSetEntry.getQuoteEntryId().toString());
        assertEquals(1000000, quoteSetEntry.getBidSize());
        assertEquals(186.129, quoteSetEntry.getBidSpotRate());
        assertEquals(50000, quoteSetEntry.getOfferSize());
        assertEquals(186.14, quoteSetEntry.getOfferSpotRate());
    }*/

    /*@Test
    void decodeMarketDataRequest() {
        // TODO: market data request decode test
    }*/

   /* @Test
    void decodeMarketDataRequestReject() {
        // TODO: market data request reject decode test
    }*/

    /*@Test
    void encodeMarketDataRequest() {
        MarketDataRequest marketDataRequest = new MarketDataRequest();
        marketDataRequest.setMdReqId("asdfghrty");
        marketDataRequest.setMarketDepth(99);
        marketDataRequest.setRelatedSymCount(3);
        marketDataRequest.setStreamReference("stream1");
        marketDataRequest.setSubscriptionRequestType(FixEnums.SUBSCRIPTION_REQUEST_TYPE_SUBSCRIBE);
        marketDataRequest.getRelatedSym(0).setSymbol("EURUSD");
        marketDataRequest.getRelatedSym(1).setSymbol("EURCAD_");
        marketDataRequest.getRelatedSym(2).setSymbol("XYZ");

        PrimeXmCodecUtil.encodeMarketDataRequest(cursor, marketDataRequest);
        assertEquals("262=asdfghrty|263=1|264=99|146=3|55=EURUSD|55=EURCAD_|55=XYZ|", FixHelper.readString(cursor));
    }*/

}
