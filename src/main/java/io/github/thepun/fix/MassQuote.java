package io.github.thepun.fix;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;

public final class MassQuote extends AbstractReferenceCounted {

    private static final Recycler<MassQuote> RECYCLER = new Recycler<MassQuote>() {
        @Override
        protected MassQuote newObject(Handle<MassQuote> handle) {
            return new MassQuote(handle);
        }
    };

    public static MassQuote newInstance() {
        return RECYCLER.get();
    }



    private final Recycler.Handle<MassQuote> recyclerHandle;

    private ByteBuf messageBuffer;

    private MassQuote(Recycler.Handle<MassQuote> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
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
}
