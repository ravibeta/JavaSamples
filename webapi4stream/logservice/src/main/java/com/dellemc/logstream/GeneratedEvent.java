package com.dellemc.logstream;

import java.io.Serializable;
import java.util.UUID;

public class GeneratedEvent implements Serializable {
    private static final long serialVersionUID = 1;

    public String id;
    public String key;
    public String data;
    private long timestamp;

    public GeneratedEvent(){
    }

    public GeneratedEvent(String key, String data, long timestamp) {
        this.id = UUID.randomUUID().toString();
        this.key = key;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "GeneratedEvent{" +
            "id='" + id + '\'' +
            ", key='" + key + '\'' +
            ", data='" + data + '\'' +
            '}';
    }
}
