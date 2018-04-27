package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

public interface FixLogger {

    void message(ByteBuf buffer, int offset, int length);

}
