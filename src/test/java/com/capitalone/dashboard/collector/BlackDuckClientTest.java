package com.capitalone.dashboard.collector;

import codesecurity.config.Constants;
import com.capitalone.dashboard.model.BlackDuckProject;
import com.capitalone.dashboard.model.BlackDuck;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.xml.sax.SAXParseException;

import java.text.ParseException;
import java.util.Map;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BlackDuckClientTest extends BlackDuckTestUtils {
    @Mock
    private BlackDuckSettings settings;
    private DefaultBlackDuckClient blackDuckClient;

    private static final String SERVER = "blackduck-tests/test.html";
    private static final String EMPTY_SERVER = "blackduck-tests/empty-test.html";
    private static final String FAIL_SERVER = "blackduck-tests/fail-test.html";
    private static final String CRON = "0 0/1 * * * *";

    private static final String NUMBER_OF_FILES = "Number of Files:";
    private static final String FILES_WITH_VIOLATIONS = "Files with Violations:";
    private static final String FILES_PENDING_IDENTIFICATION = "Files Pending Identification:";


    @Before
    public void init() {
        settings = new BlackDuckSettings();
        settings.setCron(CRON);
        settings.setServer(getUrlToTestFile(SERVER));
        settings.setPassword("");
        settings.setUsername("");

        blackDuckClient = new DefaultBlackDuckClient(settings);
    }

    @Test
    public void canGetProjects() {
        blackDuckClient.parseDocument(settings.getServer());
        BlackDuckProject project = blackDuckClient.getProject();
        BlackDuckProject expectedProject = getExpectedBlackDuckProject();
        assertEquals(project, expectedProject);
        assertTrue(project.equals(expectedProject));
    }

    @Test
    public void canGetCurrentBlackDuckMetrics() {
        blackDuckClient.parseDocument(settings.getServer());

        BlackDuckProject project = blackDuckClient.getProject();
        BlackDuck blackDuck = blackDuckClient.getCurrentMetrics(project);
        Map<String, Integer> metrics = blackDuck.getMetrics();
        assertThat(metrics.get(Constants.BlackDuck.NUMBER_OF_FILES)).isEqualTo(48244);
        assertThat(metrics.get(Constants.BlackDuck.FILES_WITH_VIOLATIONS)).isEqualTo(36);
        assertThat(metrics.get(Constants.BlackDuck.FILES_PENDING_IDENTIFICATION)).isEqualTo(42038);
        Map<String, Double> percentages = blackDuck.getPercentages();
        assertThat(percentages.get(Constants.BlackDuck.FILES_PENDING_IDENTIFICATION)).isEqualTo(87.14);
        assertThat(percentages.get(Constants.BlackDuck.FILES_WITH_VIOLATIONS)).isEqualTo(0.07);
    }

    @Test
    public void throwNullPointerExceptionWhenCanNotGetABlackDuckReport() {
        blackDuckClient.parseDocument(getUrlToTestFile(EMPTY_SERVER));
    }

    @Test
    public void throwParseExceptionWhenBlackDuckReportDoesNotContainMetrics() {
        blackDuckClient.parseDocument(getUrlToTestFile(FAIL_SERVER));
    }

    protected String getUrl(String server) {
        return getUrlToTestFile(server);
    }

    protected String getServer() {
        return SERVER;
    }
}
