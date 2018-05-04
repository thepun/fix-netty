package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.github.thepun.fix.FixHelper.readString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PrimeXMClientHandlerTest {

    private FixSessionInfo fixSessionInfo;
    private MarketDataReadyListener readyListener;
    private MarketDataQuotesListener quotesListener;

    @BeforeEach
    void prepareMocks() {
        fixSessionInfo = new FixSessionInfo("qwe_", "1asd", "+--341", "sdf", "user", "pass");
        readyListener = mock(MarketDataReadyListener.class);
        quotesListener = mock(MarketDataQuotesListener.class);
    }

    @Test
    void logonOnChannelActivation() {
        PrimeXMClientHandler handler = new PrimeXMClientHandler(fixSessionInfo, NoOpFixLogger.INSTANCE, readyListener, quotesListener, 30);

        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.checkException();

        ByteBuf buffer = channel.readOutbound();
        assertNotNull(buffer);

        String fixMessage = readString(buffer);
        Object o = null;
    }

}
