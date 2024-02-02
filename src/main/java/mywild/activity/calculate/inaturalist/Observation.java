package mywild.activity.calculate.inaturalist;

public record Observation(
    int id,
    String uri,
    // String quality_grade,
    User user,
    // ZonedDateTime time_observed_at,
    // String observed_time_zone,
    Taxon taxon,
    // boolean captive,
    String taxon_geoprivacy,
    int positional_accuracy,
    boolean obscured,
    String location,
    boolean mappable
    // String place_guess,
    // List<Integer> place_ids,
    // List<Annotation> annotations
) {}
