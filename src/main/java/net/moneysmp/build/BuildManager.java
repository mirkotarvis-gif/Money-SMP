package net.moneysmp.build;

import net.moneysmp.MoneySMP;
import net.moneysmp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/** Places a Builder's blocks a few thousand per tick and remembers what was there for /eventmain build undo. */
public final class BuildManager {

    private static final int PER_TICK = 4000;

    private record Undo(World world, List<Map.Entry<Builder.Pos, BlockData>> old, List<UUID> entities) {}

    private final MoneySMP plugin;
    private final Map<UUID, Deque<Undo>> history = new HashMap<>();
    private boolean busy;

    public BuildManager(MoneySMP plugin) {
        this.plugin = plugin;
    }

    public boolean busy() { return busy; }

    public void paste(Player player, Builder b, String name, Runnable after) {
        busy = true;
        World world = b.world();
        List<Map.Entry<Builder.Pos, BlockData>> todo = new ArrayList<>(b.blocks().entrySet());
        List<Map.Entry<Builder.Pos, BlockData>> old = new ArrayList<>(todo.size());
        Text.send(player, "<gray>Building <white><name></white> (<count> blocks)...", Text.p("name", name), Text.p("count", todo.size()));

        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                int end = Math.min(todo.size(), i + PER_TICK);
                for (; i < end; i++) {
                    Builder.Pos p = todo.get(i).getKey();
                    if (p.y() < world.getMinHeight() || p.y() >= world.getMaxHeight()) continue;
                    Block block = world.getBlockAt(p.x(), p.y(), p.z());
                    old.add(Map.entry(p, block.getBlockData()));
                    block.setBlockData(todo.get(i).getValue(), false);
                }
                if (i < todo.size()) return;
                cancel();
                List<UUID> entities = new ArrayList<>();
                for (Builder.Label l : b.labels()) entities.add(spawnLabel(b, l).getUniqueId());
                history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).push(new Undo(world, old, entities));
                busy = false;
                Text.send(player, "<green>Built <white><name></white>. Use <white>/eventmain build undo</white> to remove it.", Text.p("name", name));
                if (after != null) after.run();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private TextDisplay spawnLabel(Builder b, Builder.Label l) {
        return b.world().spawn(b.loc(l.x(), l.y(), l.z()), TextDisplay.class, td -> {
            td.text(Text.mm(l.text()));
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowed(true);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setPersistent(true);
            td.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(l.scale(), l.scale(), l.scale()), new AxisAngle4f()));
        });
    }

    public boolean undo(Player player) {
        Deque<Undo> stack = history.get(player.getUniqueId());
        if (stack == null || stack.isEmpty() || busy) return false;
        Undo u = stack.pop();
        busy = true;
        for (UUID id : u.entities()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        List<Map.Entry<Builder.Pos, BlockData>> list = u.old();
        new BukkitRunnable() {
            int i = list.size() - 1;

            @Override
            public void run() {
                int end = Math.max(-1, i - PER_TICK);
                for (; i > end; i--) {
                    Builder.Pos p = list.get(i).getKey();
                    u.world().getBlockAt(p.x(), p.y(), p.z()).setBlockData(list.get(i).getValue(), false);
                }
                if (i >= 0) return;
                cancel();
                busy = false;
                Text.send(player, "<green>Build removed.");
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }
}
