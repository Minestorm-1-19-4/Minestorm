package net.minestom.server.utils.block;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.BlockPosition;
import net.minestom.server.utils.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlockUtils {

    private final Instance instance;
    private final BlockPosition position;

    public BlockUtils(Instance instance, BlockPosition position) {
        this.instance = instance;
        this.position = position;
    }

    public BlockUtils getRelativeTo(int x, int y, int z) {
        BlockPosition position = this.position.clone().add(x, y, z);
        return new BlockUtils(instance, position);
    }

    public BlockUtils above() {
        return getRelativeTo(0, 1, 0);
    }

    public BlockUtils below() {
        return getRelativeTo(0, -1, 0);
    }

    public BlockUtils north() {
        return getRelativeTo(0, 0, -1);
    }

    public BlockUtils east() {
        return getRelativeTo(1, 0, 0);
    }

    public BlockUtils south() {
        return getRelativeTo(0, 0, 1);
    }

    public BlockUtils west() {
        return getRelativeTo(-1, 0, 0);
    }

    public Block getBlock() {
        return instance.getBlock(position);
    }

    public boolean equals(Block block) {
        return getBlock().compare(block);
    }

    public static Map<String, String> parseProperties(String query) {
        if (!query.startsWith("[") || !query.endsWith("]") ||
                query.equals("[]")) {
            return Collections.emptyMap();
        }
        final int capacity = StringUtils.countMatches(query, ',') + 1;
        Map<String, String> result = new HashMap<>(capacity);
        final String propertiesString = query.substring(1);
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        StringBuilder builder = keyBuilder;
        for (int i = 0; i < propertiesString.length(); i++) {
            final char c = propertiesString.charAt(i);
            if (c == '=') {
                // Switch to value builder
                builder = valueBuilder;
            } else if (c == ',' || c == ']') {
                // Append current text
                result.put(keyBuilder.toString().intern(), valueBuilder.toString().intern());
                keyBuilder = new StringBuilder();
                valueBuilder = new StringBuilder();
                builder = keyBuilder;
            } else if (c != ' ') {
                builder.append(c);
            }
        }
        return result;
    }
}
