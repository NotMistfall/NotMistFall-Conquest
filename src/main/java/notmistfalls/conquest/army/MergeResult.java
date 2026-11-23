package notmistfalls.conquest.army;

/** Outcome of a merge attempt. */
public sealed interface MergeResult {
    record Merged(SoldierRecord result) implements MergeResult {}

    record Rejected(MergeFailure reason) implements MergeResult {}

    default boolean success() {
        return this instanceof Merged;
    }
}
