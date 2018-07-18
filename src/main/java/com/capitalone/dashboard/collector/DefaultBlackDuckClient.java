package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.BlackDuckProject;
import com.capitalone.dashboard.model.BlackDuck;
import com.opencsv.CSVReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import org.apache.commons.csv.CSVParser;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;
import java.nio.charset.Charset;
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

    private BlackDuckProject project = new BlackDuckProject();
    private BlackDuck blackDuckMetrics = new BlackDuck();
    private BlackDuckSettings settings;

    @Autowired
    public DefaultBlackDuckClient(BlackDuckSettings settings) {
        this.settings = settings;
        this.initializationMetrics();
    }

    @Override
    public BlackDuckProject getBlackDuckProject(String instanceUrl) {
        BlackDuckProject project = new BlackDuckProject();
        try {
            instanceUrl = getUrlWithUserData(instanceUrl);
            Document document = getDocument(instanceUrl);
            if (document != null) {
                project = getProject(document);
                project.setInstanceUrl(instanceUrl);
            }
        } catch (Exception e) {
            LOG.error(e);
        }
        return project;
    }

    @Override
    public BlackDuck currentBlackDuckMetrics(BlackDuckProject project) {
        BlackDuck blackDuck = new BlackDuck();
        try {
            initializationMetrics();
        } catch (Exception e) {
            LOG.error(e);
        }
        return blackDuck;
    }

    private void initializationMetrics() {
        this.metrics.put(NUMBER_OF_FILES, "");
        this.metrics.put(FILES_WITH_VIOLATIONS, "");
        this.metrics.put(FILES_PENDING_IDENTIFICATION, "");
    }

    private BlackDuckProject getProject(Document document) {
        BlackDuckProject project = new BlackDuckProject();
        try {
            NodeList trTags = document.getElementsByTagName(TR_TAG);
            String projectName = "";
            for (int i = 0; i < trTags.getLength(); ++i) {
                Node trTag = trTags.item(i);
                String value = getNodeValue(trTag);
                if (value.equals(PROJECT_NAME)) {
                    Node tdTag = trTag.getLastChild().getPreviousSibling();
                    projectName = tdTag.getFirstChild().getNodeValue();
                }
                if (value.equals(PROJECT_TIMESTAMP)) {
                    Node tdTag = trTag.getLastChild().getPreviousSibling();
                    String date = tdTag.getFirstChild().getNodeValue();
                    project.setProjectTimestamp(Long.toString(getTimeStamp(date)));
                    project.setProjectName(getProjectName(projectName, date));
                    break;
                }
            }
        } catch (Exception e) {
            LOG.error(e);
        }
        return project;
    }


    private String getNodeValue(Node node) {
        Node thTag = node.getFirstChild();
        return thTag.getNextSibling().getFirstChild().getNodeValue();
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

//    private void initializationMetrics() {
//        this.metrics.put(LOW, 0);
//        this.metrics.put(MEDIUM, 0);
//        this.metrics.put(HIGH, 0);
//        this.metrics.put(TOTAL, 0);
//    }

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

    private Date getProjectDate(String timestamp)
    {
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
