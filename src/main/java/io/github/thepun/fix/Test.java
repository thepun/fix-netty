package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;

final class Test extends AbstractReferenceCounted {

    private static final Recycler<Test> RECYCLER = new Recycler<Test>() {
        @Override
        protected Test newObject(Handle<Test> handle) {
            return new Test(handle);
        }
    };

    static Test newInstance() {
        return RECYCLER.get();
    }


    private final OffHeapCharSequence testId;
    private final Recycler.Handle<Test> recyclerHandle;

    private ByteBuf buffer;
    private boolean testIdDefined;

    private Test(Recycler.Handle<Test> recyclerHandle) {
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

        recyclerHandle.recycle(this);
    }
}