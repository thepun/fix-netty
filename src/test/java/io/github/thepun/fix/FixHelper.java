package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

class FixHelper {

    static String readString(Cursor cursor) {
        ByteBuf buffer = cursor.getBuffer();
        buffer.writerIndex(cursor.getIndex());
        return buffer.readCharSequence(buffer.readableBytes(), CharsetUtil.US_ASCII).toString().replace((char) 1, '|');
    }

    static String readString(ByteBuf buffer) {
        return buffer.readCharSequence(buffer.readableBytes(), CharsetUtil.US_ASCII).toString().replace((char) 1, '|');
    }
}
