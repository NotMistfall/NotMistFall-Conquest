package notmistfalls.conquest.army;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent description of one soldier. Immutable; changes produce new records.
 *
 * <p>The original two-argument constructor is kept for compatibility and
 * defaults to an untrained swordsman.
 */
public record SoldierRecord(UUID id, SoldierTier tier, SoldierClass soldierClass, int trainingLevel) {
    public SoldierRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(soldierClass, "soldierClass");
        if (trainingLevel < 0) {
            throw new IllegalArgumentException("Training level cannot be negative.");
        }
    }

    public SoldierRecord(UUID id, SoldierTier tier) {
        this(id, tier, SoldierClass.SWORDSMAN, 0);
    }

    /** A brand-new, untrained Recruit of the given class. */
    public static SoldierRecord recruit(UUID id, SoldierClass soldierClass) {
        return new SoldierRecord(id, SoldierTier.RECRUIT, soldierClass, 0);
    }

    public SoldierRecord withTrainingLevel(int level) {
        return new SoldierRecord(id, tier, soldierClass, level);
    }
}
