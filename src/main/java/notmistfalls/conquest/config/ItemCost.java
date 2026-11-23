package notmistfalls.conquest.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** A cost entry: an item id (such as {@code minecraft:emerald}) and a count. */
public record ItemCost(String itemId, int count) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public ItemCost {
        if (itemId == null || !ID.matcher(itemId).matches()) {
            throw new IllegalArgumentException("Invalid item id: " + itemId);
        }
        if (count < 1) {
            throw new IllegalArgumentException("Cost count must be at least 1 for " + itemId);
        }
    }

    /** Parses {@code "minecraft:emerald*4, minecraft:bread"}; blank means free. */
    public static List<ItemCost> parseList(String text) {
        List<ItemCost> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        for (String part : text.split(",", -1)) {
            String p = part.trim();
            if (p.isEmpty()) {
                throw new IllegalArgumentException("Empty entry in cost list: '" + text + "'");
            }
            int star = p.indexOf('*');
            if (star < 0) {
                out.add(new ItemCost(p, 1));
            } else {
                String id = p.substring(0, star).trim();
                int count;
                try {
                    count = Integer.parseInt(p.substring(star + 1).trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid count in '" + p + "'");
                }
                out.add(new ItemCost(id, count));
            }
        }
        return List.copyOf(out);
    }

    public static String format(List<ItemCost> costs) {
        StringBuilder sb = new StringBuilder();
        for (ItemCost c : costs) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(c.itemId()).append('*').append(c.count());
        }
        return sb.toString();
    }
}
