package io.github.thepun.fix.md.domain;

public final class MarketDataSnapshotFullRefresh {

    private String mdReqID;
    private String symbol;
    private MDEntryGroup[] entries;

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

    public void setEntryCount(int index) {

    }

    public MDEntryGroup getEntry(int index) {
        return entries[index];
    }
}
