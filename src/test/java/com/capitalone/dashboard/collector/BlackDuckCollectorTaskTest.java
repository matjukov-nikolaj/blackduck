package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BlackDuckCollectorTaskTest extends BlackDuckTestUtils {

    private BlackDuckCollectorTask task;

    private TaskScheduler mockScheduler;
    private BlackDuckCollectorRepository mockCollectorRepository;
    private BlackDuckProjectRepository mockProjectRepository;
    private BlackDuckRepository mockRepository;
    private ComponentRepository mockComponentRepository;
    private DefaultBlackDuckClient client;

    private static final String SERVER = "blackduck-tests/test.html";
    private static final String CRON = "0 0/1 * * * *";

    @Before
    public void setup() {
        mockScheduler = mock(TaskScheduler.class);
        mockCollectorRepository = mock(BlackDuckCollectorRepository.class);
        mockProjectRepository = mock(BlackDuckProjectRepository.class);
        mockRepository = mock(BlackDuckRepository.class);
        mockComponentRepository = mock(ComponentRepository.class);

        BlackDuckSettings settings = new BlackDuckSettings();
        settings.setCron(CRON);
        settings.setServer(getUrlToTestFile(SERVER));
        settings.setUsername("");
        settings.setPassword("");

        client = new DefaultBlackDuckClient(settings);
        this.task = new BlackDuckCollectorTask(mockScheduler, mockCollectorRepository, mockProjectRepository,
                mockRepository, client, settings, mockComponentRepository);
    }

    @Test
    public void getCollectorReturnsBlackDuckCollector() {
        final BlackDuckCollector collector = task.getCollector();

        AssertionsForClassTypes.assertThat(collector).isNotNull().isInstanceOf(BlackDuckCollector.class);
        AssertionsForClassTypes.assertThat(collector.isEnabled()).isTrue();
        AssertionsForClassTypes.assertThat(collector.isOnline()).isTrue();
        AssertionsForClassTypes.assertThat(collector.getBlackDuckServer()).contains(getUrlToTestFile(SERVER));
        assertThat(collector.getCollectorType()).isEqualTo(CollectorType.BlackDuck);
        AssertionsForClassTypes.assertThat(collector.getName()).isEqualTo("BlackDuck");
        AssertionsForClassTypes.assertThat(collector.getAllFields().get("instanceUrl")).isEqualTo("");
        AssertionsForClassTypes.assertThat(collector.getAllFields().get("projectName")).isEqualTo("");
        AssertionsForClassTypes.assertThat(collector.getAllFields().get("projectTimestamp")).isEqualTo("");
        AssertionsForClassTypes.assertThat(collector.getUniqueFields().get("instanceUrl")).isEqualTo("");
        AssertionsForClassTypes.assertThat(collector.getUniqueFields().get("projectName")).isEqualTo("");
    }

    @Test
    public void getCollectorRepositoryReturnsTheRepository() {
        AssertionsForClassTypes.assertThat(task.getCollectorRepository()).isNotNull().isSameAs(mockCollectorRepository);
    }

    @Test
    public void getCron() {
        AssertionsForClassTypes.assertThat(task.getCron()).isNotNull().isSameAs(CRON);
    }


    @Test
    public void collectEmpty() {
        when(mockComponentRepository.findAll()).thenReturn(components());
        task.collect(new BlackDuckCollector());
        verifyZeroInteractions(mockRepository);
    }

    @Test
    public void collectWithServer() {
        when(mockComponentRepository.findAll()).thenReturn(components());
        BlackDuckCollector collector = collectorWithServer();
        task.collect(collector);
        BlackDuckProject project = client.getProject();
        BlackDuckProject expectedProject = getExpectedBlackDuckProject();
        assertEquals(project, expectedProject);
        assertTrue(project.equals(expectedProject));
        verify(mockProjectRepository).save(project);
        verify(mockProjectRepository).findBlackDuckProject(collector.getId(), project.getProjectTimestamp(), project.getProjectName());
    }

    private ArrayList<com.capitalone.dashboard.model.Component> components() {
        ArrayList<com.capitalone.dashboard.model.Component> cArray = new ArrayList<>();
        com.capitalone.dashboard.model.Component c = new Component();
        c.setId(new ObjectId());
        c.setName("COMPONENT1");
        c.setOwner("JOHN");
        cArray.add(c);
        return cArray;
    }

    protected String getUrl(String server) {
        return getUrlToTestFile(server);
    }

    protected String getServer() {
        return SERVER;
    }

    private BlackDuckCollector collectorWithServer() {
        return BlackDuckCollector.prototype(getUrlToTestFile(SERVER));
    }
}
