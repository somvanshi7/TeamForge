package com.teamforge.dashboard;

import lombok.Builder;

import java.util.Map;
import java.util.List;
import java.time.LocalDate;

@Builder
public record DashboardStatsDto(
    long activeProjects,
    long skillsetCount,
    long skillGapCount,
    Map<String, Long> skillsetDistribution,   // skill name -> employees who have it
    Map<String, Long> skillProjectUsage,      // skill name -> number of projects (any status) requiring it
    Map<String, Long> skillGapDistribution,   // trending/gap skill name -> employees who have it
    Map<String, Long> skillGapDemand,         // trending/gap skill name -> active/planning projects requiring it
    List<ProjectProgressDto> projectProgress
) {
    public record ProjectProgressDto(
        Long id,
        String title,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        Integer percentComplete,
        Long daysRemaining
    ) {}
}
