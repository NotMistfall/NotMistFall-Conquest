# NotMistFall: Conquest

**Project target:** Minecraft 26.2 · Fabric · Java 25
**Mod ID:** `notmistfalls_conquest`
**Version:** `0.1.0`

## Status: Stages 0 and 1 of the roadmap

| Stage | Contents | Status |
|---|---|---|
| 0 | Gradle wrapper, GitHub Actions build, build config | Written. **Needs a green CI run to confirm.** |
| 1 | Pure core model: classes, tiers, merge, training, config, army | Written. 25 core tests pass locally (JDK 21). **Needs a green CI run on JDK 25.** |
| 2+ | Persistence, entities, AI, items, blocks, GUIs, commands | Not started |

This is still **not a playable mod**: it loads and reads its config, but adds no
entities, items, blocks, GUIs or commands yet.

## Design rules implemented in Stage 1

- Soldier classes: **Swordsman** (sword only) and **Archer** (bow only).
- Tiers: Recruit, Veteran, Elite, Legendary.
- **Merge:** two different soldiers of the **same class and tier** become one soldier of
  the same class, one tier higher. Legendary cannot merge. Training is **not** required.
  The merged soldier starts at training level 0.
- **Training** raises a training level (capped by config) that improves health and damage.
  Abilities unlocked by training are not defined yet.
- **Death is permanent:** `Army.removeSoldier` removes a soldier from the army and its group.
- A soldier belongs to at most one group. Soldier limits per player and per group are configurable.

## Configuration

On first launch the mod writes `config/notmistfalls_conquest.properties` with every value
(limits, recruitment cost per class, training cost, training rules, per-tier stats).
Default numbers are placeholders for balancing. If the file is invalid, the mod logs the
problem and uses built-in defaults without overwriting your file.

## Building

**On GitHub (no local setup):** push the project to a repository. The workflow in
`.github/workflows/build.yml` builds on JDK 25, runs the tests, and uploads the mod jar as
an artifact named `notmistfalls-conquest-jar` (Actions tab -> the run -> Artifacts).

**Locally:** needs a JDK 25 and internet access.

```bat
gradlew build
```

The jar is created in `build/libs/` (use the one without `-sources`).
To run only the core tests: `gradlew coreTests`.

## Version pins

- Minecraft `26.2`, Fabric Loader `0.19.5`, Fabric Loom `1.18.2`
- Fabric API `0.161.0+26.2`, Gradle `9.7.1`, Java `25`

These pins could not be verified against Fabric's servers when this stage was written.
If dependency resolution fails, check https://fabricmc.net/develop/ and update
`gradle.properties` (and the wrapper with `gradlew wrapper --gradle-version <version>`).

Note: for Minecraft 26.x, Loom's `net.fabricmc.fabric-loom` plugin does not remap, so
`build.gradle` uses `implementation` (not `modImplementation`) and has no `mappings` line.

## Package layout

```text
notmistfalls/conquest/
├── NotMistFallsConquest.java      entrypoint, loads config
├── army/
│   ├── Army.java                  one player's soldiers and groups, limits, merge, training
│   ├── ArmyGroup.java             named group (existing, extended)
│   ├── ArmyResult.java
│   ├── MergeFailure.java / MergeResult.java / SoldierMerge.java
│   ├── SoldierClass.java / WeaponKind.java
│   ├── SoldierRecord.java         (existing, extended)
│   ├── SoldierStats.java / Training.java
│   └── SoldierTier.java           (existing, unchanged)
└── config/
    ├── ConquestConfig.java / ConfigException.java / ItemCost.java
src/test/java/notmistfalls/conquest/CoreTests.java
```
