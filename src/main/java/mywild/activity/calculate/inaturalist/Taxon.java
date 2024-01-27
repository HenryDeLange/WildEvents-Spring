package mywild.activity.calculate.inaturalist;

public record Taxon(
    int id,
    boolean is_active,
    String min_species_ancestry,
    boolean endemic,
    boolean threatened,
    int rank_level,
    String rank,
    boolean introduced,
    String preferred_common_name,
    String name
) {}
