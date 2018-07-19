package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuckProject;
import com.capitalone.dashboard.model.BlackDuck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import java.text.SimpleDateFormat;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.*;

@Component("DefaultBlackDuckClient")
public class DefaultBlackDuckClient implements BlackDuckClient {
    private static final Log LOG = LogFactory.getLog(DefaultBlackDuckClient.class);

    private static final String PROJECT_TIMESTAMP = "Last Updated:";
    private static final String PROJECT_NAME = "Name:";
    private static final String TR_TAG = "tr";
    private static final String NUMBER_OF_FILES = "Number of Files:";
    private static final String FILES_WITH_VIOLATIONS = "Files with Violations:";
    private static final String FILES_PENDING_IDENTIFICATION = "Files Pending Identification:";
    private static final String PROTOCOL_SPLITTER = "://";
    private static final String DATE_FORMAT = "MMMM d, yyyy h:mm a";
    private Map<String, String> metrics = new HashMap<>();

    private BlackDuckProject project;
    private BlackDuck blackDuck;
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
        this.blackDuck.setName(project.getProjectName());
        this.blackDuck.setMetrics(this.metrics);
        this.blackDuck.setUrl(project.getInstanceUrl());
        this.blackDuck.setTimestamp(Long.parseLong(project.getProjectTimestamp(), 10));
        return this.blackDuck;
    }

    @Override
    public void parseDocument(String instanceUrl) {
        try {
            this.initializationMetrics();
            instanceUrl = getUrlWithUserData(instanceUrl);
            Document document = getDocument(instanceUrl);
            if (document != null) {
                parseBlackDuckDocument(document);
                this.project.setInstanceUrl(instanceUrl);
            }
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private void parseBlackDuckDocument(Document document) {
        NodeList trTags = document.getElementsByTagName(TR_TAG);
        for (int i = 0; i < trTags.getLength(); ++i) {
            Node trTag = trTags.item(i);
            String fieldName = getNodeValue(trTag);
            checkNodeValue(fieldName, trTag);
            if (this.project.getProjectTimestamp() != null){
                break;
            }
        }
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

    private void initializationMetrics() {
        this.project = new BlackDuckProject();
        this.blackDuck = new BlackDuck();
        this.metrics.put(NUMBER_OF_FILES, "");
        this.metrics.put(FILES_WITH_VIOLATIONS, "");
        this.metrics.put(FILES_PENDING_IDENTIFICATION, "");
    }

    private String getNodeValue(Node node) {
        Node thTag = node.getFirstChild();
        return thTag.getFirstChild().getNodeValue();
    }

    private String getProjectName(String name, String testingDate) {
        Date date = getProjectDate(testingDate);
        if (date == null) {
            return name;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        return name + ":"
                + calendar.get(Calendar.YEAR) + "-"
                + calendar.get(Calendar.MONTH) + "-"
                + calendar.get(Calendar.DAY_OF_MONTH);
    }

    private Document getDocument(String instanceUrl) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            URL url = new URL(instanceUrl);
            Document document = db.parse(url.openStream());
            document.getDocumentElement().normalize();
            return document;

        } catch (Exception e) {
            LOG.error("Could not parse document from: " + instanceUrl, e);
        }
        return null;
    }

    public void setSettings(BlackDuckSettings settings) {
        this.settings = settings;
    }

    private long getTimeStamp(String timestamp) {
        if (!timestamp.equals("")) {
            try {
                Date date = getProjectDate(timestamp);
                return date != null ? date.getTime() : 0;
            } catch (NullPointerException e) {
                LOG.error(e);
            }
        }
        return 0;
    }

    private Date getProjectDate(String timestamp) {
        try {
            return new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH).parse(timestamp);
        } catch (java.text.ParseException e) {
            LOG.error(timestamp + " is not in expected format " + DATE_FORMAT, e);
        }
        return null;
    }

    private String getUrlWithUserData(String url) {
        StringBuilder strBuilderUrl = new StringBuilder(url);
        String username = settings.getUsername();
        String password = settings.getPassword();
        if (!username.isEmpty() && !password.isEmpty()) {
            int indexOfProtocolEnd = strBuilderUrl.lastIndexOf(PROTOCOL_SPLITTER) + PROTOCOL_SPLITTER.length();
            String userData = username + ":" + password + "@";
            strBuilderUrl.insert(indexOfProtocolEnd, userData);
        }
        return strBuilderUrl.toString();
    }
}
