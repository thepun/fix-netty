package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FixHelper {

    /*static String readString(Cursor cursor) {
        ByteBuf buffer = cursor.getBuffer();
        buffer.writerIndex(cursor.getIndex());
        return buffer.readCharSequence(buffer.readableBytes(), CharsetUtil.US_ASCII).toString().replace((char) 1, '|');
    }*/

    static String readString(ByteBuf buffer) {
        return buffer.readCharSequence(buffer.readableBytes(), CharsetUtil.US_ASCII).toString().replace((char) 1, '|');
    }

    // TODO: make possible to pass ANY with all fix fields
    static void assertFixMathes(String expected, String actual) {
        assertNotNull(actual);

        String actualWithoutTime = actual
                .replaceAll("\\|52=[:\\-\\.0-9]*\\|","|52=<ANY>|")
                .replaceAll("\\|9=[0-9]*","|9=<ANY>")
                .replaceAll("\\|10=[0-9]*","|10=<ANY>");
        assertEquals(expected, actualWithoutTime);
    }

    static String readFixMessageFromChannel(EmbeddedChannel channel) {
        ByteBuf buffer1 = channel.readOutbound();
        assertNotNull(buffer1);

        ByteBuf buffer2 = channel.readOutbound();
        assertNotNull(buffer2);

        String fix1 = readString(buffer1);
        String fix2 = readString(buffer2);
        return fix1 + fix2;
    }

    static void writeFixMessageToChannel(EmbeddedChannel channel, String message) {
        ByteBuf buffer = Unpooled.directBuffer(message.length() * 2);
        buffer.writeCharSequence(message.replace('|', (char) 1), CharsetUtil.US_ASCII);
        channel.writeInbound(buffer);
    }
}
