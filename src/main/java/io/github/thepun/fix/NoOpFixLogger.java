package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

final class NoOpFixLogger implements FixLogger {

    static final NoOpFixLogger INSTANCE = new NoOpFixLogger();


    private NoOpFixLogger() {
    }

    @Override
    public void status(String message) {

    }

    @Override
    public void incoming(ByteBuf buffer, int offset, int length) {

    }

    @Override
    public void outgoing(ByteBuf buffer, int offset, int length) {

    }
}
