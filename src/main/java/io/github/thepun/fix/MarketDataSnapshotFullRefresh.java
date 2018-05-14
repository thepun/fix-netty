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

    public static MarketDataSnapshotFullRefresh reuseOrCreate() {
        return RECYCLER.get();
    }


    private final OffHeapCharSequence mdReqId;
    private final OffHeapCharSequence symbol;
    private final Recycler.Handle<MarketDataSnapshotFullRefresh> recyclerHandle;

    private boolean mdReqIdDefined;
    private boolean symbolDefined;
    private ByteBuf buffer;
    private int entryCount;
    private MDEntry[] entries;

    private MarketDataSnapshotFullRefresh(Recycler.Handle<MarketDataSnapshotFullRefresh> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;

        mdReqId = new OffHeapCharSequence();
        symbol = new OffHeapCharSequence();

        entries = new MDEntry[0];
    }

    public OffHeapCharSequence getMdReqId() {
        return mdReqId;
    }

    public OffHeapCharSequence getSymbol() {
        return symbol;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    public boolean isMdReqIdDefined() {
        return mdReqIdDefined;
    }

    public void setMdReqIdDefined(boolean mdReqIdDefined) {
        this.mdReqIdDefined = mdReqIdDefined;
    }

    public boolean isSymbolDefined() {
        return symbolDefined;
    }

    public void setSymbolDefined(boolean symbolDefined) {
        this.symbolDefined = symbolDefined;
    }

    public MDEntry getEntry(int index) {
        return entries[index];
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void initBuffer(ByteBuf buffer) {
        buffer.retain();
        this.buffer = buffer;
    }

    public void initEntries(int entryCount) {
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

        setRefCnt(1);
        recyclerHandle.recycle(this);
    }


    public static final class MDEntry {

        private final OffHeapCharSequence id;
        private final OffHeapCharSequence symbol;
        private final OffHeapCharSequence issuer;
        private final OffHeapCharSequence currency;

        private boolean idDefined;
        private boolean symbolDefined;
        private boolean issuerDefined;
        private boolean currencyDefined;
        private int mdUpdateAction;
        private int mdEntryType;
        private double mdEntryPX;
        private double mdEntrySize;
        private int quoteCondition;

        private MDEntry() {
            id = new OffHeapCharSequence();
            issuer = new OffHeapCharSequence();
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

        public OffHeapCharSequence getIssuer() {
            return issuer;
        }

        public boolean isIdDefined() {
            return idDefined;
        }

        public void setIdDefined(boolean idDefined) {
            this.idDefined = idDefined;
        }

        public boolean isSymbolDefined() {
            return symbolDefined;
        }

        public void setSymbolDefined(boolean symbolDefined) {
            this.symbolDefined = symbolDefined;
        }

        public boolean isCurrencyDefined() {
            return currencyDefined;
        }

        public void setCurrencyDefined(boolean currencyDefined) {
            this.currencyDefined = currencyDefined;
        }

        public boolean isIssuerDefined() {
            return issuerDefined;
        }

        public void setIssuerDefined(boolean issuerDefined) {
            this.issuerDefined = issuerDefined;
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
