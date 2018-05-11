package io.github.thepun.fix;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.MathUtil;

public final class MassQuote extends AbstractReferenceCounted {

    private static final Recycler<MassQuote> RECYCLER = new Recycler<MassQuote>() {
        @Override
        protected MassQuote newObject(Handle<MassQuote> handle) {
            return new MassQuote(handle);
        }
    };

    public static MassQuote reuseOrCreate() {
        MassQuote massQuote = RECYCLER.get();
        //massQuote.retain();
        return massQuote;
    }


    private final OffHeapCharSequence quoteId;
    private final Recycler.Handle<MassQuote> recyclerHandle;

    private ByteBuf buffer;
    private boolean quoteIdDefined;
    private int quoteSetCount;
    private QuoteSet[] quoteSets;

    private MassQuote(Recycler.Handle<MassQuote> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;

        quoteId = new OffHeapCharSequence();
        quoteSets = new QuoteSet[0];
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

    public int getQuoteSetCount() {
        return quoteSetCount;
    }

    public QuoteSet getQuoteSet(int index) {
        return quoteSets[index];
    }

    public void initBuffer(ByteBuf buffer) {
        buffer.retain();
        this.buffer = buffer;
    }

    public void initQuoteSets(int quoteSetCount) {
        this.quoteSetCount = quoteSetCount;

        // ensure enough data
        if (quoteSets.length < quoteSetCount) {
            QuoteSet[] newQuoteSets = new QuoteSet[MathUtil.safeFindNextPositivePowerOfTwo(quoteSetCount)];
            System.arraycopy(quoteSets, 0, newQuoteSets, 0, quoteSets.length);

            for (int i = quoteSets.length; i < newQuoteSets.length; i++) {
                newQuoteSets[i] = new QuoteSet();
            }

            quoteSets = newQuoteSets;
        }
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


    public static class QuoteSet {

        private final OffHeapCharSequence quoteSetId;

        private int entryCount;
        private QuoteEntry[] entries;
        private boolean quoteSetIdDefined;

        private QuoteSet() {
            quoteSetId = new OffHeapCharSequence();
            entries = new QuoteEntry[0];
        }

        public OffHeapCharSequence getQuoteSetId() {
            return quoteSetId;
        }

        public boolean isQuoteSetIdDefined() {
            return quoteSetIdDefined;
        }

        public void setQuoteSetIdDefined(boolean quoteSetIdDefined) {
            this.quoteSetIdDefined = quoteSetIdDefined;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public QuoteEntry getEntry(int index) {
            return entries[index];
        }

        public void initEntries(int entryCount) {
            this.entryCount = entryCount;

            // ensure enough data
            if (entries.length < entryCount) {
                QuoteEntry[] newEntries = new QuoteEntry[MathUtil.safeFindNextPositivePowerOfTwo(entryCount)];
                System.arraycopy(entries, 0, newEntries, 0, entries.length);

                for (int i = entries.length; i < entryCount; i++) {
                    newEntries[i] = new QuoteEntry();
                }

                entries = newEntries;
            }
        }
    }


    public static class QuoteEntry {

        private final OffHeapCharSequence quoteEntryId;
        private final OffHeapCharSequence issuer;

        private double bidSize;
        private double bidSpotRate;
        private double offerSize;
        private double offerSpotRate;
        private boolean issuerDefined;
        private boolean quoteEntryIdDefined;

        private QuoteEntry() {
            quoteEntryId = new OffHeapCharSequence();
            issuer = new OffHeapCharSequence();
        }

        public OffHeapCharSequence getQuoteEntryId() {
            return quoteEntryId;
        }

        public OffHeapCharSequence getIssuer() {
            return issuer;
        }

        public double getBidSize() {
            return bidSize;
        }

        public void setBidSize(double bidSize) {
            this.bidSize = bidSize;
        }

        public double getBidSpotRate() {
            return bidSpotRate;
        }

        public void setBidSpotRate(double bidSpotRate) {
            this.bidSpotRate = bidSpotRate;
        }

        public double getOfferSize() {
            return offerSize;
        }

        public void setOfferSize(double offerSize) {
            this.offerSize = offerSize;
        }

        public double getOfferSpotRate() {
            return offerSpotRate;
        }

        public void setOfferSpotRate(double offerSpotRate) {
            this.offerSpotRate = offerSpotRate;
        }

        public boolean isIssuerDefined() {
            return issuerDefined;
        }

        public void setIssuerDefined(boolean issuerDefined) {
            this.issuerDefined = issuerDefined;
        }

        public boolean isQuoteEntryIdDefined() {
            return quoteEntryIdDefined;
        }

        public void setQuoteEntryIdDefined(boolean quoteEntryIdDefined) {
            this.quoteEntryIdDefined = quoteEntryIdDefined;
        }
    }
}
