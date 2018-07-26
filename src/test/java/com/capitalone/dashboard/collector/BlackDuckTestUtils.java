package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuckProject;

public abstract  class BlackDuckTestUtils {

    private static final String EXPECTED_NAME = "test:2018-7-12";
    private static final String EXPECTED_TIMESTAMP= "1531388760000";

    protected String getUrlToTestFile(String server) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResource(server).toString();
    }

    protected BlackDuckProject getExpectedBlackDuckProject() {
        BlackDuckProject project = new BlackDuckProject();
        project.setProjectName(EXPECTED_NAME);
        project.setProjectTimestamp(EXPECTED_TIMESTAMP);
        project.setInstanceUrl(getUrl(getServer()));
        return project;
    }

    protected abstract String getUrl(String server);

    protected abstract String getServer();

}
