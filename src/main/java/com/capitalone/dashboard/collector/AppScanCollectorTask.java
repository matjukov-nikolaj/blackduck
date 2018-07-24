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

import java.util.*;

@Component
public class AppScanCollectorTask extends CollectorTask<AppScanCollector> {
    @SuppressWarnings({"PMD.UnusedPrivateField", "unused"})
    private static final Log LOG = LogFactory.getLog(AppScanCollectorTask.class);

    private final AppScanCollectorRepository appScanCollectorRepository;
    private final AppScanProjectRepository appScanProjectRepository;
    private final AppScanRepository appScanRepository;
    private final DefaultAppScanClient appScanClient;
    private final AppScanSettings appScanSettings;
    private final ComponentRepository dbComponentRepository;

    @Autowired
    public AppScanCollectorTask(TaskScheduler taskScheduler,
                                AppScanCollectorRepository appScanCollectorRepository,
                                AppScanProjectRepository appScanProjectRepository,
                                AppScanRepository appScanRepository,
                                DefaultAppScanClient appScanClient,
                                AppScanSettings appScanSettings,
                                ComponentRepository dbComponentRepository) {
        super(taskScheduler, "AppScan");
        this.appScanCollectorRepository = appScanCollectorRepository;
        this.appScanProjectRepository = appScanProjectRepository;
        this.appScanRepository = appScanRepository;
        this.appScanClient = appScanClient;
        this.appScanSettings = appScanSettings;
        this.dbComponentRepository = dbComponentRepository;
    }

    @Override
    public AppScanCollector getCollector() {
        return AppScanCollector.prototype(appScanSettings.getServer());
    }

    @Override
    public BaseCollectorRepository<AppScanCollector> getCollectorRepository() {
        return appScanCollectorRepository;
    }

    @Override

    public String getCron() {
        return appScanSettings.getCron();
    }

    @Override
    public void collect(AppScanCollector collector) {
        if (collector.getBlackDuckServer().isEmpty()) {
            return;
        }
        Set<ObjectId> udId = new HashSet<>();
        udId.add(collector.getId());
        List<AppScanProject> existingProjects = appScanProjectRepository.findByCollectorIdIn(udId);
        clean(collector, existingProjects);

        String instanceUrl = collector.getBlackDuckServer();
        appScanClient.parseDocument(instanceUrl);
        AppScanProject project = appScanClient.getProject();
        logBanner("Fetched project: " + project.getProjectName());
        if (isNewProject(project, existingProjects)) {
            addNewProject(project, collector);
        }
        refreshData(enabledProject(collector, project));
    }

    private boolean isNewProject(AppScanProject project, List<AppScanProject> existingProjects) {
        return (!existingProjects.contains(project));
    }

    private void addNewProject(AppScanProject project, AppScanCollector collector) {
        project.setCollectorId(collector.getId());
        project.setEnabled(false);
        project.setDescription(project.getProjectName());
        appScanProjectRepository.save(project);
    }

    private void refreshData(AppScanProject project) {
        AppScan appScan = appScanClient.getCurrentMetrics(project);
        if (appScan != null && isNewData(project, appScan)) {
            appScan.setCollectorItemId(project.getId());
            appScanRepository.save(appScan);
        }
    }

    private AppScanProject enabledProject(AppScanCollector collector, AppScanProject project) {
        return appScanProjectRepository.findBlackDuckProject(collector.getId(), project.getProjectName(), project.getProjectTimestamp());
    }

    private boolean isNewData(AppScanProject project, AppScan appScan) {
        return appScanRepository.findByCollectorItemIdAndTimestamp(
                project.getId(), appScan.getTimestamp()) == null;
    }

    /**
     * Clean up unused sonar collector items
     *
     * @param collector the {@link AppScanCollector}
     */

    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    private void clean(AppScanCollector collector, List<AppScanProject> existingProjects) {
        Set<ObjectId> uniqueIDs = getUniqueIds(collector);
        List<AppScanProject> stateChangeJobList = new ArrayList<>();
        for (AppScanProject job : existingProjects) {
            if ((job.isEnabled() && !uniqueIDs.contains(job.getId())) ||  // if it was enabled but not on a dashboard
                    (!job.isEnabled() && uniqueIDs.contains(job.getId()))) { // OR it was disabled and now on a dashboard
                job.setEnabled(uniqueIDs.contains(job.getId()));
                stateChangeJobList.add(job);
            }
        }
        if (!CollectionUtils.isEmpty(stateChangeJobList)) {
            appScanProjectRepository.save(stateChangeJobList);
        }
    }

    private Set<ObjectId> getUniqueIds(AppScanCollector collector) {
        Set<ObjectId> uniqueIDs = new HashSet<>();
        for (com.capitalone.dashboard.model.Component comp : dbComponentRepository
                .findAll()) {
            if (comp.getCollectorItems().isEmpty()) continue;
            List<CollectorItem> itemList = comp.getCollectorItems().get(CollectorType.AppScan);
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
