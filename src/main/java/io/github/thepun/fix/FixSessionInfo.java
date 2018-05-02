package io.github.thepun.fix;

public final class FixSessionInfo {

    private final String senderCompId;
    private final String senderSubId;
    private final String targetCompId;
    private final String targetSubId;
    private final String username;
    private final String password;

    public FixSessionInfo(String senderCompId, String senderSubId, String targetCompId, String targetSubId, String username, String password) {
        this.senderCompId = senderCompId;
        this.senderSubId = senderSubId;
        this.targetCompId = targetCompId;
        this.targetSubId = targetSubId;
        this.username = username;
        this.password = password;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
