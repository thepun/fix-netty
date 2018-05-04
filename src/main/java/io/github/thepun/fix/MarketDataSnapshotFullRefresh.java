package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.MathUtil;

public final class MarketDataSnapshotFullRefresh extends AbstractReferenceCounted {

    private static final Recycler<MarketDataSnapshotFullRefresh> RECYCLER = new Recycler<MarketDataSnapshotFullRefresh>() {
        @Override
        protected MarketDataSnapshotFullRefresh newObject(Handle<MarketDataSnapshotFullRefresh> handle) {
            return new MarketDataSnapshotFullRefresh(handle);
        }
    };

    public static MarketDataSnapshotFullRefresh newInstance() {
        return RECYCLER.get();
    }


    private final OffHeapCharSequence mdReqID;
    private final OffHeapCharSequence symbol;
    private final Recycler.Handle<MarketDataSnapshotFullRefresh> recyclerHandle;

    private ByteBuf buffer;
    private int entryCount;
    private MDEntry[] entries;

    private MarketDataSnapshotFullRefresh(Recycler.Handle<MarketDataSnapshotFullRefresh> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;

        mdReqID = new OffHeapCharSequence();
        symbol = new OffHeapCharSequence();
    }

    public OffHeapCharSequence getMdReqID() {
        return mdReqID;
    }

    public OffHeapCharSequence getSymbol() {
        return symbol;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    public MDEntry getEntry(int index) {
        return entries[index];
    }

    public int getEntryCount() {
        return entryCount;
    }

    void initBuffer(ByteBuf buffer) {
        buffer.retain();
        this.buffer = buffer;
    }

    void initEntries(int entryCount) {
        this.entryCount = entryCount;

        // ensure we have enough data
        if (entries.length < entryCount) {
            MDEntry[] newEntries = new MDEntry[MathUtil.safeFindNextPositivePowerOfTwo(entryCount)];
            System.arraycopy(entries, 0, newEntries, 0, entries.length);

            for (int i = entries.length; i < newEntries.length; i++) {
                newEntries[i] = new MDEntry();
            }

            entries = newEntries;
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        buffer.touch(hint);
        return this;
    }

    @Override
    protected void deallocate() {
        if (buffer != null) {
            buffer.release();
            buffer = null;
        }

        recyclerHandle.recycle(this);
    }


    public static final class MDEntry {

        private final OffHeapCharSequence id;
        private final OffHeapCharSequence symbol;
        private final OffHeapCharSequence currency;

        private int mdUpdateAction;
        private int mdEntryType;
        private double mdEntryPX;
        private double mdEntrySize;
        private int quoteCondition;

        private MDEntry() {
            id = new OffHeapCharSequence();
            symbol = new OffHeapCharSequence();
            currency = new OffHeapCharSequence();
        }

        public OffHeapCharSequence getId() {
            return id;
        }

        public OffHeapCharSequence getSymbol() {
            return symbol;
        }

        public OffHeapCharSequence getCurrency() {
            return currency;
        }

        public int getMdUpdateAction() {
            return mdUpdateAction;
        }

        public void setMdUpdateAction(int mdUpdateAction) {
            this.mdUpdateAction = mdUpdateAction;
        }

        public int getMdEntryType() {
            return mdEntryType;
        }

        public void setMdEntryType(int mdEntryType) {
            this.mdEntryType = mdEntryType;
        }

        public double getMdEntryPX() {
            return mdEntryPX;
        }

        public void setMdEntryPX(double mdEntryPX) {
            this.mdEntryPX = mdEntryPX;
        }

        public double getMdEntrySize() {
            return mdEntrySize;
        }

        public void setMdEntrySize(double mdEntrySize) {
            this.mdEntrySize = mdEntrySize;
        }

        public int getQuoteCondition() {
            return quoteCondition;
        }

        public void setQuoteCondition(int quoteCondition) {
            this.quoteCondition = quoteCondition;
        }
    }
}
