package me.morperx.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MorperXCore extends JavaPlugin implements Listener {

    private BossBar bossBar;
    private BukkitTask announcementTask;

    private final List<String> announcements = new ArrayList<>();
    private int announcementIndex = 0;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        setupBossBar();
        startAnnouncementTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
            updatePlayerTab(player);
        }

        getLogger().info("======================================");
        getLogger().info("       MorperX_Core v1.0.0");
        getLogger().info("======================================");
        getLogger().info("MorperX Core başarıyla aktif edildi.");
        getLogger().info("Scoreboard: " + getConfig().getBoolean("scoreboard.enabled"));
        getLogger().info("BossBar: " + getConfig().getBoolean("bossbar.enabled"));
        getLogger().info("Tablist: " + getConfig().getBoolean("tablist.enabled"));
        getLogger().info("======================================");
    }

    @Override
    public void onDisable() {

        if (announcementTask != null) {
            announcementTask.cancel();
        }

        if (bossBar != null) {
            bossBar.removeAll();
        }

        getLogger().info("MorperX_Core kapatıldı.");
    }

    // =========================================================
    // BOSSBAR
    // =========================================================

    private void setupBossBar() {

        if (!getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }

        announcements.clear();

        List<String> configuredMessages =
                getConfig().getStringList("bossbar.announcements");

        if (configuredMessages != null) {
            announcements.addAll(configuredMessages);
        }

        if (announcements.isEmpty()) {
            announcements.add("&b&lMORPERX &7» &fdiscord.gg/morperx");
        }

        bossBar = Bukkit.createBossBar(
                color(getCurrentAnnouncement()),
                BarColor.BLUE,
                BarStyle.SOLID
        );

        bossBar.setProgress(1.0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private void startAnnouncementTask() {

        if (!getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }

        long intervalSeconds =
                Math.max(1, getConfig().getLong(
                        "bossbar.announcement-interval",
                        10
                ));

        announcementTask = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {

                    if (bossBar == null || announcements.isEmpty()) {
                        return;
                    }

                    announcementIndex++;

                    if (announcementIndex >= announcements.size()) {
                        announcementIndex = 0;
                    }

                    bossBar.setTitle(
                            color(getCurrentAnnouncement())
                    );

                },
                intervalSeconds * 20L,
                intervalSeconds * 20L
        );
    }

    private String getCurrentAnnouncement() {

        if (announcements.isEmpty()) {
            return "&b&lMORPERX &7» &fdiscord.gg/morperx";
        }

        return announcements.get(
                Math.max(
                        0,
                        Math.min(
                                announcementIndex,
                                announcements.size() - 1
                        )
                )
        );
    }

    private void showPlayerEvent(String message) {

        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(color(message));

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (bossBar == null) {
                        return;
                    }

                    bossBar.setTitle(
                            color(getCurrentAnnouncement())
                    );

                },
                100L
        );
    }

    // =========================================================
    // PLAYER JOIN
    // =========================================================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (getConfig().getBoolean("join-quit.enabled", true)) {

            String joinMessage = getConfig().getString(
                    "join-quit.join",
                    "&a+ &e%player% &7sunucuya katıldı."
            );

            event.setJoinMessage(
                    color(
                            replacePlayer(
                                    joinMessage,
                                    player
                            )
                    )
            );

        } else {
            event.setJoinMessage(null);
        }

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    addPlayerToBossBar(player);
                    updatePlayerScoreboard(player);
                    updatePlayerTab(player);

                    String bossMessage = getConfig().getString(
                            "bossbar.join-message",
                            "&a● &e%player% &fsunucuya katıldı!"
                    );

                    showPlayerEvent(
                            replacePlayer(
                                    bossMessage,
                                    player
                            )
                    );

                    updateAllPlayers();

                },
                1L
        );
    }

    // =========================================================
    // PLAYER QUIT
    // =========================================================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (getConfig().getBoolean("join-quit.enabled", true)) {

            String quitMessage = getConfig().getString(
                    "join-quit.quit",
                    "&c- &e%player% &7sunucudan ayrıldı."
            );

            event.setQuitMessage(
                    color(
                            replacePlayer(
                                    quitMessage,
                                    player
                            )
                    )
            );

        } else {
            event.setQuitMessage(null);
        }

        if (bossBar != null) {
            bossBar.removePlayer(player);
        }

        String bossMessage = getConfig().getString(
                "bossbar.quit-message",
                "&c● &e%player% &fsunucudan ayrıldı!"
        );

        showPlayerEvent(
                replacePlayer(
                        bossMessage,
                        player
                )
        );

        Bukkit.getScheduler().runTaskLater(
                this,
                this::updateAllPlayers,
                1L
        );
    }

    // =========================================================
    // SCOREBOARD
    // =========================================================

    private void updatePlayerScoreboard(Player player) {

        if (!getConfig().getBoolean("scoreboard.enabled", true)) {
            player.setScoreboard(
                    Bukkit.getScoreboardManager().getNewScoreboard()
            );
            return;
        }

        ScoreboardManager manager =
                Bukkit.getScoreboardManager();

        if (manager == null) {
            return;
        }

        Scoreboard scoreboard =
                manager.getNewScoreboard();

        Objective objective =
                scoreboard.registerNewObjective(
                        "morperx",
                        "dummy",
                        color(
                                getConfig().getString(
                                        "scoreboard.title",
                                        "&6&lMORPERX"
                                )
                        )
                );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines =
                getConfig().getStringList(
                        "scoreboard.lines"
                );

        if (lines.isEmpty()) {
            lines = Collections.singletonList(
                    "&bdiscord.gg/morperx"
            );
        }

        /*
         * Minecraft scoreboard aynı yazıyı iki kere kabul etmez.
         * Bu yüzden duplicate satırlara görünmez renk kodları ekliyoruz.
         */

        int score = lines.size();

        for (String originalLine : lines) {

            String line = replacePlaceholders(
                    originalLine,
                    player
            );

            line = color(line);

            String uniqueLine =
                    makeUniqueScoreboardLine(
                            line,
                            score
                    );

            Team team =
                    scoreboard.registerNewTeam(
                            "line" + score
                    );

            team.addEntry(uniqueLine);

            objective.getScore(uniqueLine)
                    .setScore(score);

            score--;
        }

        player.setScoreboard(scoreboard);
    }

    private String makeUniqueScoreboardLine(
            String line,
            int number
    ) {

        String[] codes = {
                ChatColor.BLACK.toString(),
                ChatColor.DARK_BLUE.toString(),
                ChatColor.DARK_GREEN.toString(),
                ChatColor.DARK_AQUA.toString(),
                ChatColor.DARK_RED.toString(),
                ChatColor.DARK_PURPLE.toString(),
                ChatColor.GOLD.toString(),
                ChatColor.GRAY.toString(),
                ChatColor.DARK_GRAY.toString(),
                ChatColor.BLUE.toString(),
                ChatColor.GREEN.toString(),
                ChatColor.AQUA.toString(),
                ChatColor.RED.toString(),
                ChatColor.LIGHT_PURPLE.toString(),
                ChatColor.YELLOW.toString(),
                ChatColor.WHITE.toString()
        };

        String uniqueCode =
                codes[Math.abs(number) % codes.length];

        return line + uniqueCode;
    }

    // =========================================================
    // TABLIST
    // =========================================================

    private void updatePlayerTab(Player player) {

        if (!getConfig().getBoolean("tablist.enabled", true)) {
            return;
        }

        List<String> header =
                getConfig().getStringList(
                        "tablist.header"
                );

        List<String> footer =
                getConfig().getStringList(
                        "tablist.footer"
                );

        String headerText =
                formatMultiline(
                        header,
                        player
                );

        String footerText =
                formatMultiline(
                        footer,
                        player
                );

        player.setPlayerListHeaderFooter(
                headerText,
                footerText
        );
    }

    // =========================================================
    // GLOBAL UPDATE
    // =========================================================

    private void updateAllPlayers() {

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
            updatePlayerTab(player);
        }
    }

    private void addPlayerToBossBar(Player player) {

        if (bossBar == null) {
            return;
        }

        bossBar.addPlayer(player);
    }

    // =========================================================
    // PLACEHOLDERS
    // =========================================================

    private String replacePlayer(
            String text,
            Player player
    ) {

        if (text == null) {
            return "";
        }

        return text.replace(
                "%player%",
                player.getName()
        );
    }

    private String replacePlaceholders(
            String text,
            Player player
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace(
                        "%player%",
                        player.getName()
                )
                .replace(
                        "%online%",
                        String.valueOf(
                                Bukkit.getOnlinePlayers().size()
                        )
                )
                .replace(
                        "%max%",
                        String.valueOf(
                                Bukkit.getMaxPlayers()
                        )
                );
    }

    private String formatMultiline(
            List<String> lines,
            Player player
    ) {

        if (lines == null || lines.isEmpty()) {
            return "";
        }

        List<String> formatted =
                new ArrayList<>();

        for (String line : lines) {

            formatted.add(
                    replacePlaceholders(
                            line,
                            player
                    )
            );
        }

        return color(
                String.join(
                        "\n",
                        formatted
                )
        );
    }

    // =========================================================
    // COLOR
    // =========================================================

    private String color(String text) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
