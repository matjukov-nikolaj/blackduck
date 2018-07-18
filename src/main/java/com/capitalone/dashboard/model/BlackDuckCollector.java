package com.capitalone.dashboard.model;

import java.util.HashMap;
import java.util.Map;

public class BlackDuckCollector extends Collector {
    private String blackDuckServer = "";
    public String getBlackDuckServer() {
        return blackDuckServer;
    }

    public static BlackDuckCollector prototype(String server) {
        BlackDuckCollector protoType = new BlackDuckCollector();
        protoType.setName("BlackDuck");
        protoType.setCollectorType(CollectorType.BlackDuck);
        protoType.setOnline(true);
        protoType.setEnabled(true);

        if(server!=null) {
            protoType.blackDuckServer = server;
        }

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(BlackDuckProject.INSTANCE_URL,"");
        allOptions.put(BlackDuckProject.PROJECT_NAME,"");
        allOptions.put(BlackDuckProject.PROJECT_TIMESTAMP, "");
        protoType.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(BlackDuckProject.INSTANCE_URL,"");
        uniqueOptions.put(BlackDuckProject.PROJECT_NAME,"");
        protoType.setUniqueFields(uniqueOptions);
        return protoType;
    }
}
