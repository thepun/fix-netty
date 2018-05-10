package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;

final class Heartbeat extends AbstractReferenceCounted {

    private static final Recycler<Heartbeat> RECYCLER = new Recycler<Heartbeat>() {
        @Override
        protected Heartbeat newObject(Handle<Heartbeat> handle) {
            return new Heartbeat(handle);
        }
    };

    static Heartbeat reuseOrCreate() {
        Heartbeat heartbeat = RECYCLER.get();
        //heartbeat.retain();
        return heartbeat;
    }


    private final OffHeapCharSequence testId;
    private final Recycler.Handle<Heartbeat> recyclerHandle;

    private ByteBuf buffer;
    private boolean testIdDefined;

    private Heartbeat(Recycler.Handle<Heartbeat> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;

        testId = new OffHeapCharSequence();
    }

    OffHeapCharSequence getTestId() {
        return testId;
    }

    boolean isTestIdDefined() {
        return testIdDefined;
    }

    void setTestIdDefined(boolean testIdDefined) {
        this.testIdDefined = testIdDefined;
    }

    void initBuffer(ByteBuf buffer) {
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