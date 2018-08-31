package com.capitalone.dashboard.model;

import codesecurity.collectors.model.CodeSecurityProject;

public class BlackDuckProject extends CodeSecurityProject {

    public static final String PROJECT_TIMESTAMP = "projectTimestamp";

    public Long getProjectTimestamp() { return (Long) getOptions().get(PROJECT_TIMESTAMP); }

    public void setProjectTimestamp(Long timestamp) { getOptions().put(PROJECT_TIMESTAMP, timestamp); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlackDuckProject that = (BlackDuckProject) o;
        return getProjectTimestamp().equals(that.getProjectTimestamp())
                && getInstanceUrl().equals(that.getInstanceUrl())
                && getProjectName().equals(that.getProjectName())
                && getProjectDate().equals(that.getProjectDate());
    }

    @Override
    public int hashCode() {
        int result = getInstanceUrl().hashCode();
        result = 31 * result + getProjectTimestamp().hashCode();
        return result;
    }

}
