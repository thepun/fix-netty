package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

final class ConsoleLogger implements FixLogger {

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
