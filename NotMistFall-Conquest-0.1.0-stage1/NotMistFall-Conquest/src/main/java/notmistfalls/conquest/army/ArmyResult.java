package notmistfalls.conquest.army;

/** Result codes for {@link Army} operations. */
public enum ArmyResult {
    OK,
    DUPLICATE_SOLDIER,
    DUPLICATE_GROUP,
    ARMY_FULL,
    GROUP_FULL,
    NO_SUCH_SOLDIER,
    NO_SUCH_GROUP,
    MAX_TRAINING
}
