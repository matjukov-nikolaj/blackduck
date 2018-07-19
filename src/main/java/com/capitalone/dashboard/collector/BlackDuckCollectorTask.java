package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.*;
import com.capitalone.dashboard.repository.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BlackDuckCollectorTask extends CollectorTask<BlackDuckCollector> {
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"})
    private static final Log LOG = LogFactory.getLog(BlackDuckCollectorTask.class);

    private final BlackDuckCollectorRepository blackDuckCollectorRepository;
    private final BlackDuckProjectRepository blackDuckProjectRepository;
    private final BlackDuckRepository blackDuckRepository;
    private final DefaultBlackDuckClient blackDuckClient;
    private final BlackDuckSettings blackDuckSettings;
    private final ComponentRepository dbComponentRepository;

    @Autowired
    public BlackDuckCollectorTask(TaskScheduler taskScheduler,
                                  BlackDuckCollectorRepository blackDuckCollectorRepository,
                                  BlackDuckProjectRepository blackDuckProjectRepository,
                                  BlackDuckRepository blackDuckRepository,
                                  DefaultBlackDuckClient blackDuckClient,
                                  BlackDuckSettings blackDuckSettings,
                                  ComponentRepository dbComponentRepository) {
        super(taskScheduler, "BlackDuck");
        this.blackDuckCollectorRepository = blackDuckCollectorRepository;
        this.blackDuckProjectRepository = blackDuckProjectRepository;
        this.blackDuckRepository = blackDuckRepository;
        this.blackDuckClient = blackDuckClient;
        this.blackDuckSettings = blackDuckSettings;
        this.dbComponentRepository = dbComponentRepository;
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
        List<BlackDuckProject> latestProjects = new ArrayList<>();
        clean(collector, existingProjects);

        String instanceUrl = collector.getBlackDuckServer();
        blackDuckClient.parseDocument(instanceUrl);
        BlackDuckProject project = blackDuckClient.getProject();
        logBanner("Fetched project: " + project.getProjectName());
        latestProjects.add(project);
        if (isNewProject(project, existingProjects)) {
            addNewProject(project, collector);
        }
        refreshData(enabledProject(collector, project));
    }

    private boolean isNewProject(BlackDuckProject project, List<BlackDuckProject> existingProjects) {
        return (!existingProjects.contains(project));
    }

    private void addNewProject(BlackDuckProject project, BlackDuckCollector collector) {
        project.setCollectorId(collector.getId());
        project.setEnabled(false);
        project.setDescription(project.getProjectName());
        blackDuckProjectRepository.save(project);
    }

    private void refreshData(BlackDuckProject project) {
        BlackDuck blackDuck = blackDuckClient.getCurrentMetrics(project);
        if (blackDuck != null && isNewCheckMarxData(project, blackDuck)) {
            blackDuck.setCollectorItemId(project.getId());
            blackDuckRepository.save(blackDuck);
        }
    }

    private BlackDuckProject enabledProject(BlackDuckCollector collector, BlackDuckProject project) {
        return blackDuckProjectRepository.findBlackDuckProject(collector.getId(), project.getProjectName(), project.getProjectTimestamp());
    }

    private boolean isNewCheckMarxData(BlackDuckProject project, BlackDuck blackDuck) {
        return blackDuckRepository.findByCollectorItemIdAndTimestamp(
                project.getId(), blackDuck.getTimestamp()) == null;
    }

    /**
     * Clean up unused sonar collector items
     *
     * @param collector the {@link BlackDuckCollector}
     */

    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private void clean(BlackDuckCollector collector, List<BlackDuckProject> existingProjects) {
        Set<ObjectId> uniqueIDs = getUniqueIds(collector);
        List<BlackDuckProject> stateChangeJobList = new ArrayList<>();
        for (BlackDuckProject job : existingProjects) {
            if ((job.isEnabled() && !uniqueIDs.contains(job.getId())) ||  // if it was enabled but not on a dashboard
                    (!job.isEnabled() && uniqueIDs.contains(job.getId()))) { // OR it was disabled and now on a dashboard
                job.setEnabled(uniqueIDs.contains(job.getId()));
                stateChangeJobList.add(job);
            }
        }
        if (!CollectionUtils.isEmpty(stateChangeJobList)) {
            blackDuckProjectRepository.save(stateChangeJobList);
        }
    }

    private Set<ObjectId> getUniqueIds(BlackDuckCollector collector) {
        Set<ObjectId> uniqueIDs = new HashSet<>();
        for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
                .findAll()) {
            if (comp.getCollectorItems().isEmpty()) continue;
            List<CollectorItem> itemList = comp.getCollectorItems().get(CollectorType.BlackDuck);
            if (CollectionUtils.isEmpty(itemList)) continue;

            for (CollectorItem ci : itemList) {
                if (collector.getId().equals(ci.getCollectorId())) {
                    uniqueIDs.add(ci.getId());
                }
            }
        }
        return uniqueIDs;
    }
}
