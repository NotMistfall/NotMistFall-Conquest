package notmistfalls.conquest.army;

/** Why a merge was refused. */
public enum MergeFailure {
    SAME_SOLDIER,
    CLASS_MISMATCH,
    TIER_MISMATCH,
    MAX_TIER,
    /** Used by {@link Army} when one of the soldiers is not in that army. */
    NOT_IN_ARMY
}
