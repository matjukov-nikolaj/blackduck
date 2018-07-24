package com.capitalone.dashboard.model;

import java.util.HashMap;
import java.util.Map;

public class AppScanCollector extends Collector {
    private String appScanServer = "";
    public String getBlackDuckServer() {
        return appScanServer;
    }

    public static AppScanCollector prototype(String server) {
        AppScanCollector protoType = new AppScanCollector();
        protoType.setName("AppScan");
        protoType.setCollectorType(CollectorType.AppScan);
        protoType.setOnline(true);
        protoType.setEnabled(true);

        if(server!=null) {
            protoType.appScanServer = server;
        }

        Map<String, Object> allOptions = new HashMap<>();
        allOptions.put(AppScanProject.INSTANCE_URL,"");
        allOptions.put(AppScanProject.PROJECT_NAME,"");
        protoType.setAllFields(allOptions);

        Map<String, Object> uniqueOptions = new HashMap<>();
        uniqueOptions.put(AppScanProject.INSTANCE_URL,"");
        uniqueOptions.put(AppScanProject.PROJECT_NAME,"");
        protoType.setUniqueFields(uniqueOptions);
        return protoType;
    }
}
