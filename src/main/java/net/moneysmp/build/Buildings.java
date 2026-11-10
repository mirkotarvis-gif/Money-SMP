package net.moneysmp.build;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

import static org.bukkit.Material.*;

/**
 * The spawnable Money SMP builds, in the black / light-blue neon style of the server.
 * Every build is placed in front of the player and rotated to the way they are facing.
 */
public final class Buildings {

    public static final List<String> NAMES = List.of("lobby", "biddinghall", "gladiators", "capturepoint", "podium");

    private Buildings() {}

    /** Fills the builder for the named build and returns its centre, or null for an unknown name. */
    public static Location build(String name, Builder b, int zoneSize, int hologramHeight, String pointName) {
        return switch (name) {
            case "lobby" -> lobby(b);
            case "biddinghall" -> biddingHall(b);
            case "gladiators" -> gladiators(b);
            case "capturepoint" -> capturePoint(b, zoneSize, hologramHeight, pointName);
            case "podium" -> podium(b);
            default -> null;
        };
    }

    private static int[] polar(double degrees, double radius) {
        double a = Math.toRadians(degrees);
        return new int[]{(int) Math.round(Math.cos(a) * radius), (int) Math.round(Math.sin(a) * radius)};
    }

    // ------------------------------------------------------------------ spawn lobby (centred on the player)

    private static Location lobby(Builder b) {
        int r = 24;
        b.cylinder(0, 0, 0, 22, r + 1, AIR);
        b.cylinder(0, 0, -3, -2, r + 1, DEEPSLATE_TILES);
        b.disc(0, 0, -1, r + 1, DARK_PRISMARINE);
        b.disc(0, 0, -1, r, BLACK_CONCRETE);
        b.ring(0, 0, -1, 7, LIGHT_BLUE_CONCRETE);
        b.ring(0, 0, -1, 15, LIGHT_BLUE_CONCRETE);
        b.ring(0, 0, -1, 22, CYAN_CONCRETE);

        // 8 spokes with lights
        for (int k = 0; k < 8; k++) {
            for (int d = 8; d <= 21; d++) {
                int[] p = polar(k * 45, d);
                b.set(p[0], -1, p[1], d == 11 || d == 19 ? SEA_LANTERN : GRAY_CONCRETE);
            }
        }
        // edge lights + glass railing with 4 exits
        for (int deg = 0; deg < 360; deg += 15) {
            int[] p = polar(deg, r);
            b.set(p[0], -1, p[1], SEA_LANTERN);
        }
        for (int x = -r - 2; x <= r + 2; x++)
            for (int z = -r - 2; z <= r + 2; z++) {
                double d = Builder.dist(x, z, 0, 0);
                if (d > r + 0.5 && d <= r + 1.5 && Math.abs(x) > 2 && Math.abs(z) > 2) {
                    b.set(x, 0, z, LIGHT_BLUE_STAINED_GLASS);
                }
            }

        // money fountain
        b.disc(0, 0, -1, 3, SEA_LANTERN);
        b.ring(0, 0, 0, 4, PRISMARINE_BRICKS);
        b.disc(0, 0, 0, 3, WATER);
        b.fill(0, 0, 0, 0, 5, 0, GOLD_BLOCK);
        b.set(0, 6, 0, SEA_LANTERN);
        for (int[] off : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) b.set(off[0], 3, off[1], RAW_GOLD_BLOCK);

        // 8 light pillars
        for (int k = 0; k < 8; k++) {
            int[] p = polar(22.5 + k * 45, 19);
            b.fill(p[0] - 1, 0, p[1] - 1, p[0] + 1, 9, p[1] + 1, POLISHED_BLACKSTONE_BRICKS);
            b.fill(p[0] - 1, 3, p[1] - 1, p[0] + 1, 3, p[1] + 1, LIGHT_BLUE_STAINED_GLASS);
            b.fill(p[0] - 1, 6, p[1] - 1, p[0] + 1, 6, p[1] + 1, LIGHT_BLUE_STAINED_GLASS);
            b.fill(p[0] - 1, 10, p[1] - 1, p[0] + 1, 10, p[1] + 1, SEA_LANTERN);
            b.set(p[0], 11, p[1], END_ROD);
        }

        b.label(0, 10, 0, "<gradient:#8bebf0:#4588e3><bold>MONEY SMP</bold></gradient>", 5f);
        b.label(0, 8.3, 0, "<#fbe69b>Welcome to the server!", 1.6f);
        return b.loc(0, 0, 0);
    }

    // ------------------------------------------------------------------ bidding hall (in front of the player)

