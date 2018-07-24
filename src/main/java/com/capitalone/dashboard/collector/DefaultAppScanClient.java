package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.AppScanProject;
import com.capitalone.dashboard.model.AppScan;
import codesecurity.collectors.collector.DefaultCodeSecurityClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component("DefaultAppScanClient")
public class DefaultAppScanClient extends DefaultCodeSecurityClient<AppScan, AppScanProject> {
    private static final Log LOG = LogFactory.getLog(DefaultAppScanClient.class);

    private static final String INFORMATIONAL = "TotalInformationalIssues";
    private static final String LOW = "TotalLowSeverityIssues";
    private static final String MEDIUM = "TotalMediumSeverityIssues";
    private static final String HIGH = "TotalHighSeverityIssues";
    private static final String TOTAL = "Total";
    private static final String XML_REPORT = "XmlReport";
    private static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    private static final String PROJECT_NAME = "Name";

    private AppScan appScan;
    private AppScanProject project;
    private Map<String, String> metrics = new HashMap<>();
    private AppScanSettings settings;

    @Autowired
    public DefaultAppScanClient(AppScanSettings settings) {
        this.settings = settings;
    }

    @Override
    public AppScanProject getProject() { return this.project; }

    @Override
    public AppScan getCurrentMetrics(AppScanProject project) {
        return this.appScan;
    }

    public void setSettings(AppScanSettings settings) {
        this.settings = settings;
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

    protected void parseCodeSecurityDocument(Document document) {
        parseProject(document);
        parseMetrics(document);
        setAppScanMetrics();
    }

    protected void initializationFields() {
        this.project = new AppScanProject();
        this.appScan = new AppScan();
        this.metrics.put(INFORMATIONAL, "");
        this.metrics.put(LOW, "");
        this.metrics.put(MEDIUM, "");
        this.metrics.put(HIGH, "");
        this.metrics.put(TOTAL, "");
    }

    private void setAppScanMetrics() {
        this.appScan.setName(project.getProjectName());
        this.appScan.setMetrics(this.metrics);
        this.appScan.setUrl(project.getInstanceUrl());
        this.appScan.setTimestamp(Long.parseLong(project.getProjectTimestamp(), 10));
    }

    private void parseProject(Document document) {
        NodeList xmlReportTag = document.getElementsByTagName(XML_REPORT);
        String name = getValueOfNodeAttribute(xmlReportTag, PROJECT_NAME);
        String currentDate = getCurrentDate();
        this.project.setProjectName(getProjectName(name, currentDate));
        this.project.setProjectTimestamp(Long.toString(getTimeStamp(currentDate)));
    }

    private void parseMetrics(Document document) {
        this.metrics.put(INFORMATIONAL, getValueOfTag(INFORMATIONAL, document));
        this.metrics.put(LOW, getValueOfTag(LOW, document));
        this.metrics.put(MEDIUM, getValueOfTag(MEDIUM, document));
        this.metrics.put(HIGH, getValueOfTag(HIGH, document));
        this.metrics.put(TOTAL, getValueOfTag(TOTAL, document));
    }

    private String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat(getDateFormat(), Locale.ENGLISH);
        Date date = new Date();
        return dateFormat.format(date);
    }

    private String getValueOfTag(String tagName, Document document) {
        NodeList tag = document.getElementsByTagName(tagName);
        Node tagNode = tag.item(0);
        return tagNode.getFirstChild().getNodeValue();
    }

    private String getValueOfNodeAttribute(NodeList xmlReportTag, String itemName) {
        Node node = xmlReportTag.item(0);
        return node.getAttributes().getNamedItem(itemName).getNodeValue();
    }
}
