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

    private static final String PROJECT_TIMESTAMP = "ProjectTimestamp";
    private static final String PROJECT_NAME = "ProjectName";
    private static final String SCAN_START = "ScanStart";
    private static final String PROTOCOL_SPLITTER = "://";
    private static final String DATE_FORMAT = "EEEE, MMMM d, yyyy h:mm:ss a";

    private Map<String, String> metrics = new HashMap<>();
    private BlackDuckSettings settings;

    @Autowired
    public DefaultBlackDuckClient(BlackDuckSettings settings) {
        this.settings = settings;
    }

    @Override
    public BlackDuckProject getProject(String instanceUrl) {
        BlackDuckProject project = new BlackDuckProject();
        try {
            instanceUrl = getUrlWithUserData(instanceUrl);
            URL url = new URL(instanceUrl);
            URLConnection urlConn = url.openConnection();
            InputStreamReader inputCSV = new InputStreamReader(
                    ((URLConnection) urlConn).getInputStream());
            CSVParser parser = new CSVParser(inputCSV, CSVFormat.EXCEL);
            String check, name, time;
            for (CSVRecord csvRecord : parser) {
                check = csvRecord.get(1);

                name = csvRecord.get(2);
                time = csvRecord.get(3);
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
            blackDuck.setName("lol");
        } catch (Exception e) {
            LOG.error(e);
        }
        return blackDuck;
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
