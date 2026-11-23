package notmistfalls.conquest.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import notmistfalls.conquest.army.SoldierClass;
import notmistfalls.conquest.army.SoldierTier;

/**
 * All tunable numbers: soldier limits, recruitment/training costs, training
 * rules and per-tier stats. Immutable and free of Minecraft types.
 *
 * <p>Stored as a simple {@code key=value} file. Missing keys fall back to
 * defaults, so config files written by older versions keep working. Default
 * values are placeholders for balancing and are expected to be tuned.
 */
public final class ConquestConfig {
    public record ClassStats(double maxHealth, double attackDamage) {}

    private final int maxSoldiersPerPlayer;
    private final int maxSoldiersPerGroup;
    private final int maxTrainingLevel;
    private final double trainingBonusPercentPerLevel;
    private final Map<SoldierClass, List<ItemCost>> recruitCost;
    private final List<ItemCost> trainingCost;
    private final Map<SoldierTier, Map<SoldierClass, ClassStats>> stats;
    private final Map<SoldierTier, Double> speed;

    private ConquestConfig(Builder b) {
        this.maxSoldiersPerPlayer = b.maxSoldiersPerPlayer;
        this.maxSoldiersPerGroup = b.maxSoldiersPerGroup;
        this.maxTrainingLevel = b.maxTrainingLevel;
        this.trainingBonusPercentPerLevel = b.trainingBonusPercentPerLevel;
        this.recruitCost = new EnumMap<>(b.recruitCost);
        this.trainingCost = List.copyOf(b.trainingCost);
        this.stats = new EnumMap<>(SoldierTier.class);
        for (SoldierTier t : SoldierTier.values()) {
            this.stats.put(t, new EnumMap<>(b.stats.get(t)));
        }
        this.speed = new EnumMap<>(b.speed);
        validate();
    }

    // ---- accessors -------------------------------------------------------

    public int maxSoldiersPerPlayer() { return maxSoldiersPerPlayer; }

    public int maxSoldiersPerGroup() { return maxSoldiersPerGroup; }

    public int maxTrainingLevel() { return maxTrainingLevel; }

    public double trainingBonusPercentPerLevel() { return trainingBonusPercentPerLevel; }

    public List<ItemCost> recruitCost(SoldierClass c) { return recruitCost.get(c); }

    public List<ItemCost> trainingCost() { return trainingCost; }

    public ClassStats baseStats(SoldierTier t, SoldierClass c) { return stats.get(t).get(c); }

    public double speedMultiplier(SoldierTier t) { return speed.get(t); }

    // ---- defaults, parsing, writing -------------------------------------

    public static ConquestConfig defaults() {
        return new Builder().build();
    }

    public static ConquestConfig parse(Properties p) {
        Builder b = new Builder();
        b.maxSoldiersPerPlayer = intKey(p, "limits.maxSoldiersPerPlayer", b.maxSoldiersPerPlayer);
        b.maxSoldiersPerGroup = intKey(p, "limits.maxSoldiersPerGroup", b.maxSoldiersPerGroup);
        b.maxTrainingLevel = intKey(p, "training.maxLevel", b.maxTrainingLevel);
        b.trainingBonusPercentPerLevel =
                doubleKey(p, "training.bonusPercentPerLevel", b.trainingBonusPercentPerLevel);
        for (SoldierClass c : SoldierClass.values()) {
            b.recruitCost.put(c, costKey(p, "cost.recruit." + lower(c), b.recruitCost.get(c)));
        }
        b.trainingCost = costKey(p, "cost.train", b.trainingCost);
        for (SoldierTier t : SoldierTier.values()) {
            for (SoldierClass c : SoldierClass.values()) {
                String base = "stats." + lower(t) + "." + lower(c);
                ClassStats d = b.stats.get(t).get(c);
                b.stats.get(t).put(c, new ClassStats(
                        doubleKey(p, base + ".health", d.maxHealth()),
                        doubleKey(p, base + ".damage", d.attackDamage())));
            }
            b.speed.put(t, doubleKey(p, "stats." + lower(t) + ".speed", b.speed.get(t)));
        }
        return b.build();
    }

