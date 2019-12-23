package com.dellemc.pravega.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogException implements Serializable {
    private static final long serialVersionUID = 1;

    public String id;
    public String timestamp;
    public List<String> data;

    public LogException(){
        this.id = UUID.randomUUID().toString();
        this.data = new ArrayList<>();
    }

    public LogException(String timestamp, List<String> data) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.data = data;
        if (data == null) {
            data = new ArrayList<>();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getData() {
        return data;
    }

    public void setData(List<String> data) {
        this.data = data;
    }


    @Override
    public String toString() {
        String list = "";
        for (String s : data) {
            list += s + "\n";
        }
        return "LogException{" +
            "id='" + id + '\'' +
            ", timestamp='" + timestamp + '\'' +
            ", data='" + list + '\'' +
            '}';
    }
}
