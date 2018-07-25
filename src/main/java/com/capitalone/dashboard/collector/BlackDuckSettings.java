package com.capitalone.dashboard.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import codesecurity.collectors.collector.CodeSecuritySettings;

@Component
@ConfigurationProperties(prefix = "blackduck")
public class BlackDuckSettings extends CodeSecuritySettings{
}

