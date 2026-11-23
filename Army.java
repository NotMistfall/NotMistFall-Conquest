package notmistfalls.conquest.army;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import notmistfalls.conquest.config.ConquestConfig;

/**
 * Everything one player owns: a pool of soldiers and named groups.
 *
 * <p>Rules: a soldier belongs to at most one group; limits come from the config
 * and are checked when adding or assigning. Lowering a limit never removes
 * existing soldiers; it only blocks further additions. Soldiers who die are
 * removed permanently with {@link #removeSoldier(UUID)}.
 */
public final class Army {
    private final UUID owner;
    private final Map<UUID, SoldierRecord> soldiers = new LinkedHashMap<>();
    private final Map<UUID, ArmyGroup> groups = new LinkedHashMap<>();

    public Army(UUID owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    public UUID owner() {
        return owner;
    }

    // ---- soldiers --------------------------------------------------------

    public Collection<SoldierRecord> soldiers() {
        return Collections.unmodifiableCollection(soldiers.values());
    }

    public Optional<SoldierRecord> soldier(UUID id) {
        return Optional.ofNullable(soldiers.get(id));
    }

    public int soldierCount() {
        return soldiers.size();
    }

    public ArmyResult addSoldier(SoldierRecord soldier, ConquestConfig config) {
        Objects.requireNonNull(soldier, "soldier");
        if (soldiers.containsKey(soldier.id())) {
            return ArmyResult.DUPLICATE_SOLDIER;
        }
        if (soldiers.size() >= config.maxSoldiersPerPlayer()) {
            return ArmyResult.ARMY_FULL;
        }
        soldiers.put(soldier.id(), soldier);
        return ArmyResult.OK;
    }

    /** Permanently removes a soldier (death or dismissal) from the army and any group. */
    public Optional<SoldierRecord> removeSoldier(UUID id) {
        SoldierRecord removed = soldiers.remove(id);
        if (removed != null) {
            for (ArmyGroup g : groups.values()) {
                g.remove(id);
            }
        }
        return Optional.ofNullable(removed);
    }

    /** Raises a soldier's training level by one (costs are handled by the caller). */
    public ArmyResult train(UUID id, ConquestConfig config) {
        SoldierRecord s = soldiers.get(id);
        if (s == null) {
            return ArmyResult.NO_SUCH_SOLDIER;
        }
        if (!Training.canTrain(s, config)) {
            return ArmyResult.MAX_TRAINING;
        }
        replace(Training.train(s, config));
        return ArmyResult.OK;
    }

    /**
     * Merges two soldiers of this army. On success both are replaced by one
     * soldier one tier higher, placed in the first soldier's group (or the
     * second's if the first has none). The soldier count drops by one.
     */
    public MergeResult merge(UUID idA, UUID idB, UUID newId) {
        SoldierRecord a = soldiers.get(idA);
        SoldierRecord b = soldiers.get(idB);
        if (a == null || b == null) {
            return new MergeResult.Rejected(MergeFailure.NOT_IN_ARMY);
        }
        MergeResult result = SoldierMerge.merge(a, b, newId);
        if (result instanceof MergeResult.Merged merged) {
            if (soldiers.containsKey(newId)) {
                throw new IllegalArgumentException("newId is already used by a soldier.");
            }
            Optional<ArmyGroup> target = groupOf(idA).or(() -> groupOf(idB));
            removeSoldier(idA);
            removeSoldier(idB);
            soldiers.put(newId, merged.result());
            target.ifPresent(g -> g.add(merged.result()));
        }
        return result;
    }

    private void replace(SoldierRecord updated) {
        soldiers.replace(updated.id(), updated);
        for (ArmyGroup g : groups.values()) {
            g.replace(updated);
        }
    }

    // ---- groups ----------------------------------------------------------

    public Collection<ArmyGroup> groups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    public Optional<ArmyGroup> group(UUID id) {
        return Optional.ofNullable(groups.get(id));
    }

    public Optional<ArmyGroup> groupOf(UUID soldierId) {
        for (ArmyGroup g : groups.values()) {
            if (g.contains(soldierId)) {
                return Optional.of(g);
            }
        }
        return Optional.empty();
    }

    /** Creates a group. The name must not be blank (see {@link ArmyGroup#rename}). */
    public ArmyGroup createGroup(UUID id, String name) {
        if (groups.containsKey(id)) {
            throw new IllegalArgumentException("Group id already exists.");
        }
        ArmyGroup g = new ArmyGroup(id, name);
        groups.put(id, g);
        return g;
    }

    public ArmyResult renameGroup(UUID id, String name) {
        ArmyGroup g = groups.get(id);
        if (g == null) {
            return ArmyResult.NO_SUCH_GROUP;
        }
        g.rename(name);
        return ArmyResult.OK;
    }

    /** Removes a group. Its soldiers stay in the army, just ungrouped. */
    public ArmyResult removeGroup(UUID id) {
        return groups.remove(id) == null ? ArmyResult.NO_SUCH_GROUP : ArmyResult.OK;
    }

    /** Moves a soldier into a group (leaving any previous group). */
    public ArmyResult assign(UUID soldierId, UUID groupId, ConquestConfig config) {
        SoldierRecord s = soldiers.get(soldierId);
        if (s == null) {
            return ArmyResult.NO_SUCH_SOLDIER;
        }
        ArmyGroup target = groups.get(groupId);
        if (target == null) {
            return ArmyResult.NO_SUCH_GROUP;
        }
        if (target.contains(soldierId)) {
            return ArmyResult.OK;
        }
        if (target.size() >= config.maxSoldiersPerGroup()) {
            return ArmyResult.GROUP_FULL;
        }
        groupOf(soldierId).ifPresent(g -> g.remove(soldierId));
        target.add(s);
        return ArmyResult.OK;
    }

    public ArmyResult unassign(UUID soldierId) {
        if (!soldiers.containsKey(soldierId)) {
            return ArmyResult.NO_SUCH_SOLDIER;
        }
        groupOf(soldierId).ifPresent(g -> g.remove(soldierId));
        return ArmyResult.OK;
    }
}
