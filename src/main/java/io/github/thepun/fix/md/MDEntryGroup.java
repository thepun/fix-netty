package io.github.thepun.fix.md;

import io.github.thepun.unsafe.chars.OffHeapCharSequence;

public final class MDEntryGroup {

    public static final int MD_UPDATE_ACTION_NEW = 0;
    public static final int MD_UPDATE_ACTION_DELETE = 2;

    public static final int MD_ENTRY_TYPE_BID = 0;
    public static final int MD_ENTRY_TYPE_OFFER = 1;

    public static final int QUOTE_CONDITION_OPEN_ACTIVE = (int) 'A';
    public static final int QUOTE_CONDITION_CLOSED_INACTIVE = (int) 'B';
    public static final int QUOTE_CONDITION_EXCHANGE_BEST = (int) 'C';
    public static final int QUOTE_CONDITION_CONSOLIDATED_BEST = (int) 'D';


    private final OffHeapCharSequence id;
    private final OffHeapCharSequence symbol;
    private final OffHeapCharSequence currency;

    private int mdUpdateAction;
    private int mdEntryType;
    private double mdEntryPX;
    private double mdEntrySize;
    private int quoteCondition;

    public MDEntryGroup() {
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
