package net.moneysmp.build;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;

import java.util.*;

/**
 * Collects blocks in local coordinates: x = right, y = up, z = forward (the way the player faces).
 * y = 0 is the block the player stands in, so floors go at y = -1.
 */
public final class Builder {

    public record Pos(int x, int y, int z) {}

    public record Label(double x, double y, double z, String text, float scale) {}

    private final World world;
    private final int ox, oy, oz;
    private final int fx, fz, rx, rz;
    private final BlockFace forward;
    private final Map<Pos, BlockData> blocks = new LinkedHashMap<>();
    private final List<Label> labels = new ArrayList<>();
    private final Map<Material, BlockData> cache = new EnumMap<>(Material.class);

    public Builder(Location origin, BlockFace facing) {
        this.world = origin.getWorld();
        this.ox = origin.getBlockX();
        this.oy = origin.getBlockY();
        this.oz = origin.getBlockZ();
        this.forward = switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> facing;
            default -> BlockFace.SOUTH;
        };
        this.fx = forward.getModX();
        this.fz = forward.getModZ();
        this.rx = -fz;
        this.rz = fx;
    }

    public World world() { return world; }
    public Map<Pos, BlockData> blocks() { return blocks; }
    public List<Label> labels() { return labels; }

    private Pos toWorld(int x, int y, int z) {
        return new Pos(ox + x * rx + z * fx, oy + y, oz + x * rz + z * fz);
    }

    /** World location of a local point (block centre). */
    public Location loc(double x, double y, double z) {
        return new Location(world, ox + 0.5 + x * rx + z * fx, oy + y, oz + 0.5 + x * rz + z * fz);
    }

    private BlockData data(Material m) {
        return cache.computeIfAbsent(m, Material::createBlockData);
    }

    public void set(int x, int y, int z, Material m) { blocks.put(toWorld(x, y, z), data(m)); }
    public void set(int x, int y, int z, BlockData d) { blocks.put(toWorld(x, y, z), d); }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, m);
    }

    /** Hollow walls of a box (no floor or roof). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        fill(x1, y1, z1, x2, y2, z1, m);
        fill(x1, y1, z2, x2, y2, z2, m);
        fill(x1, y1, z1, x1, y2, z2, m);
        fill(x2, y1, z1, x2, y2, z2, m);
    }

    public static double dist(int x, int z, int cx, int cz) {
        return Math.sqrt((double) (x - cx) * (x - cx) + (double) (z - cz) * (z - cz));
    }

    /** Filled circle. */
    public void disc(int cx, int cz, int y, int r, Material m) {
        for (int x = cx - r; x <= cx + r; x++)
            for (int z = cz - r; z <= cz + r; z++)
                if (dist(x, z, cx, cz) <= r + 0.5) set(x, y, z, m);
    }

    /** Filled cylinder between y1 and y2. */
    public void cylinder(int cx, int cz, int y1, int y2, int r, Material m) {
        for (int y = y1; y <= y2; y++) disc(cx, cz, y, r, m);
    }

    /** One-block-thick ring of radius r. */
    public void ring(int cx, int cz, int y, int r, Material m) {
        for (int x = cx - r - 1; x <= cx + r + 1; x++)
            for (int z = cz - r - 1; z <= cz + r + 1; z++) {
                double d = dist(x, z, cx, cz);
                if (d > r - 0.5 && d <= r + 0.5) set(x, y, z, m);
            }
    }

    public enum Dir { FORWARD, BACK, LEFT, RIGHT }

    public BlockFace face(Dir d) {
        BlockFace right = switch (forward) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
        return switch (d) {
            case FORWARD -> forward;
            case BACK -> forward.getOppositeFace();
            case RIGHT -> right;
            case LEFT -> right.getOppositeFace();
        };
    }

    /** Stairs whose tall side points in direction d (you sit facing the other way). */
    public void stairs(int x, int y, int z, Material m, Dir d) {
        BlockData bd = m.createBlockData();
        if (bd instanceof Stairs s) s.setFacing(face(d));
        set(x, y, z, bd);
    }

    /** Floating text (MiniMessage) shown with a text display. */
    public void label(double x, double y, double z, String text, float scale) {
        labels.add(new Label(x, y, z, text, scale));
    }
}
