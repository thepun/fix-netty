package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;

public final class MarketDataSnapshotFullRefresh extends AbstractReferenceCounted {

    public static final int MD_UPDATE_ACTION_NEW = 0;
    public static final int MD_UPDATE_ACTION_DELETE = 2;

    public static final int MD_ENTRY_TYPE_BID = 0;
    public static final int MD_ENTRY_TYPE_OFFER = 1;

    public static final int QUOTE_CONDITION_OPEN_ACTIVE = (int) 'A';
    public static final int QUOTE_CONDITION_CLOSED_INACTIVE = (int) 'B';
    public static final int QUOTE_CONDITION_EXCHANGE_BEST = (int) 'C';
    public static final int QUOTE_CONDITION_CONSOLIDATED_BEST = (int) 'D';

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

    private ByteBuf messageBuffer;
    private MDEntryGroup[] entries;

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

    public ByteBuf getMessageBuffer() {
        return messageBuffer;
    }

    public void setMessageBuffer(ByteBuf messageBuffer) {
        messageBuffer.retain();
        this.messageBuffer = messageBuffer;
    }

    public MDEntryGroup[] getEntries() {
        return entries;
    }

    public void setEntryCount(int count) {
        if (entries.length < count) {
            MDEntryGroup[] newEntries = new MDEntryGroup[count];
            System.arraycopy(entries, 0, newEntries, 0, entries.length);

            for (int i = entries.length; i < count; i++) {
                newEntries[i] = new MDEntryGroup();
            }

            entries = newEntries;
        }
    }

    public MDEntryGroup getEntry(int index) {
        return entries[index];
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        messageBuffer.touch(hint);
        return this;
    }

    @Override
    protected void deallocate() {
        messageBuffer.release();
        messageBuffer = null;

        recyclerHandle.recycle(this);
    }


    public static final class MDEntryGroup {

        private final OffHeapCharSequence id;
        private final OffHeapCharSequence symbol;
        private final OffHeapCharSequence currency;

        private int mdUpdateAction;
        private int mdEntryType;
        private double mdEntryPX;
        private double mdEntrySize;
        private int quoteCondition;

        private MDEntryGroup() {
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
