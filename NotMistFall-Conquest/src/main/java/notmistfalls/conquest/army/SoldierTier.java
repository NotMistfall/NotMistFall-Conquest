package notmistfalls.conquest.army;

import java.util.Optional;

public enum SoldierTier {
    RECRUIT,
    VETERAN,
    ELITE,
    LEGENDARY;

    public Optional<SoldierTier> nextTier() {
        int next = ordinal() + 1;
        return next < values().length
                ? Optional.of(values()[next])
                : Optional.empty();
    }

    public boolean canMergeWith(SoldierTier other) {
        return this == other && this != LEGENDARY;
    }
}
