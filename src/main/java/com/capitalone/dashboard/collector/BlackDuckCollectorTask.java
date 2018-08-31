package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BlackDuckCollectorTask extends CollectorTask<BlackDuckCollector> {
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"})
    private static final Log LOG = LogFactory.getLog(BlackDuckCollectorTask.class);

    private final BlackDuckCollectorRepository blackDuckCollectorRepository;
    private final BlackDuckProjectRepository blackDuckProjectRepository;
    private final DefaultBlackDuckClient blackDuckClient;
    private final BlackDuckSettings blackDuckSettings;

    private final BlackDuckCollectorController collectorController;

    @Autowired
    public BlackDuckCollectorTask(TaskScheduler taskScheduler,
                                  BlackDuckCollectorRepository blackDuckCollectorRepository,
                                  BlackDuckProjectRepository blackDuckProjectRepository,
                                  BlackDuckRepository blackDuckRepository,
                                  DefaultBlackDuckClient blackDuckClient,
                                  BlackDuckSettings blackDuckSettings) {
        super(taskScheduler, "BlackDuck");
        this.blackDuckCollectorRepository = blackDuckCollectorRepository;
        this.blackDuckProjectRepository = blackDuckProjectRepository;
        this.blackDuckClient = blackDuckClient;
        this.blackDuckSettings = blackDuckSettings;

        this.collectorController = new BlackDuckCollectorController(blackDuckProjectRepository, blackDuckRepository, blackDuckClient);
    }

    @Override
    public BlackDuckCollector getCollector() {
        return BlackDuckCollector.prototype(blackDuckSettings.getServer());
    }

    @Override
    public BaseCollectorRepository<BlackDuckCollector> getCollectorRepository() {
        return blackDuckCollectorRepository;
    }

    @Override
    public String getCron() {
        return blackDuckSettings.getCron();
    }

    @Override
    public void collect(BlackDuckCollector collector) {
        if (collector.getBlackDuckServer().isEmpty()) {
            return;
        }
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        List<BlackDuckProject> existingProjects = blackDuckProjectRepository.findByCollectorIdIn(udId);

        String instanceUrl = collector.getBlackDuckServer();
        blackDuckClient.parseDocument(instanceUrl);
        BlackDuckProject project = blackDuckClient.getProject();
        logBanner("Fetched project: " + project.getProjectName() + ":" + project.getProjectDate());
        if (this.collectorController.isNewProject(project, existingProjects)) {
            this.collectorController.addNewProject(project, collector, existingProjects);
        }
    }
}
