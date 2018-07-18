package com.capitalone.dashboard.model;

public class BlackDuckProject extends CollectorItem {

    protected static final String INSTANCE_URL = "instanceUrl";
    protected static final String PROJECT_NAME = "projectName";
    protected static final String PROJECT_TIMESTAMP = "projectTimestamp";

    public String getInstanceUrl() {
        return (String) getOptions().get(INSTANCE_URL);
    }

    public void setInstanceUrl(String instanceUrl) {
        getOptions().put(INSTANCE_URL, instanceUrl);
    }

    public String getProjectTimestamp() { return (String) getOptions().get(PROJECT_TIMESTAMP); }

    public void setProjectTimestamp(String timestamp) { getOptions().put(PROJECT_TIMESTAMP, timestamp); }

    public String getProjectName() {
        return (String) getOptions().get(PROJECT_NAME);
    }

    public void setProjectName(String name) {
        getOptions().put(PROJECT_NAME, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlackDuckProject that = (BlackDuckProject) o;
        return getProjectTimestamp().equals(that.getProjectTimestamp())
                && getInstanceUrl().equals(that.getInstanceUrl())
                && getProjectName().equals(that.getProjectName());
    }

    @Override
    public int hashCode() {
        int result = getInstanceUrl().hashCode();
        result = 31 * result + getProjectTimestamp().hashCode();
        return result;
    }

}
