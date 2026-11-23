package notmistfalls.conquest.army;

import notmistfalls.conquest.config.ConquestConfig;

/**
 * Training raises a soldier's training level, which improves stats. It is never
 * required for merging. The level is capped by the configured maximum.
 */
public final class Training {
    private Training() {}

    public static boolean canTrain(SoldierRecord soldier, ConquestConfig config) {
        return soldier.trainingLevel() < config.maxTrainingLevel();
    }

    /** Returns the soldier one training level higher. */
    public static SoldierRecord train(SoldierRecord soldier, ConquestConfig config) {
        if (!canTrain(soldier, config)) {
            throw new IllegalStateException("Soldier is already at the maximum training level.");
        }
        return soldier.withTrainingLevel(soldier.trainingLevel() + 1);
    }

    /**
     * Base stats for the tier and class, with health and damage multiplied by
     * {@code 1 + bonusPercentPerLevel/100 * trainingLevel}. Speed is not affected.
     */
    public static SoldierStats statsFor(SoldierRecord soldier, ConquestConfig config) {
        ConquestConfig.ClassStats base = config.baseStats(soldier.tier(), soldier.soldierClass());
        double factor = 1.0 + (config.trainingBonusPercentPerLevel() / 100.0) * soldier.trainingLevel();
        return new SoldierStats(
                base.maxHealth() * factor,
                base.attackDamage() * factor,
                config.speedMultiplier(soldier.tier()));
    }
}
