package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuckProject;
import com.capitalone.dashboard.model.BlackDuck;
import codesecurity.collectors.collector.DefaultCodeSecurityClient;
import codesecurity.config.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import java.util.*;

@Component("DefaultBlackDuckClient")
public class DefaultBlackDuckClient extends DefaultCodeSecurityClient<BlackDuck, BlackDuckProject> {
    private static final String PROJECT_TIMESTAMP = "Last Updated:";
    private static final String PROJECT_NAME = "Name:";
    private static final String TR_TAG = "tr";
    private static final String DATE_FORMAT = "MMMM d, yyyy h:mm a";
    private static final String PERCENT = "%";
    private static final String COMMA = ",";
    private static final String EMPTY_STRING = "";
    private static final Integer TWO_CHARACTER_SPACE = 2;

    private BlackDuck blackDuck;
    private BlackDuckProject project;
    private Map<String, Integer> metrics = new HashMap<>();
    private Map<String, Double> percentages = new HashMap<>();
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
        this.metrics.put(Constants.BlackDuck.NUMBER_OF_FILES, 0);
        this.metrics.put(Constants.BlackDuck.FILES_WITH_VIOLATIONS, 0);
        this.metrics.put(Constants.BlackDuck.FILES_PENDING_IDENTIFICATION, 0);
        this.percentages.put(Constants.BlackDuck.FILES_PENDING_IDENTIFICATION, 1E-12);
        this.percentages.put(Constants.BlackDuck.FILES_WITH_VIOLATIONS, 1E-12);
    }

    private void setBlackDuckMetrics() {
        this.blackDuck.setName(project.getProjectName());
        this.blackDuck.setMetrics(this.metrics);
        this.blackDuck.setPercentages(this.percentages);
        this.blackDuck.setUrl(project.getInstanceUrl());
        this.blackDuck.setTimestamp(project.getProjectTimestamp());
    }

    private void checkNodeValue(String fieldName, Node trTag) {
        switch (fieldName) {
            case PROJECT_NAME:
                updateProjectName(trTag);
                break;
            case Constants.BlackDuck.NUMBER_OF_FILES:
                updateNumberOfFiles(trTag);
                break;
            case Constants.BlackDuck.FILES_PENDING_IDENTIFICATION:
                updateNumberOfFilesPendingIdentification(trTag);
                break;
            case Constants.BlackDuck.FILES_WITH_VIOLATIONS:
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
        String value = getFieldValue(trTag);
        value = value.replace(COMMA, EMPTY_STRING);
        this.metrics.put(Constants.BlackDuck.NUMBER_OF_FILES, Integer.parseInt(value));
    }

    private void updateNumberOfFilesPendingIdentification(Node trTag) {
        List<String> metricsList = getMetricsList(getFieldValue(trTag));
        this.metrics.put(Constants.BlackDuck.FILES_PENDING_IDENTIFICATION, Integer.parseInt(metricsList.get(0)));
        this.percentages.put(Constants.BlackDuck.FILES_PENDING_IDENTIFICATION, Double.parseDouble(metricsList.get(1)));
    }

    private void updateNumberOfFilesWithViolations(Node trTag) {
        List<String> metricsList = getMetricsList(getFieldValue(trTag));
        this.metrics.put(Constants.BlackDuck.FILES_WITH_VIOLATIONS, Integer.parseInt(metricsList.get(0)));
        this.percentages.put(Constants.BlackDuck.FILES_WITH_VIOLATIONS, Double.parseDouble(metricsList.get(1)));
    }

    private List<String> getMetricsList(String value) {
        List<String> metricsList = new ArrayList<>();
        int indexOfOpenBracket = value.indexOf(' ');
        metricsList.add(getNumber(value, indexOfOpenBracket));
        metricsList.add(getPercent(value, indexOfOpenBracket));
        return metricsList;
    }

    private String getPercent(String str, Integer indexOfOpenBracket) {
        return str.substring(indexOfOpenBracket + TWO_CHARACTER_SPACE, str.indexOf(PERCENT));
    }

    private String getNumber(String str, Integer indexOfOpenBracket) {
        return str.substring(0, indexOfOpenBracket).replace(COMMA,  EMPTY_STRING);
    }

    private void updateProjectTimestamp(Node trTag) {
        String date = getFieldValue(trTag);
        this.project.setProjectDate(getProjectDate(date));
        this.project.setProjectTimestamp(getTimeStamp(date));
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
