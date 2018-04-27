package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import sun.misc.Contended;

@Contended
public final class FixParserCursor {

    private ByteBuf in;
    private int count;
    private int index;
    private int tag;
    private int intValue;
    private int strAsInt;
    private long strStart;
    private int strLength;
    private long nativeAddress;
    private double doubleValue;

    public ByteBuf getIn() {
        return in;
    }

    public void setIn(ByteBuf in) {
        this.in = in;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public int getStrAsInt() {
        return strAsInt;
    }

    public void setStrAsInt(int strAsInt) {
        this.strAsInt = strAsInt;
    }

    public long getStrStart() {
        return strStart;
    }

    public void setStrStart(long strStart) {
        this.strStart = strStart;
    }

    public int getStrLength() {
        return strLength;
    }

    public void setStrLength(int strLength) {
        this.strLength = strLength;
    }

    public long getNativeAddress() {
        return nativeAddress;
    }

    public void setNativeAddress(long nativeAddress) {
        this.nativeAddress = nativeAddress;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }
}