    private static Location biddingHall(Builder b) {
        int x = 15, z1 = 2, z2 = 34, h = 13;
        b.fill(-x, 0, z1, x, h, z2, AIR);
        b.fill(-x, -2, z1, x, -2, z2, DEEPSLATE_TILES);
        b.fill(-x, -1, z1, x, -1, z2, BLACK_CONCRETE);
        b.fill(-1, -1, z1, 1, -1, 22, LIGHT_BLUE_CONCRETE);

        b.walls(-x, 0, z1, x, h - 1, z2, DARK_PRISMARINE);
        b.walls(-x, 0, z1, x, 0, z2, POLISHED_BLACKSTONE_BRICKS);
        b.walls(-x, h - 1, z1, x, h - 1, z2, POLISHED_BLACKSTONE_BRICKS);
        for (int z = z1 + 3; z <= z2 - 3; z += 3) {
            b.fill(-x, 4, z, -x, 8, z, LIGHT_BLUE_STAINED_GLASS);
            b.fill(x, 4, z, x, 8, z, LIGHT_BLUE_STAINED_GLASS);
        }
        b.fill(-x, h, z1, x, h, z2, BLACK_CONCRETE);
        for (int lx = -12; lx <= 12; lx += 4)
            for (int lz = z1 + 3; lz <= z2 - 3; lz += 4) b.set(lx, h, lz, SEA_LANTERN);

        // entrance
        b.fill(-2, 0, z1, 2, 4, z1, AIR);
        b.fill(-3, 5, z1, 3, 5, z1, SEA_LANTERN);
        b.fill(-3, 0, z1, -3, 4, z1, SEA_LANTERN);
        b.fill(3, 0, z1, 3, 4, z1, SEA_LANTERN);
        b.label(0, 7.5, z1 - 0.7, "<gradient:#8bebf0:#4588e3><bold>BIDDING HALL</bold></gradient>", 2.5f);

        // stage + auction podium
        b.fill(-11, 0, 24, 11, 1, z2 - 1, POLISHED_BLACKSTONE);
        b.fill(-11, 1, 24, 11, 1, 24, LIGHT_BLUE_CONCRETE);
        for (int sx = -3; sx <= 3; sx++) b.stairs(sx, 0, 23, POLISHED_BLACKSTONE_STAIRS, Builder.Dir.FORWARD);
        b.fill(-1, 1, 27, 1, 1, 29, GOLD_BLOCK);
        b.set(0, 1, 28, SEA_LANTERN);
        b.fill(-1, h, 27, 1, h, 29, SEA_LANTERN);

        // screen on the back wall
        b.fill(-10, 3, z2 - 1, 10, 11, z2 - 1, BLACK_STAINED_GLASS);
        b.fill(-11, 2, z2 - 1, 11, 2, z2 - 1, SEA_LANTERN);
        b.fill(-11, 12, z2 - 1, 11, 12, z2 - 1, SEA_LANTERN);
        b.fill(-11, 2, z2 - 1, -11, 12, z2 - 1, SEA_LANTERN);
        b.fill(11, 2, z2 - 1, 11, 12, z2 - 1, SEA_LANTERN);
        b.label(0, 8.5, z2 - 1.6, "<gradient:#8bebf0:#4588e3><bold>PLAYER AUCTION</bold></gradient>", 3f);
        b.label(0, 5.5, z2 - 1.6, "<#fbe69b>Captains: /bid \\<amount>", 1.6f);

        // 6 captain booths facing the stage
        Material[] floors = {RED_CONCRETE, BLUE_CONCRETE, LIME_CONCRETE, YELLOW_CONCRETE, PURPLE_CONCRETE, ORANGE_CONCRETE};
        Material[] desks = {RED_TERRACOTTA, BLUE_TERRACOTTA, LIME_TERRACOTTA, YELLOW_TERRACOTTA, PURPLE_TERRACOTTA, ORANGE_TERRACOTTA};
        String[] colors = {"#E24A4A", "#4588E3", "#77E24A", "#E2D24A", "#9A4DBD", "#E29A4A"};
        int[] xs = {-9, 0, 9};
        int[] zs = {8, 15};
        int i = 0;
        for (int bz : zs)
            for (int bx : xs) {
                b.fill(bx - 2, 0, bz - 1, bx + 2, 0, bz + 1, floors[i]);
                b.fill(bx - 2, 0, bz + 2, bx + 2, 1, bz + 2, desks[i]);
                b.set(bx, 1, bz + 2, SEA_LANTERN);
                b.label(bx, 4, bz, "<" + colors[i] + "><bold>TEAM " + (i + 1) + "</bold>", 1.3f);
                i++;
            }

        // spectator stands along both side walls
        for (int z = 4; z <= 20; z++) {
            b.stairs(12, 0, z, DARK_PRISMARINE_STAIRS, Builder.Dir.RIGHT);
            b.set(13, 0, z, DARK_PRISMARINE);
            b.stairs(13, 1, z, DARK_PRISMARINE_STAIRS, Builder.Dir.RIGHT);
            b.stairs(-12, 0, z, DARK_PRISMARINE_STAIRS, Builder.Dir.LEFT);
            b.set(-13, 0, z, DARK_PRISMARINE);
            b.stairs(-13, 1, z, DARK_PRISMARINE_STAIRS, Builder.Dir.LEFT);
        }
        return b.loc(0, 2, 28);
    }

    // ------------------------------------------------------------------ gladiators arena

