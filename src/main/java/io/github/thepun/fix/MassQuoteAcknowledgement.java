package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;

public final class MassQuoteAcknowledgement extends AbstractReferenceCounted {

    private static final Recycler<MassQuoteAcknowledgement> RECYCLER = new Recycler<MassQuoteAcknowledgement>() {
        @Override
        protected MassQuoteAcknowledgement newObject(Handle<MassQuoteAcknowledgement> handle) {
            return new MassQuoteAcknowledgement(handle);
        }
    };

    public static MassQuoteAcknowledgement reuseOrCreate() {
        MassQuoteAcknowledgement massQuoteAcknowledgement = RECYCLER.get();
        return massQuoteAcknowledgement;
    }


    private final OffHeapCharSequence quoteId;
    private final Recycler.Handle<MassQuoteAcknowledgement> recyclerHandle;

    private ByteBuf buffer;
    private boolean quoteIdDefined;

    private MassQuoteAcknowledgement(Recycler.Handle<MassQuoteAcknowledgement> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;

        quoteId = new OffHeapCharSequence();
    }

    public OffHeapCharSequence getQuoteId() {
        return quoteId;
    }

    public ByteBuf getBuffer() {
        return buffer;
    }

    public boolean isQuoteIdDefined() {
        return quoteIdDefined;
    }

    public void setQuoteIdDefined(boolean quoteIdDefined) {
        this.quoteIdDefined = quoteIdDefined;
    }

    public void initBuffer(ByteBuf buffer) {
        buffer.retain();
        this.buffer = buffer;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        if (buffer != null) {
            buffer.touch(hint);
        }

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


}
