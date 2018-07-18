package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuck;
import com.capitalone.dashboard.model.BlackDuckProject;

public interface BlackDuckClient {
    BlackDuckProject getBlackDuckProject(String instanceUrl);
    BlackDuck currentBlackDuckMetrics(BlackDuckProject project);
}
