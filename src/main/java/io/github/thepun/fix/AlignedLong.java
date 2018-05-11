package io.github.thepun.fix;

import io.github.thepun.unsafe.ObjectMemory;

final class AlignedLong extends AlignedLongFields {

    static final long valueOffset;
    static {
        valueOffset = ObjectMemory.fieldOffset(AlignedLongFields.class, "value");
    }

    // 56 bytes gap
    private long t1, t2, t3, t4, t5, t6, t7, t8;


    long get() {
        return value;
    }

    void set(long newValue) {
        value = newValue;
    }

    boolean compareAndSwap(long expectedValue, long newValue) {
        return ObjectMemory.compareAndSwapLong(this, valueOffset, expectedValue, newValue);
    }

    long getAndIncrement() {
        return ObjectMemory.getAndAddLong(this, valueOffset, 1L);
    }
}

class AlignedLongPadding {
    // 12 bytes header

    // 4 byte gap
    private int t0;

    // 48 byte gap
    private long t1, t2, t3, t4, t5, t6;
}

class AlignedLongFields extends AlignedLongPadding {

    // non-volatile field to store current value
    long value;

}