package org.fedorahosted.freeotp.data;

import java.lang.annotation.Target;

public class TargetData {
    String ip;
    String port;
    String algo;
    String key;

    public TargetData(String ip, String port, String algo, String key)
    {
        this.ip = ip;
        this.port = port;
        this.algo = algo;
        this.key = key;
    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAlgo() {
        return algo;
    }

    public void setAlgo(String algo) {
        this.algo = algo;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
