package io.github.thepun.fix;


final class Logon {

    private String username;
    private String password;
    private int encryptMethod;
    private int heartbeatInterval;
    private boolean resetSqNumFlag;
    
    public int getEncryptMethod() {
        return encryptMethod;
    }

    public void setEncryptMethod(int encryptMethod) {
        this.encryptMethod = encryptMethod;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public boolean isResetSqNumFlag() {
        return resetSqNumFlag;
    }

    public void setResetSqNumFlag(boolean resetSqNumFlag) {
        this.resetSqNumFlag = resetSqNumFlag;
    }
}
