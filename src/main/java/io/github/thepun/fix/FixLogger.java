package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

public interface FixLogger {

    void status(String status);
    void incoming(ByteBuf buffer, int offset, int length);
    void outgoing(ByteBuf buffer, int offset, int length);

}