    private static Location gladiators(Builder b) {
        int cz = 24;
        b.cylinder(0, cz, 0, 16, 23, AIR);
        b.cylinder(0, cz, -3, -2, 23, DEEPSLATE_TILES);
        b.disc(0, cz, -1, 23, POLISHED_BLACKSTONE);
        b.disc(0, cz, -1, 14, SAND);
        b.ring(0, cz, -1, 4, LIGHT_BLUE_CONCRETE);
        b.disc(0, cz, -1, 1, SEA_LANTERN);
        b.disc(0, cz - 10, -1, 1, GOLD_BLOCK);
        b.disc(0, cz + 10, -1, 1, GOLD_BLOCK);

        for (int y = 0; y <= 3; y++) b.ring(0, cz, y, 15, POLISHED_BLACKSTONE_BRICKS);
        b.ring(0, cz, 4, 15, DARK_PRISMARINE);
        for (int deg = 0; deg < 360; deg += 30) {
            int[] p = polar(deg, 15);
            b.set(p[0], 2, cz + p[1], SEA_LANTERN);
        }

        // stepped stands
        for (int k = 0; k <= 5; k++) {
            int r = 16 + k;
            for (int y = 0; y <= 2 + k; y++) b.ring(0, cz, y, r, DARK_PRISMARINE);
            b.ring(0, cz, 3 + k, r, POLISHED_DEEPSLATE);
        }
        for (int y = 0; y <= 11; y++) b.ring(0, cz, y, 22, y == 11 ? DARK_PRISMARINE : LIGHT_BLUE_STAINED_GLASS);

        // two gates
        for (int side : new int[]{-1, 1}) {
            int inner = cz + side * 15, outer = cz + side * 22;
            b.fill(-1, 0, inner, 1, 2, outer, AIR);
            b.fill(-1, -1, inner, 1, -1, outer, BLACK_CONCRETE);
            b.fill(-2, 3, inner, 2, 3, inner, SEA_LANTERN);
            b.fill(-2, 0, inner, -2, 3, inner, SEA_LANTERN);
            b.fill(2, 0, inner, 2, 3, inner, SEA_LANTERN);
        }
        b.label(0, 13, cz, "<gradient:#8bebf0:#4588e3><bold>⚔ GLADIATORS ⚔</bold></gradient>", 4f);
        return b.loc(0, 0, cz);
    }

    // ------------------------------------------------------------------ capture point

    private static Location capturePoint(Builder b, int size, int hologramHeight, String name) {
        int half = Math.max(2, size / 2);
        int cz = half + 2;
        b.fill(-half, 0, cz - half, half, 8, cz + half, AIR);
        b.fill(-half, -2, cz - half, half, -2, cz + half, DEEPSLATE_TILES);
        b.fill(-half, -1, cz - half, half, -1, cz + half, WHITE_CONCRETE);
        b.walls(-half, -1, cz - half, half, -1, cz + half, LIGHT_BLUE_CONCRETE);
        if (half > 3) b.walls(-half + 2, -1, cz - half + 2, half - 2, -1, cz + half - 2, CYAN_CONCRETE);
        for (int[] c : new int[][]{{-half, -half}, {-half, half}, {half, -half}, {half, half}}) {
            b.fill(c[0], 0, cz + c[1], c[0], 2, cz + c[1], DARK_PRISMARINE);
            b.set(c[0], 3, cz + c[1], SEA_LANTERN);
        }
        b.fill(-1, -3, cz - 1, 1, -3, cz + 1, IRON_BLOCK);
        b.set(0, -2, cz, BEACON);
        b.set(0, -1, cz, LIGHT_BLUE_STAINED_GLASS);
        b.label(0, hologramHeight, cz, "<#8bebf0><bold>CAPTURE POINT</bold><newline><white>" + name, 2f);
        return b.loc(0, 0, cz);
    }

    // ------------------------------------------------------------------ winners podium

    private static Location podium(Builder b) {
        int cz = 4;
        b.fill(-6, 0, cz - 1, 6, 9, cz + 3, AIR);
        b.fill(-6, -1, cz - 2, 6, -1, cz + 3, BLACK_CONCRETE);
        b.fill(-6, -1, cz - 2, 6, -1, cz - 2, LIGHT_BLUE_CONCRETE);
        b.fill(-1, 0, cz, 1, 2, cz + 2, GOLD_BLOCK);
        b.fill(-4, 0, cz, -2, 1, cz + 2, IRON_BLOCK);
        b.fill(2, 0, cz, 4, 0, cz + 2, WAXED_COPPER_BLOCK);
        b.fill(-6, 0, cz + 3, 6, 7, cz + 3, LIGHT_BLUE_STAINED_GLASS);
        b.fill(-6, 8, cz + 3, 6, 8, cz + 3, SEA_LANTERN);
        b.label(0, 5.5, cz + 1, "<#FFD54A><bold>#1", 2.5f);
        b.label(-3, 4.5, cz + 1, "<#d9d9d9><bold>#2", 2f);
        b.label(3, 3.5, cz + 1, "<#E29A4A><bold>#3", 2f);
        return b.loc(0, 3, cz + 1);
    }
}
