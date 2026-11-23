package notmistfalls.conquest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import notmistfalls.conquest.army.Army;
import notmistfalls.conquest.army.ArmyGroup;
import notmistfalls.conquest.army.ArmyResult;
import notmistfalls.conquest.army.MergeFailure;
import notmistfalls.conquest.army.MergeResult;
import notmistfalls.conquest.army.SoldierClass;
import notmistfalls.conquest.army.SoldierMerge;
import notmistfalls.conquest.army.SoldierRecord;
import notmistfalls.conquest.army.SoldierStats;
import notmistfalls.conquest.army.SoldierTier;
import notmistfalls.conquest.army.Training;
import notmistfalls.conquest.army.WeaponKind;
import notmistfalls.conquest.config.ConfigException;
import notmistfalls.conquest.config.ConquestConfig;
import notmistfalls.conquest.config.ItemCost;

/**
 * Dependency-free tests for the Minecraft-free core model. Run with
 * {@code gradlew coreTests} (also part of {@code gradlew build}). Exits non-zero on failure.
 */
public final class CoreTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        run("tier: existing behaviour preserved", CoreTests::tierExisting);
        run("class: swordsman sword only, archer bow only", CoreTests::weaponRules);
        run("record: legacy 2-arg constructor and validation", CoreTests::recordBasics);
        run("merge: same class+tier advances exactly one tier", CoreTests::mergeSuccess);
        run("merge: every tier steps up correctly", CoreTests::mergeAllTiers);
        run("merge: rejects different class", CoreTests::mergeClassMismatch);
        run("merge: rejects different tier", CoreTests::mergeTierMismatch);
        run("merge: rejects legendary", CoreTests::mergeLegendary);
        run("merge: rejects same soldier twice", CoreTests::mergeSameSoldier);
        run("merge: training is not required", CoreTests::mergeNoTrainingNeeded);
        run("training: cap and stat improvement", CoreTests::trainingRules);
        run("training: config max respected (incl. 0)", CoreTests::trainingConfigurable);
        run("config: defaults valid and round-trip through file", CoreTests::configRoundTrip);
        run("config: custom limits and costs are read", CoreTests::configCustom);
        run("config: invalid values rejected", CoreTests::configInvalid);
        run("config: missing keys fall back to defaults", CoreTests::configPartial);
        run("cost: parsing", CoreTests::costParsing);
        run("group: existing behaviour preserved", CoreTests::groupExisting);
        run("army: player soldier limit enforced and configurable", CoreTests::armyLimit);
        run("army: group limit enforced and configurable", CoreTests::groupLimit);
        run("army: one group per soldier, moving works", CoreTests::oneGroupOnly);
        run("army: death removes permanently from army and group", CoreTests::permanentDeath);
        run("army: merge replaces two with one, keeps group, frees slot", CoreTests::armyMerge);
        run("army: lowering limit keeps existing soldiers", CoreTests::loweredLimit);
        run("army: training updates soldier and group copy", CoreTests::armyTraining);
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---- tests -----------------------------------------------------------

    static void tierExisting() {
        eq(SoldierTier.VETERAN, SoldierTier.RECRUIT.nextTier().orElseThrow());
        eq(SoldierTier.LEGENDARY, SoldierTier.ELITE.nextTier().orElseThrow());
        isTrue(SoldierTier.LEGENDARY.nextTier().isEmpty());
        isTrue(SoldierTier.RECRUIT.canMergeWith(SoldierTier.RECRUIT));
        isTrue(!SoldierTier.RECRUIT.canMergeWith(SoldierTier.VETERAN));
        isTrue(!SoldierTier.LEGENDARY.canMergeWith(SoldierTier.LEGENDARY));
        eq(4, SoldierTier.values().length);
    }

    static void weaponRules() {
        isTrue(SoldierClass.SWORDSMAN.canWield(WeaponKind.SWORD));
        isTrue(!SoldierClass.SWORDSMAN.canWield(WeaponKind.BOW));
        isTrue(SoldierClass.ARCHER.canWield(WeaponKind.BOW));
        isTrue(!SoldierClass.ARCHER.canWield(WeaponKind.SWORD));
        isTrue(SoldierClass.ARCHER.isRanged());
        isTrue(!SoldierClass.SWORDSMAN.isRanged());
        eq(2, WeaponKind.values().length);
    }

    static void recordBasics() {
        SoldierRecord legacy = new SoldierRecord(id(1), SoldierTier.ELITE);
        eq(SoldierClass.SWORDSMAN, legacy.soldierClass());
        eq(0, legacy.trainingLevel());
        throwsIae(() -> new SoldierRecord(id(1), SoldierTier.RECRUIT, SoldierClass.ARCHER, -1));
        throwsNpe(() -> new SoldierRecord(null, SoldierTier.RECRUIT));
        throwsNpe(() -> new SoldierRecord(id(1), null));
        throwsNpe(() -> new SoldierRecord(id(1), SoldierTier.RECRUIT, null, 0));
        SoldierRecord r = SoldierRecord.recruit(id(2), SoldierClass.ARCHER);
        eq(SoldierTier.RECRUIT, r.tier());
        eq(SoldierClass.ARCHER, r.soldierClass());
    }

    static void mergeSuccess() {
        SoldierRecord a = new SoldierRecord(id(1), SoldierTier.RECRUIT, SoldierClass.ARCHER, 3);
        SoldierRecord b = new SoldierRecord(id(2), SoldierTier.RECRUIT, SoldierClass.ARCHER, 0);
        MergeResult r = SoldierMerge.merge(a, b, id(3));
        isTrue(r.success());
        SoldierRecord out = ((MergeResult.Merged) r).result();
        eq(id(3), out.id());
        eq(SoldierTier.VETERAN, out.tier());
        eq(SoldierClass.ARCHER, out.soldierClass());
        eq(0, out.trainingLevel());
    }

    static void mergeAllTiers() {
        SoldierTier[] t = SoldierTier.values();
        for (SoldierClass c : SoldierClass.values()) {
            for (int i = 0; i < t.length - 1; i++) {
                SoldierRecord a = new SoldierRecord(id(1), t[i], c, 0);
                SoldierRecord b = new SoldierRecord(id(2), t[i], c, 0);
                MergeResult.Merged m = (MergeResult.Merged) SoldierMerge.merge(a, b, id(3));
                eq(t[i + 1], m.result().tier());
            }
        }
    }

    static void mergeClassMismatch() {
        SoldierRecord a = SoldierRecord.recruit(id(1), SoldierClass.SWORDSMAN);
        SoldierRecord b = SoldierRecord.recruit(id(2), SoldierClass.ARCHER);
        eq(MergeFailure.CLASS_MISMATCH, rejected(SoldierMerge.merge(a, b, id(3))));
    }

    static void mergeTierMismatch() {
        SoldierRecord a = new SoldierRecord(id(1), SoldierTier.RECRUIT, SoldierClass.ARCHER, 0);
        SoldierRecord b = new SoldierRecord(id(2), SoldierTier.VETERAN, SoldierClass.ARCHER, 0);
        eq(MergeFailure.TIER_MISMATCH, rejected(SoldierMerge.merge(a, b, id(3))));
    }

    static void mergeLegendary() {
        SoldierRecord a = new SoldierRecord(id(1), SoldierTier.LEGENDARY, SoldierClass.ARCHER, 0);
        SoldierRecord b = new SoldierRecord(id(2), SoldierTier.LEGENDARY, SoldierClass.ARCHER, 0);
        eq(MergeFailure.MAX_TIER, rejected(SoldierMerge.merge(a, b, id(3))));
    }

    static void mergeSameSoldier() {
        SoldierRecord a = SoldierRecord.recruit(id(1), SoldierClass.ARCHER);
        eq(MergeFailure.SAME_SOLDIER, rejected(SoldierMerge.merge(a, a, id(3))));
    }

    static void mergeNoTrainingNeeded() {
        SoldierRecord a = new SoldierRecord(id(1), SoldierTier.ELITE, SoldierClass.SWORDSMAN, 0);
        SoldierRecord b = new SoldierRecord(id(2), SoldierTier.ELITE, SoldierClass.SWORDSMAN, 0);
        isTrue(SoldierMerge.merge(a, b, id(3)).success());
    }

    static void trainingRules() {
        ConquestConfig cfg = ConquestConfig.defaults();
        SoldierRecord s = SoldierRecord.recruit(id(1), SoldierClass.SWORDSMAN);
        SoldierStats base = Training.statsFor(s, cfg);
        eq(20.0, base.maxHealth());
        eq(3.0, base.attackDamage());
        for (int i = 0; i < cfg.maxTrainingLevel(); i++) {
            isTrue(Training.canTrain(s, cfg));
            s = Training.train(s, cfg);
        }
        eq(cfg.maxTrainingLevel(), s.trainingLevel());
        isTrue(!Training.canTrain(s, cfg));
        try {
            Training.train(s, cfg);
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
        SoldierStats trained = Training.statsFor(s, cfg);
        isTrue(trained.maxHealth() > base.maxHealth());
        isTrue(trained.attackDamage() > base.attackDamage());
        near(base.maxHealth() * (1 + 0.04 * 5), trained.maxHealth());
        near(base.speedMultiplier(), trained.speedMultiplier());
        // tier changes base stats
        SoldierStats leg = Training.statsFor(
                new SoldierRecord(id(2), SoldierTier.LEGENDARY, SoldierClass.ARCHER, 0), cfg);
        eq(60.0, leg.maxHealth());
        eq(6.0, leg.attackDamage());
        near(1.15, leg.speedMultiplier());
    }

    static void trainingConfigurable() {
        Properties p = new Properties();
        p.setProperty("training.maxLevel", "0");
        ConquestConfig none = ConquestConfig.parse(p);
        isTrue(!Training.canTrain(SoldierRecord.recruit(id(1), SoldierClass.ARCHER), none));
        p.setProperty("training.maxLevel", "10");
        p.setProperty("training.bonusPercentPerLevel", "10");
        ConquestConfig ten = ConquestConfig.parse(p);
        SoldierRecord s = SoldierRecord.recruit(id(1), SoldierClass.SWORDSMAN).withTrainingLevel(10);
        near(40.0, Training.statsFor(s, ten).maxHealth());
    }

    static void configRoundTrip() throws Exception {
        ConquestConfig d = ConquestConfig.defaults();
        eq(24, d.maxSoldiersPerPlayer());
        Path dir = Files.createTempDirectory("nmf-cfg");
        Path file = dir.resolve("sub").resolve("conf.properties");
        ConquestConfig created = ConquestConfig.load(file);
        isTrue(Files.exists(file));
        ConquestConfig reread = ConquestConfig.load(file);
        eq(d.toFileText(), created.toFileText());
        eq(d.toFileText(), reread.toFileText());
        for (SoldierClass c : SoldierClass.values()) {
            eq(d.recruitCost(c), reread.recruitCost(c));
        }
        eq(d.trainingCost(), reread.trainingCost());
        Files.delete(file);
        Files.delete(file.getParent());
        Files.delete(dir);
    }

    static void configCustom() {
        Properties p = new Properties();
        p.setProperty("limits.maxSoldiersPerPlayer", "50");
        p.setProperty("limits.maxSoldiersPerGroup", "7");
        p.setProperty("cost.recruit.archer", "minecraft:gold_ingot*2, minecraft:arrow*8");
        p.setProperty("cost.recruit.swordsman", "");
        p.setProperty("cost.train", "minecraft:diamond");
        p.setProperty("stats.elite.archer.damage", "9.5");
        ConquestConfig c = ConquestConfig.parse(p);
        eq(50, c.maxSoldiersPerPlayer());
        eq(7, c.maxSoldiersPerGroup());
        eq(List.of(new ItemCost("minecraft:gold_ingot", 2), new ItemCost("minecraft:arrow", 8)),
                c.recruitCost(SoldierClass.ARCHER));
        isTrue(c.recruitCost(SoldierClass.SWORDSMAN).isEmpty());
        eq(List.of(new ItemCost("minecraft:diamond", 1)), c.trainingCost());
        near(9.5, c.baseStats(SoldierTier.ELITE, SoldierClass.ARCHER).attackDamage());
    }

    static void configInvalid() {
        String[][] bad = {
            {"limits.maxSoldiersPerPlayer", "0"},
            {"limits.maxSoldiersPerPlayer", "abc"},
            {"limits.maxSoldiersPerGroup", "-3"},
            {"training.maxLevel", "-1"},
            {"training.bonusPercentPerLevel", "NaN"},
            {"training.bonusPercentPerLevel", "5000"},
            {"stats.recruit.archer.health", "0"},
            {"stats.recruit.archer.damage", "-1"},
            {"stats.legendary.speed", "0"},
            {"cost.train", "emerald*2"},
            {"cost.train", "minecraft:emerald*0"},
            {"cost.train", "minecraft:emerald*x"},
            {"cost.recruit.archer", "minecraft:arrow,,"},
        };
        for (String[] kv : bad) {
            Properties p = new Properties();
            p.setProperty(kv[0], kv[1]);
            try {
                ConquestConfig.parse(p);
                throw new AssertionError("expected ConfigException for " + kv[0] + "=" + kv[1]);
            } catch (ConfigException expected) {
                // ok
            }
        }
    }

    static void configPartial() {
        Properties p = new Properties();
        p.setProperty("limits.maxSoldiersPerPlayer", "10");
        ConquestConfig c = ConquestConfig.parse(p);
        eq(10, c.maxSoldiersPerPlayer());
        eq(ConquestConfig.defaults().maxSoldiersPerGroup(), c.maxSoldiersPerGroup());
        eq(ConquestConfig.defaults().trainingCost(), c.trainingCost());
    }

    static void costParsing() {
        eq(List.of(), ItemCost.parseList(""));
        eq(List.of(), ItemCost.parseList("   "));
        eq(List.of(new ItemCost("minecraft:bread", 1)), ItemCost.parseList("minecraft:bread"));
        eq(List.of(new ItemCost("a:b", 3), new ItemCost("c:d", 1)), ItemCost.parseList(" a:b * 3 , c:d "));
        eq("minecraft:emerald*4, minecraft:bread*2",
                ItemCost.format(ItemCost.parseList("minecraft:emerald*4,minecraft:bread*2")));
        throwsIae(() -> ItemCost.parseList("Minecraft:Emerald"));
        throwsIae(() -> new ItemCost("minecraft:emerald", 0));
    }

    static void groupExisting() {
        ArmyGroup g = new ArmyGroup(id(9), "  Alpha ");
        eq("Alpha", g.name());
        SoldierRecord s = new SoldierRecord(id(1), SoldierTier.RECRUIT);
        isTrue(g.add(s));
        isTrue(!g.add(s));
        eq(1, g.size());
        g.rename("Bravo");
        eq("Bravo", g.name());
        throwsIae(() -> g.rename("  "));
        throwsIae(() -> new ArmyGroup(id(8), ""));
        isTrue(g.remove(id(1)).isPresent());
        isTrue(g.remove(id(1)).isEmpty());
        eq(0, g.size());
        // new helpers
        g.add(s);
        isTrue(g.contains(id(1)));
        SoldierRecord upgraded = s.withTrainingLevel(2);
        isTrue(g.replace(upgraded));
        eq(2, g.soldiers().iterator().next().trainingLevel());
        isTrue(!g.replace(SoldierRecord.recruit(id(77), SoldierClass.ARCHER)));
        eq(1, g.size());
    }

    static void armyLimit() {
        Properties p = new Properties();
        p.setProperty("limits.maxSoldiersPerPlayer", "3");
        ConquestConfig cfg = ConquestConfig.parse(p);
        Army army = new Army(id(100));
        for (int i = 1; i <= 3; i++) {
            eq(ArmyResult.OK, army.addSoldier(SoldierRecord.recruit(id(i), SoldierClass.ARCHER), cfg));
        }
        eq(ArmyResult.ARMY_FULL, army.addSoldier(SoldierRecord.recruit(id(4), SoldierClass.ARCHER), cfg));
        eq(ArmyResult.DUPLICATE_SOLDIER,
                army.addSoldier(SoldierRecord.recruit(id(1), SoldierClass.ARCHER), cfg));
        eq(3, army.soldierCount());
        Properties bigger = new Properties();
        bigger.setProperty("limits.maxSoldiersPerPlayer", "4");
        eq(ArmyResult.OK, army.addSoldier(SoldierRecord.recruit(id(4), SoldierClass.ARCHER),
                ConquestConfig.parse(bigger)));
    }

    static void groupLimit() {
        Properties p = new Properties();
        p.setProperty("limits.maxSoldiersPerGroup", "2");
        ConquestConfig cfg = ConquestConfig.parse(p);
        Army army = new Army(id(100));
        army.createGroup(id(50), "G");
        for (int i = 1; i <= 3; i++) {
            army.addSoldier(SoldierRecord.recruit(id(i), SoldierClass.SWORDSMAN), cfg);
        }
        eq(ArmyResult.OK, army.assign(id(1), id(50), cfg));
        eq(ArmyResult.OK, army.assign(id(2), id(50), cfg));
        eq(ArmyResult.GROUP_FULL, army.assign(id(3), id(50), cfg));
        eq(ArmyResult.OK, army.assign(id(1), id(50), cfg)); // already inside: no-op
        eq(ArmyResult.NO_SUCH_SOLDIER, army.assign(id(99), id(50), cfg));
        eq(ArmyResult.NO_SUCH_GROUP, army.assign(id(3), id(51), cfg));
    }

    static void oneGroupOnly() {
        ConquestConfig cfg = ConquestConfig.defaults();
        Army army = new Army(id(100));
        army.createGroup(id(50), "A");
        army.createGroup(id(51), "B");
        army.addSoldier(SoldierRecord.recruit(id(1), SoldierClass.ARCHER), cfg);
        army.assign(id(1), id(50), cfg);
        army.assign(id(1), id(51), cfg);
        eq(0, army.group(id(50)).orElseThrow().size());
        eq(1, army.group(id(51)).orElseThrow().size());
        eq(id(51), army.groupOf(id(1)).orElseThrow().id());
        army.unassign(id(1));
        isTrue(army.groupOf(id(1)).isEmpty());
        eq(1, army.soldierCount());
        army.assign(id(1), id(50), cfg);
        eq(ArmyResult.OK, army.removeGroup(id(50)));
        eq(1, army.soldierCount()); // soldier survives group removal
        eq(ArmyResult.NO_SUCH_GROUP, army.removeGroup(id(50)));
        eq(ArmyResult.OK, army.renameGroup(id(51), "Renamed"));
        eq("Renamed", army.group(id(51)).orElseThrow().name());
        eq(ArmyResult.NO_SUCH_GROUP, army.renameGroup(id(60), "X"));
        throwsIae(() -> army.createGroup(id(51), "Dup"));
    }

    static void permanentDeath() {
        ConquestConfig cfg = ConquestConfig.defaults();
        Army army = new Army(id(100));
        army.createGroup(id(50), "A");
        army.addSoldier(SoldierRecord.recruit(id(1), SoldierClass.ARCHER), cfg);
        army.assign(id(1), id(50), cfg);
        isTrue(army.removeSoldier(id(1)).isPresent());
        eq(0, army.soldierCount());
        eq(0, army.group(id(50)).orElseThrow().size());
        isTrue(army.soldier(id(1)).isEmpty());
        isTrue(army.removeSoldier(id(1)).isEmpty());
    }

    static void armyMerge() {
        Properties p = new Properties();
        p.setProperty("limits.maxSoldiersPerPlayer", "2");
        p.setProperty("limits.maxSoldiersPerGroup", "2");
        ConquestConfig cfg = ConquestConfig.parse(p);
        Army army = new Army(id(100));
        army.createGroup(id(50), "A");
        army.addSoldier(SoldierRecord.recruit(id(1), SoldierClass.ARCHER), cfg);
        army.addSoldier(SoldierRecord.recruit(id(2), SoldierClass.ARCHER), cfg);
        army.assign(id(2), id(50), cfg);
        MergeResult r = army.merge(id(1), id(2), id(3));
        isTrue(r.success());
        eq(1, army.soldierCount());
        isTrue(army.soldier(id(1)).isEmpty());
        isTrue(army.soldier(id(2)).isEmpty());
        eq(SoldierTier.VETERAN, army.soldier(id(3)).orElseThrow().tier());
        eq(id(50), army.groupOf(id(3)).orElseThrow().id());
        eq(ArmyResult.OK, army.addSoldier(SoldierRecord.recruit(id(4), SoldierClass.ARCHER), cfg));
        // rejected merges change nothing
        army.addSoldier(SoldierRecord.recruit(id(5), SoldierClass.SWORDSMAN), ConquestConfig.defaults());
        int before = army.soldierCount();
        eq(MergeFailure.CLASS_MISMATCH, rejected(army.merge(id(4), id(5), id(6))));
        eq(MergeFailure.TIER_MISMATCH, rejected(army.merge(id(3), id(4), id(6))));
        eq(MergeFailure.NOT_IN_ARMY, rejected(army.merge(id(4), id(77), id(6))));
        eq(MergeFailure.SAME_SOLDIER, rejected(army.merge(id(4), id(4), id(6))));
        eq(before, army.soldierCount());
    }

    static void loweredLimit() {
        ConquestConfig big = ConquestConfig.defaults();
        Properties p = new Properties();
        p.setProperty("limits.maxSoldiersPerPlayer", "1");
        ConquestConfig small = ConquestConfig.parse(p);
        Army army = new Army(id(100));
        for (int i = 1; i <= 5; i++) {
            army.addSoldier(SoldierRecord.recruit(id(i), SoldierClass.ARCHER), big);
        }
        eq(ArmyResult.ARMY_FULL, army.addSoldier(SoldierRecord.recruit(id(9), SoldierClass.ARCHER), small));
        eq(5, army.soldierCount());
    }

    static void armyTraining() {
        ConquestConfig cfg = ConquestConfig.defaults();
        Army army = new Army(id(100));
        army.createGroup(id(50), "A");
        army.addSoldier(SoldierRecord.recruit(id(1), SoldierClass.SWORDSMAN), cfg);
        army.assign(id(1), id(50), cfg);
        eq(ArmyResult.OK, army.train(id(1), cfg));
        eq(1, army.soldier(id(1)).orElseThrow().trainingLevel());
        eq(1, army.group(id(50)).orElseThrow().soldiers().iterator().next().trainingLevel());
        for (int i = 1; i < cfg.maxTrainingLevel(); i++) {
            eq(ArmyResult.OK, army.train(id(1), cfg));
        }
        eq(ArmyResult.MAX_TRAINING, army.train(id(1), cfg));
        eq(ArmyResult.NO_SUCH_SOLDIER, army.train(id(99), cfg));
    }

    // ---- tiny harness ----------------------------------------------------

    interface Body {
        void run() throws Exception;
    }

    private static void run(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + "  ->  " + t);
        }
    }

    private static UUID id(int n) {
        return new UUID(0L, n);
    }

    private static MergeFailure rejected(MergeResult r) {
        if (r instanceof MergeResult.Rejected rej) {
            return rej.reason();
        }
        throw new AssertionError("expected merge to be rejected");
    }

    private static void isTrue(boolean cond) {
        if (!cond) {
            throw new AssertionError("expected true");
        }
    }

    private static void eq(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void near(double expected, double actual) {
        if (Math.abs(expected - actual) > 1e-9) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void throwsIae(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }

    private static void throwsNpe(Runnable r) {
        try {
            r.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("expected NullPointerException");
    }

    private CoreTests() {}
}
