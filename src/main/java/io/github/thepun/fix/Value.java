package io.github.thepun.fix;

import sun.misc.Contended;

@Contended
final class Value {

    private int intValue;
    private long longValue;
    private double doubleValue;
    private boolean booleanValue;
    private String strValue;

    int getIntValue() {
        return intValue;
    }

    void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    long getLongValue() {
        return longValue;
    }

    void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    double getDoubleValue() {
        return doubleValue;
    }

    void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    boolean getBooleanValue() {
        return booleanValue;
    }

    void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    String getStrValue() {
        return strValue;
    }

    void setStrValue(String strValue) {
        this.strValue = strValue;
    }
}
