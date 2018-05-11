package io.github.thepun.fix;

import io.github.thepun.unsafe.ArrayMemory;
import io.github.thepun.unsafe.MemoryFence;

final class MassQuoteBridge {

    private final int size;
    private final int mask;
    private final MassQuote[] data;
    private final AlignedLong readCounter;
    private final AlignedLong writeCounter;
    private final AlignedLong localReadCounter;
    private final AlignedLong localWriteCounter;

    MassQuoteBridge(int queueSize) {
        if (queueSize < 1) {
            throw new IllegalArgumentException("Size should be greater then zero");
        }

        double log2 = Math.log10(queueSize) / Math.log10(2);
        int pow = (int) Math.ceil(log2);

        size = (int) Math.pow(2, pow);
        mask = size - 1;
        data = new MassQuote[size];
        readCounter = new AlignedLong();
        writeCounter = new AlignedLong();
        localReadCounter = new AlignedLong();
        localWriteCounter = new AlignedLong();
    }

    public MassQuote removeFromHead() {
        long writeIndex = localWriteCounter.get();
        long readIndex = readCounter.get();
        if (readIndex >= writeIndex) {
            writeIndex = writeCounter.get();
            localWriteCounter.set(writeIndex);

            if (readIndex >= writeIndex) {
                return null;
            }
        }

        int index = (int) (readIndex & mask);
        MassQuote element = ArrayMemory.getObject(data, index);
        MemoryFence.load();
        readCounter.set(readIndex + 1);

        return element;
    }

    public boolean addToTail(MassQuote element) {
        long readIndex = localReadCounter.get();
        long writeIndex = writeCounter.get();
        if (writeIndex >= readIndex + size) {
            readIndex = readCounter.get();
            localReadCounter.set(readIndex);

            if (readIndex >= writeIndex) {
                return false;
            }
        }

        int index = (int) (writeIndex & mask);
        ArrayMemory.setObject(data, index, element);
        MemoryFence.store();
        writeCounter.set(writeIndex + 1);

        return true;
    }
}
