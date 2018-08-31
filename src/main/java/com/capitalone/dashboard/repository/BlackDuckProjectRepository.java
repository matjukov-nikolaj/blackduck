package com.capitalone.dashboard.repository;

import com.capitalone.dashboard.model.BlackDuckProject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface BlackDuckProjectRepository extends BaseCollectorItemRepository<BlackDuckProject> {

    @Query(value="{ 'collectorId' : ?0, options.projectName : ?1, options.projectTimestamp : ?2}")
    BlackDuckProject findBlackDuckProject(ObjectId collectorId, String projectName, Long timestamp);

    @Query(value="{ 'collectorId' : ?0, options.current : ?1}")
    BlackDuckProject findCurrentProject(ObjectId collectorId, Boolean current);

    @Query(value="{ 'collectorId' : ?0, options.instanceUrl : ?1, enabled: true}")
    List<BlackDuckProject> findEnabledProjects(ObjectId collectorId, String instanceUrl);
}
