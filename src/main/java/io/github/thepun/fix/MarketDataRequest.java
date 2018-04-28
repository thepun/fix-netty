package io.github.thepun.fix;

public final class MarketDataRequest {

    public static MarketDataRequest newInstance() {
        return new MarketDataRequest();
    }


    private String mdReqId;
    private int subscriptionRequestType;
    private int marketDepth;
    private String streamReference;
    private RelatedSymGroup[] relatedSyms;

    private MarketDataRequest() {
    }

    public String getMdReqId() {
        return mdReqId;
    }

    public void setMdReqId(String mdReqId) {
        this.mdReqId = mdReqId;
    }

    public int getSubscriptionRequestType() {
        return subscriptionRequestType;
    }

    public void setSubscriptionRequestType(int subscriptionRequestType) {
        this.subscriptionRequestType = subscriptionRequestType;
    }

    public int getMarketDepth() {
        return marketDepth;
    }

    public void setMarketDepth(int marketDepth) {
        this.marketDepth = marketDepth;
    }

    public String getStreamReference() {
        return streamReference;
    }

    public void setStreamReference(String streamReference) {
        this.streamReference = streamReference;
    }

    public RelatedSymGroup[] getRelatedSyms() {
        return relatedSyms;
    }

    public void setRelatedSymCount(int count) {
        if (relatedSyms.length < count) {
            MarketDataRequest.RelatedSymGroup[] newRelatedSyms = new MarketDataRequest.RelatedSymGroup[count];
            System.arraycopy(relatedSyms, 0, newRelatedSyms, 0, relatedSyms.length);

            for (int i = relatedSyms.length; i < count; i++) {
                newRelatedSyms[i] = new MarketDataRequest.RelatedSymGroup();
            }

            relatedSyms = newRelatedSyms;
        }
    }


    static final class RelatedSymGroup {

        private String symbol;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
    }
}
