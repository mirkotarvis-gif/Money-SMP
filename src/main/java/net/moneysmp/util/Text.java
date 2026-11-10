package net.moneysmp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/** MiniMessage helpers, sounds, time formatting and default-font pixel widths. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static Component prefix = Component.empty();

    private Text() {}

    public static void load(FileConfiguration config) {
        prefix = MM.deserialize(config.getString("messages.prefix", ""));
    }

    public static Component mm(String input, TagResolver... resolvers) {
        return MM.deserialize(input, resolvers);
    }

    public static Component msg(String input, TagResolver... resolvers) {
        return prefix.append(mm(input, resolvers));
    }

    public static void send(CommandSender to, String input, TagResolver... resolvers) {
        to.sendMessage(msg(input, resolvers));
    }

    public static void broadcast(String input, TagResolver... resolvers) {
        Bukkit.getServer().sendMessage(msg(input, resolvers));
    }

    /** Plain text placeholder (not parsed as MiniMessage). */
    public static TagResolver p(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    public static TagResolver c(String key, Component value) {
        return Placeholder.component(key, value);
    }

    public static TextColor hex(String hex, TextColor fallback) {
        if (hex == null) return fallback;
        TextColor c = TextColor.fromHexString(hex.startsWith("#") ? hex : "#" + hex);
        return c == null ? fallback : c;
    }

    @SuppressWarnings("deprecation")
    public static Sound sound(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MoneySMP] Unknown sound: " + name);
            return null;
        }
    }

    public static void playAll(String soundName) {
        Sound s = sound(soundName);
        if (s == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), s, 1f, 1f);
    }

    public static void play(Player p, String soundName) {
        Sound s = sound(soundName);
        if (s != null) p.playSound(p.getLocation(), s, 1f, 1f);
    }

    public static String time(long seconds) {
        if (seconds < 0) seconds = 0;
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%d:%02d", m, s);
    }

    /** True for a single private-use character (custom resource pack glyph). */
    public static boolean isGlyph(String s) {
        if (s == null || s.isEmpty()) return false;
        int cp = s.codePointAt(0);
        if (Character.charCount(cp) != s.length()) return false;
        return (cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xF0000;
    }

    /** Approximate width in pixels of text in the default Minecraft font (incl. 1px spacing). */
    public static int pixelWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            w += charWidth(cp);
        }
        return w;
    }

    private static int charWidth(int c) {
        // MoneySMP-ResourcePack glyphs
        if (c == 0xE000 || c == 0xE030) return 10;
        if (c >= 0xE001 && c <= 0xE006) return 38;
        if (c >= 0xE010 && c <= 0xE015) return 11;
        if (c == 0xE020 || c == 0xE021) return 32;
        return switch (c) {
            case 'i', '!', '.', ',', ':', ';', '\'', '|' -> 2;
            case 'l', '`' -> 3;
            case ' ', 't', 'I', '[', ']', '(', ')', '{', '}', '"', '*' -> 4;
            case 'f', 'k', '<', '>' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }
}
