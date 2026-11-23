package notmistfalls.conquest.army;

/**
 * Soldier classes. Swordsmen use swords only; archers use bows only.
 */
public enum SoldierClass {
    SWORDSMAN(WeaponKind.SWORD),
    ARCHER(WeaponKind.BOW);

    private final WeaponKind weapon;

    SoldierClass(WeaponKind weapon) {
        this.weapon = weapon;
    }

    /** The single weapon kind this class is allowed to wield. */
    public WeaponKind weapon() {
        return weapon;
    }

    public boolean canWield(WeaponKind kind) {
        return weapon == kind;
    }

    public boolean isRanged() {
        return this == ARCHER;
    }
}
