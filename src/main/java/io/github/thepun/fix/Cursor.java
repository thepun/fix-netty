package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import sun.misc.Contended;

@Contended
final class Cursor {

    private ByteBuf buffer;
    private int point;
    private int index;
    //private int before;
    private int tag;
    private int intValue;
    private long strStart;
    private int strLength;
    private long nativeAddress;
    private double doubleValue;
    private byte[] temp;

    ByteBuf getBuffer() {
        return buffer;
    }

    void setBuffer(ByteBuf buffer) {
        this.buffer = buffer;
    }

    int getPoint() {
        return point;
    }

    void setPoint(int point) {
        this.point = point;
    }

    int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    int getTag() {
        return tag;
    }

    void setTag(int tag) {
        this.tag = tag;
    }

    int getIntValue() {
        return intValue;
    }

    void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    long getStrStart() {
        return strStart;
    }

    void setStrStart(long strStart) {
        this.strStart = strStart;
    }

    int getStrLength() {
        return strLength;
    }

    void setStrLength(int strLength) {
        this.strLength = strLength;
    }

    long getNativeAddress() {
        return nativeAddress;
    }

    void setNativeAddress(long nativeAddress) {
        this.nativeAddress = nativeAddress;
    }

    double getDoubleValue() {
        return doubleValue;
    }

    void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    byte[] getTemp() {
        return temp;
    }

    void setTemp(byte[] temp) {
        this.temp = temp;
    }



    /*int getBefore() {
        return before;
    }

    void setBefore(int before) {
        this.before = before;
    }*/
}
