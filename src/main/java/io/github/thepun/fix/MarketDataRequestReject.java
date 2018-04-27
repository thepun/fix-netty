package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;

final class MarketDataRequestReject {

    private final OffHeapCharSequence mdReqID;
    private final OffHeapCharSequence text;

    MarketDataRequestReject() {
        mdReqID = new OffHeapCharSequence();
        text = new OffHeapCharSequence();
    }

    OffHeapCharSequence getMdReqID() {
        return mdReqID;
    }

    OffHeapCharSequence getText() {
        return text;
    }
}
