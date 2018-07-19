package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuck;
import com.capitalone.dashboard.model.BlackDuckProject;

public interface BlackDuckClient {
    BlackDuckProject getProject();
    BlackDuck getCurrentMetrics(BlackDuckProject project);
    void parseDocument(String instanceUrl);
}
