package com.capitalone.dashboard.collector;

import codesecurity.collectors.collector.CodeSecurityCollectorController;
import com.capitalone.dashboard.model.BlackDuck;
import com.capitalone.dashboard.model.BlackDuckCollector;
import com.capitalone.dashboard.model.BlackDuckProject;
import com.capitalone.dashboard.repository.BlackDuckProjectRepository;
import com.capitalone.dashboard.repository.BlackDuckRepository;

public class BlackDuckCollectorController extends CodeSecurityCollectorController<BlackDuckCollector, BlackDuckProject> {

    private BlackDuckProjectRepository projectRepository;
    private BlackDuckRepository dataRepository;
    private DefaultBlackDuckClient client;

    public BlackDuckCollectorController(BlackDuckProjectRepository projectRepository,
                                        BlackDuckRepository dataRepository,
                                        DefaultBlackDuckClient client) {
        this.projectRepository = projectRepository;
        this.dataRepository = dataRepository;
        this.client = client;
    }

    @Override
    protected void saveProjectToProjectRepository(BlackDuckProject project) {
        projectRepository.save(project);
    }

    @Override
    protected BlackDuckProject getAMovedProject(BlackDuckProject lhs, BlackDuckProject rhs) {
        lhs.setProjectDate(rhs.getProjectDate());
        lhs.setProjectName(rhs.getProjectName());
        lhs.setInstanceUrl(rhs.getInstanceUrl());
        lhs.setProjectTimestamp(rhs.getProjectTimestamp());
        lhs.setDescription(rhs.getProjectName());
        return lhs;
    }

    @Override
    protected BlackDuckProject enabledProject(BlackDuckCollector collector, BlackDuckProject project) {
        return projectRepository.findBlackDuckProject(collector.getId(), project.getProjectName(), project.getProjectTimestamp());
    }

    @Override
    protected void refreshCollectorItemId(BlackDuckProject currentProject, BlackDuckProject project) {
        BlackDuck blackDuck = dataRepository.findByCollectorItemIdAndTimestamp(currentProject.getId(), project.getProjectTimestamp());
        saveToDataRepository(blackDuck, project);
    }

    @Override
    protected BlackDuckProject getNewProject() {
        return new BlackDuckProject();
    }

    @Override
    protected void refreshCollectorData(BlackDuckProject project) {
        BlackDuck blackDuck = client.getCurrentMetrics(project);
        if (blackDuck != null && isNewData(project, blackDuck)) {
            saveToDataRepository(blackDuck, project);
        }
    }

    @Override
    protected BlackDuckProject getCurrentProjectFromProjectRepository(BlackDuckCollector collector) {
        return projectRepository.findCurrentProject(collector.getId(), true);
    }

    private void saveToDataRepository(BlackDuck blackDuck, BlackDuckProject project) {
        blackDuck.setCollectorItemId(project.getId());
        dataRepository.save(blackDuck);
    }

    private boolean isNewData(BlackDuckProject project, BlackDuck blackDuck) {
        return dataRepository.findByCollectorItemIdAndTimestamp(
                project.getId(), blackDuck.getTimestamp()) == null;
    }

}
