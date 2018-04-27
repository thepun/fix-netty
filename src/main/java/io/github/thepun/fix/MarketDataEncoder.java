package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

final class MarketDataEncoder extends MessageToByteEncoder<Object> {

    @Override
    public void encode(ChannelHandlerContext ctx, Object message, ByteBuf out) {
        if (message instanceof MarketDataSnapshotFullRefresh) {

        } else if (message instanceof Logon) {
            encodeLogon((Logon) message);
        } else {

        }

        int initialOffset = out.writerIndex();

        ByteBuf bodyBuf = ctx.alloc().buffer();
        ByteBuf headBuf = ctx.alloc().buffer();
        try {
            // write body
            message.getFields().forEach(field -> writeField(field, bodyBuf));

            // Common headers
            writeHeader(message.getBeginString(), out);
            writeHeader(message.getMsgType(), headBuf);
            writeHeader(message.getMsgSeqNum(), headBuf);
            writeHeader(message.getSendingTime(), headBuf);
            writeHeader(message.getSenderCompID(), headBuf);
            writeHeader(message.getSenderSubID(), headBuf);
            writeHeader(message.getTargetCompID(), headBuf);
            writeHeader(message.getTargetSubID(), headBuf);

            // BodyLength header
            FixPacketEntry bodyLengthHeader = message.getBodyLength();
            int bodyLength = bodyBuf.writerIndex() + headBuf.writerIndex();
            bodyLengthHeader.setString(Integer.toString(bodyLength));
            writeHeader(bodyLengthHeader, out);

            // write temp buffers
            out.writeBytes(headBuf);
            out.writeBytes(bodyBuf);

            // calculate checksum
            FixPacketEntry checksumTrailer = message.getCheckSum();
            int checksum = calculateChecksum(out, initialOffset);
            int x2 = checksum / 100;
            int x1 = (checksum - x2 * 100) / 10;
            int x0 = checksum - x2 * 100 - x1 * 10;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append((char) ('0' + x2));
            stringBuilder.append((char) ('0' + x1));
            stringBuilder.append((char) ('0' + x0));
            checksumTrailer.setString(stringBuilder.toString());
            writeHeader(checksumTrailer, out);
        } finally {
            headBuf.release();
            bodyBuf.release();
        }
    }

    private static void writeField(FixPacketEntry entry, ByteBuf out) {
        if (entry.isDefined()) {
            writeTagAndValue(entry.getId(), entry.getString(), out);
        }
    }

    private static void writeHeader(FixPacketEntry entry, ByteBuf out) {
        if (entry.isDefined()) {
            writeTagAndValue(entry.getId(), entry.getString(), out);
        }
    }

    private static void writeTagAndValue(int id, String value, ByteBuf out) {
        out.writeBytes(Integer.toString(id).getBytes(CharsetUtil.US_ASCII));
        out.writeByte('=');
        out.writeCharSequence(value, CharsetUtil.US_ASCII);
        out.writeByte(1);
    }

    private static int calculateChecksum(ByteBuf buf, int offset) {
        int sum = 0;
        for (int i = offset; i < buf.writerIndex(); i++) {
            sum += buf.getByte(i);
        }
        return sum % 256;
    }



    private void encodeLogon(Logon logon) {

    }
}
