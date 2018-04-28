package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;

final class EncodingUtil {

    static void encodeOnDigitTag(Cursor cursor) {
        ByteBuf out = cursor.getBuffer();
        int index = cursor.getIndex();

        int tagNum = cursor.getTag();
        out.setByte(index, tagNum);

        cursor.setIndex(index);
    }

    static void encodeIntValue(Cursor cursor) {

    }

}
