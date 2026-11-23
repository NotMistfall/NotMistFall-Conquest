package notmistfalls.conquest.army;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ArmyGroup {
    private final UUID id;
    private String name;
    private final Map<UUID, SoldierRecord> soldiers = new LinkedHashMap<>();

    public ArmyGroup(UUID id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be blank.");
        }
        this.name = name.trim();
    }

    public boolean add(SoldierRecord soldier) {
        Objects.requireNonNull(soldier, "soldier");
        return soldiers.putIfAbsent(soldier.id(), soldier) == null;
    }

    public Optional<SoldierRecord> remove(UUID soldierId) {
        return Optional.ofNullable(soldiers.remove(soldierId));
    }

    public boolean contains(UUID soldierId) {
        return soldiers.containsKey(soldierId);
    }

    /** Replaces an existing soldier's record in place, keeping its position. */
    public boolean replace(SoldierRecord soldier) {
        Objects.requireNonNull(soldier, "soldier");
        return soldiers.replace(soldier.id(), soldier) != null;
    }

    public Collection<SoldierRecord> soldiers() {
        return Collections.unmodifiableCollection(soldiers.values());
    }

    public int size() {
        return soldiers.size();
    }
}