    /** Reads the file, or writes and returns defaults if it does not exist yet. */
    public static ConquestConfig load(Path file) {
        try {
            if (!Files.exists(file)) {
                ConquestConfig defaults = defaults();
                Path parent = file.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    w.write(defaults.toFileText());
                }
                return defaults;
            }
            Properties props = new Properties();
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(r);
            }
            return parse(props);
        } catch (IOException e) {
            throw new ConfigException("Could not read config " + file + ": " + e.getMessage(), e);
        }
    }

    public String toFileText() {
        StringBuilder sb = new StringBuilder();
        sb.append("# NotMistFall: Conquest configuration\n");
        sb.append("# Costs are lists like: minecraft:emerald*4, minecraft:bread*2  (empty = free)\n");
        sb.append("# Default values are starting points for balancing.\n\n");
        sb.append("# Maximum soldiers a player may own, and per group.\n");
        sb.append("limits.maxSoldiersPerPlayer=").append(maxSoldiersPerPlayer).append('\n');
        sb.append("limits.maxSoldiersPerGroup=").append(maxSoldiersPerGroup).append("\n\n");
        sb.append("# Training improves health and damage; it is never required for merging.\n");
        sb.append("training.maxLevel=").append(maxTrainingLevel).append('\n');
        sb.append("training.bonusPercentPerLevel=").append(trainingBonusPercentPerLevel).append("\n\n");
        sb.append("# Recruitment cost per class, and cost of one training level.\n");
        for (SoldierClass c : SoldierClass.values()) {
            sb.append("cost.recruit.").append(lower(c)).append('=')
                    .append(ItemCost.format(recruitCost.get(c))).append('\n');
        }
        sb.append("cost.train=").append(ItemCost.format(trainingCost)).append("\n\n");
        sb.append("# Base stats per tier (before training bonus).\n");
        for (SoldierTier t : SoldierTier.values()) {
            for (SoldierClass c : SoldierClass.values()) {
                ClassStats s = stats.get(t).get(c);
                sb.append("stats.").append(lower(t)).append('.').append(lower(c))
                        .append(".health=").append(s.maxHealth()).append('\n');
                sb.append("stats.").append(lower(t)).append('.').append(lower(c))
                        .append(".damage=").append(s.attackDamage()).append('\n');
            }
            sb.append("stats.").append(lower(t)).append(".speed=").append(speed.get(t)).append('\n');
        }
        return sb.toString();
    }

    // ---- validation ------------------------------------------------------

    private void validate() {
        require(maxSoldiersPerPlayer >= 1, "limits.maxSoldiersPerPlayer must be at least 1");
        require(maxSoldiersPerGroup >= 1, "limits.maxSoldiersPerGroup must be at least 1");
        require(maxTrainingLevel >= 0, "training.maxLevel cannot be negative");
        require(trainingBonusPercentPerLevel >= 0 && trainingBonusPercentPerLevel <= 1000,
                "training.bonusPercentPerLevel must be between 0 and 1000");
        for (SoldierTier t : SoldierTier.values()) {
            for (SoldierClass c : SoldierClass.values()) {
                ClassStats s = stats.get(t).get(c);
                String base = "stats." + lower(t) + "." + lower(c);
                require(s.maxHealth() > 0 && s.maxHealth() <= 1024, base + ".health must be in (0, 1024]");
                require(s.attackDamage() >= 0 && s.attackDamage() <= 2048,
                        base + ".damage must be in [0, 2048]");
            }
            double sp = speed.get(t);
            require(sp > 0 && sp <= 3, "stats." + lower(t) + ".speed must be in (0, 3]");
        }
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new ConfigException(message);
        }
    }

    // ---- key readers -----------------------------------------------------

    private static String lower(Enum<?> e) {
        return e.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static int intKey(Properties p, String key, int fallback) {
        String v = p.getProperty(key);
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new ConfigException("Invalid whole number for " + key + ": '" + v + "'");
        }
    }

    private static double doubleKey(Properties p, String key, double fallback) {
        String v = p.getProperty(key);
        if (v == null) {
            return fallback;
        }
        try {
            double d = Double.parseDouble(v.trim());
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new NumberFormatException();
            }
            return d;
        } catch (NumberFormatException e) {
            throw new ConfigException("Invalid number for " + key + ": '" + v + "'");
        }
    }

    private static List<ItemCost> costKey(Properties p, String key, List<ItemCost> fallback) {
        String v = p.getProperty(key);
        if (v == null) {
            return fallback;
        }
        try {
            return ItemCost.parseList(v);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Invalid cost for " + key + ": " + e.getMessage());
        }
    }

    // ---- defaults builder ------------------------------------------------

    private static final class Builder {
        int maxSoldiersPerPlayer = 24;
        int maxSoldiersPerGroup = 24;
        int maxTrainingLevel = 5;
        double trainingBonusPercentPerLevel = 4.0;
        final Map<SoldierClass, List<ItemCost>> recruitCost = new EnumMap<>(SoldierClass.class);
        List<ItemCost> trainingCost = List.of(new ItemCost("minecraft:emerald", 3));
        final Map<SoldierTier, Map<SoldierClass, ClassStats>> stats = new EnumMap<>(SoldierTier.class);
        final Map<SoldierTier, Double> speed = new EnumMap<>(SoldierTier.class);

        Builder() {
            recruitCost.put(SoldierClass.SWORDSMAN,
                    List.of(new ItemCost("minecraft:emerald", 4), new ItemCost("minecraft:bread", 2)));
            recruitCost.put(SoldierClass.ARCHER,
                    List.of(new ItemCost("minecraft:emerald", 4), new ItemCost("minecraft:arrow", 16)));
            double[] health = {20, 28, 40, 60};
            double[] swordDamage = {3, 4, 6, 8};
            double[] bowDamage = {2, 3, 4, 6};
            double[] spd = {1.00, 1.05, 1.10, 1.15};
            for (SoldierTier t : SoldierTier.values()) {
                int i = t.ordinal();
                Map<SoldierClass, ClassStats> m = new EnumMap<>(SoldierClass.class);
                m.put(SoldierClass.SWORDSMAN, new ClassStats(health[i], swordDamage[i]));
                m.put(SoldierClass.ARCHER, new ClassStats(health[i], bowDamage[i]));
                stats.put(t, m);
                speed.put(t, spd[i]);
            }
        }

        ConquestConfig build() {
            return new ConquestConfig(this);
        }
    }
}
