package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

public interface FixLogger {

    void status(String status);
    void incoming(ByteBuf buffer, int offset, int length);
    void outgoing(ByteBuf first, int firstOffset, int firstLength, ByteBuf second, int secondOffset, int secondLength);

}
