package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuckProject;
import com.capitalone.dashboard.model.BlackDuck;
import codesecurity.collectors.collector.DefaultCodeSecurityClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import java.util.*;

@Component("DefaultBlackDuckClient")
public class DefaultBlackDuckClient extends DefaultCodeSecurityClient<BlackDuck, BlackDuckProject> {
    private static final Log LOG = LogFactory.getLog(DefaultBlackDuckClient.class);

    private static final String PROJECT_TIMESTAMP = "Last Updated:";
    private static final String PROJECT_NAME = "Name:";
    private static final String TR_TAG = "tr";
    private static final String NUMBER_OF_FILES = "Number of Files:";
    private static final String FILES_WITH_VIOLATIONS = "Files with Violations:";
    private static final String FILES_PENDING_IDENTIFICATION = "Files Pending Identification:";
    private static final String DATE_FORMAT = "MMMM d, yyyy h:mm a";

    private BlackDuck blackDuck;
    private BlackDuckProject project;
    private Map<String, String> metrics = new HashMap<>();
    private BlackDuckSettings settings;

    @Autowired
    public DefaultBlackDuckClient(BlackDuckSettings settings) {
        this.settings = settings;
    }

    @Override
    public BlackDuckProject getProject() {
        return this.project;
    }

    @Override
    public BlackDuck getCurrentMetrics(BlackDuckProject project) {
        return this.blackDuck;
    }

    protected void setInstanceUrlInProject(String instanceUrl) {
        this.project.setInstanceUrl(instanceUrl);
    }

    protected String getDateFormat() {
        return DATE_FORMAT;
    }

    protected String getUsernameFromSettings() {
        return this.settings.getUsername();
    }

    protected String getPasswordFromSettings() {
        return this.settings.getPassword();
    }

    protected void parseCodeSecurityDocument(Document document) throws Exception {
        NodeList trTags = document.getElementsByTagName(TR_TAG);
        if (trTags.getLength() == 0) {
            throw new NullPointerException();
        }
        for (int i = 0; i < trTags.getLength(); ++i) {
            Node trTag = trTags.item(i);
            String fieldName = getNodeValue(trTag);
            checkNodeValue(fieldName, trTag);
            if (this.project.getProjectTimestamp() != null) {
                break;
            }
        }
        setBlackDuckMetrics();
    }

    protected void initializationFields() {
        this.project = new BlackDuckProject();
        this.blackDuck = new BlackDuck();
        this.metrics.put(NUMBER_OF_FILES, "");
        this.metrics.put(FILES_WITH_VIOLATIONS, "");
        this.metrics.put(FILES_PENDING_IDENTIFICATION, "");
    }

    private void setBlackDuckMetrics() {
        this.blackDuck.setName(project.getProjectName());
        this.blackDuck.setMetrics(this.metrics);
        this.blackDuck.setUrl(project.getInstanceUrl());
        this.blackDuck.setTimestamp(Long.parseLong(project.getProjectTimestamp(), 10));
    }

    private void checkNodeValue(String fieldName, Node trTag) {
        switch (fieldName) {
            case PROJECT_NAME:
                updateProjectName(trTag);
                break;
            case NUMBER_OF_FILES:
                updateNumberOfFiles(trTag);
                break;
            case FILES_PENDING_IDENTIFICATION:
                updateNumberOfFilesPendingIdentification(trTag);
                break;
            case FILES_WITH_VIOLATIONS:
                updateNumberOfFilesWithViolations(trTag);
                break;
            case PROJECT_TIMESTAMP:
                updateProjectTimestamp(trTag);
                break;
            default:
                break;
        }
    }

    private void updateProjectName(Node trTag) {
        this.project.setProjectName(getFieldValue(trTag));
    }

    private void updateNumberOfFiles(Node trTag) {
        this.metrics.put(NUMBER_OF_FILES, getFieldValue(trTag));
    }

    private void updateNumberOfFilesPendingIdentification(Node trTag) {
        this.metrics.put(FILES_PENDING_IDENTIFICATION, getFieldValue(trTag));
    }

    private void updateNumberOfFilesWithViolations(Node trTag) {
        this.metrics.put(FILES_WITH_VIOLATIONS, getFieldValue(trTag));
    }

    private void updateProjectTimestamp(Node trTag) {
        String date = getFieldValue(trTag);
        this.project.setProjectTimestamp(Long.toString(getTimeStamp(date)));
        addDateToProjectName(date);
    }

    private void addDateToProjectName(String date) {
        String projectName = this.project.getProjectName();
        this.project.setProjectName(getProjectName(projectName, date));
    }

    private String getFieldValue(Node trTag) {
        Node tdTag = trTag.getLastChild();
        return tdTag.getFirstChild().getNodeValue();
    }

    private String getNodeValue(Node node) {
        Node thTag = node.getFirstChild();
        return thTag.getFirstChild().getNodeValue();
    }

    public void setSettings(BlackDuckSettings settings) {
        this.settings = settings;
    }

}
