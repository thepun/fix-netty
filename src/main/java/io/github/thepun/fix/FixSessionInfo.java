package io.github.thepun.fix;

public final class FixSessionInfo {

    private final String senderCompId;
    private final String senderSubId;
    private final String targetCompId;
    private final String targetSubId;

    public FixSessionInfo(String senderCompId, String senderSubId, String targetCompId, String targetSubId) {
        this.senderCompId = senderCompId;
        this.senderSubId = senderSubId;
        this.targetCompId = targetCompId;
        this.targetSubId = targetSubId;
    }

    public String getSenderCompId() {
        return senderCompId;
    }

    public String getSenderSubId() {
        return senderSubId;
    }

    public String getTargetCompId() {
        return targetCompId;
    }

    public String getTargetSubId() {
        return targetSubId;
    }
}
