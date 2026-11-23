package notmistfalls.conquest.army;

import java.util.Objects;
import java.util.UUID;

/**
 * Merge rule: two different soldiers of the same class and the same tier become
 * one soldier of the same class, one tier higher. Legendary cannot merge.
 *
 * <p>The merged soldier starts with training level 0 (training is not carried over).
 */
public final class SoldierMerge {
    private SoldierMerge() {}

    public static MergeResult merge(SoldierRecord a, SoldierRecord b, UUID newId) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(newId, "newId");

        if (a.id().equals(b.id())) {
            return new MergeResult.Rejected(MergeFailure.SAME_SOLDIER);
        }
        if (a.soldierClass() != b.soldierClass()) {
            return new MergeResult.Rejected(MergeFailure.CLASS_MISMATCH);
        }
        if (a.tier() != b.tier()) {
            return new MergeResult.Rejected(MergeFailure.TIER_MISMATCH);
        }
        if (!a.tier().canMergeWith(b.tier())) {
            return new MergeResult.Rejected(MergeFailure.MAX_TIER);
        }
        SoldierTier next = a.tier().nextTier().orElseThrow();
        return new MergeResult.Merged(new SoldierRecord(newId, next, a.soldierClass(), 0));
    }
}
