package mywild.activity.calculate.inaturalist;

import java.util.List;

public record Observations(
    int total_results,
    int page,
    int per_page,
    List<Observation> results
) {}
