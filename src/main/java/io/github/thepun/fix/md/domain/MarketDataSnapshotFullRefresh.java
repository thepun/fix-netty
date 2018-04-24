package io.github.thepun.fix.md.domain;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

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


    private final Recycler.Handle<MarketDataSnapshotFullRefresh> recyclerHandle;

    private ByteBuf messageBuffer;
    private String mdReqID;
    private String symbol;
    private MDEntryGroup[] entries;

    private MarketDataSnapshotFullRefresh(Recycler.Handle<MarketDataSnapshotFullRefresh> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
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

    public void setEntries(MDEntryGroup[] entries) {
        this.entries = entries;
    }

    public String getMdReqID() {
        return mdReqID;
    }

    public void setMdReqID(String mdReqID) {
        this.mdReqID = mdReqID;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getEntryCount() {
        return entries.length;
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
        ReferenceCountUtil.touch(messageBuffer);
        return this;
    }

    @Override
    protected void deallocate() {
        messageBuffer.release();
        messageBuffer = null;

        recyclerHandle.recycle(this);
    }
}
