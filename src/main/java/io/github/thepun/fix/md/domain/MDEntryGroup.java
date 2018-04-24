package io.github.thepun.fix.md.domain;

public final class MDEntryGroup {

    public static final int MD_UPDATE_ACTION_NEW = 0;
    public static final int MD_UPDATE_ACTION_DELETE = 2;

    public static final int MD_ENTRY_TYPE_BID = 0;
    public static final int MD_ENTRY_TYPE_OFFER = 1;

    public static final int QUOTE_CONDITION_OPEN_ACTIVE = (int) 'A';
    public static final int QUOTE_CONDITION_CLOSED_INACTIVE = (int) 'B';
    public static final int QUOTE_CONDITION_EXCHANGE_BEST = (int) 'C';
    public static final int QUOTE_CONDITION_CONSOLIDATED_BEST = (int) 'D';


    private int mdUpdateAction;
    private int mdEntryType;
    private String symbol;
    private String currency;
    private double mdEntryPX;
    private double mdEntrySize;
    private int quoteCondition;

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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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
