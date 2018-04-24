package io.github.thepun.fix.md.codec;

import com.sfs.acceptance.common.HasInt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.util.List;

import static com.sfs.acceptance.transport.fix.Header.BeginString;

final class MarketDataDecoder extends ByteToMessageDecoder {

    private FixPacketWithHeaders message;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int arrayIndex = 0;
        int tagNum = 0;

        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);

        int length = bytes.length;
        for (; arrayIndex < length; arrayIndex++) {
            byte b = bytes[arrayIndex];
            if (b == '=') {
                break;
            }
            tagNum = tagNum * 10 + (b - '0');
        }

        int offset = arrayIndex + 1;
        int valueLength = length - arrayIndex - 1;
        String value = new String(bytes, offset, valueLength);

        try {
            Header headerId = HasInt.fromIntegerOrNull(Header.class, tagNum);
            if (headerId != null) {
                if (message == null && headerId != BeginString) {
                    return;
                }

                switch (headerId) {
                    case BeginString:
                        message = new FixPacketWithHeaders();
                        message.getBeginString().setString(value);
                        break;

                    case BodyLength:
                        message.getBodyLength().setString(value);
                        break;

                    case MsgType:
                        message.getMsgType().setString(value);
                        break;

                    case MsgSeqNum:
                        message.getMsgSeqNum().setString(value);
                        break;

                    case SenderCompID:
                        message.getSenderCompID().setString(value);
                        break;

                    case SenderSubID:
                        message.getSenderSubID().setString(value);
                        break;

                    case TargetCompID:
                        message.getTargetCompID().setString(value);
                        break;

                    case TargetSubID:
                        message.getTargetSubID().setString(value);
                        break;

                    case SendingTime:
                        message.getSendingTime().setString(value);
                        break;

                    case CheckSum:
                        message.getCheckSum().setString(value);
                        out.add(message);
                        message = null;
                        break;
                }
            } else {
                if (message == null) {
                    return;
                }

                FixPacketEntry entry = new FixPacketEntry(tagNum);
                entry.setString(value);
                message.getFields().add(entry);
            }
        } catch (DecoderException e) {
            message = null;
            throw e;
        } catch (Exception e) {
            message = null;
            throw new DecoderException("Exception during fix packet decoding", e);
        }
    }
}
