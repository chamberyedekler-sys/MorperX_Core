package me.morperx.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MorperXCore extends JavaPlugin implements Listener, CommandExecutor {

    private BossBar bossBar;
    private BukkitTask announcementTask;

    private final List<String> announcements = new ArrayList<>();
    private int announcementIndex = 0;

    private NamespacedKey modeKey;

    private static final String GUI_TITLE =
            "§8» §bMorperX §fSunucu Seçimi";

    @Override
    public void onEnable() {

        saveDefaultConfig();

        modeKey = new NamespacedKey(this, "player_mode");

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("sunucu") != null) {
            getCommand("sunucu").setExecutor(this);
        }

        if (getCommand("mod") != null) {
            getCommand("mod").setExecutor(this);
        }

        setupBossBar();
        startAnnouncementTask();

        /*
         * Sunucu daha önce açıkken oyuncular bulunuyorsa
         * onların UI'larını yeniden kuruyoruz.
         */
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
            updatePlayerTab(player);
            ensureSafePlayerPosition(player);
        }

        getLogger().info("======================================");
        getLogger().info("       MorperX_Core v1.2.0");
        getLogger().info("======================================");
        getLogger().info("MorperX Core başarıyla aktif edildi.");
        getLogger().info("Lobby sistemi aktif.");
        getLogger().info("Survival sistemi aktif.");
        getLogger().info("Skyblock sistemi aktif.");
        getLogger().info("Mod seçim GUI aktif.");
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
    // COMMANDS
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("sunucu")
                || command.getName().equalsIgnoreCase("mod")) {

            openModeGUI(player);
            return true;
        }

        return true;
    }

    // =========================================================
    // MODE GUI
    // =========================================================

    private void openModeGUI(Player player) {

        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                GUI_TITLE
        );

        ItemStack lobby = createItem(
                Material.GRASS_BLOCK,
                "&a&lLOBBY",
                "&7Sunucunun ana merkezi.",
                "",
                "&eTıklayarak Lobby'ye git."
        );

        ItemStack survival = createItem(
                Material.IRON_SWORD,
                "&c&lSURVIVAL",
                "&7Klasik hayatta kalma deneyimi.",
                "",
                "&eTıklayarak Survival'a git."
        );

        ItemStack skyblock = createItem(
                Material.GRASS_BLOCK,
                "&b&lSKYBLOCK",
                "&7Kendi adanı geliştir.",
                "",
                "&eTıklayarak Skyblock'a git."
        );

        inventory.setItem(11, lobby);
        inventory.setItem(13, survival);
        inventory.setItem(15, skyblock);

        player.openInventory(inventory);
    }

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(color(name));

            List<String> formattedLore = new ArrayList<>();

            for (String line : lore) {
                formattedLore.add(color(line));
            }

            meta.setLore(formattedLore);

            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == 11) {

            player.closeInventory();

            Bukkit.getScheduler().runTask(
                    this,
                    () -> teleportToMode(player, "lobby")
            );

        } else if (slot == 13) {

            player.closeInventory();

            Bukkit.getScheduler().runTask(
                    this,
                    () -> teleportToMode(player, "survival")
            );

        } else if (slot == 15) {

            player.closeInventory();

            Bukkit.getScheduler().runTask(
                    this,
                    () -> teleportToMode(player, "skyblock")
            );
        }
    }

    // =========================================================
    // PLAYER JOIN
    // =========================================================

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (getConfig().getBoolean(
                "join-quit.enabled",
                true
        )) {

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

                    /*
                     * İlk defa giriyorsa kesinlikle Lobby.
                     */
                    if (!player.hasPlayedBefore()) {

                        teleportToMode(
                                player,
                                "lobby"
                        );

                        player.sendMessage(
                                color(
                                        "&8&m--------------------------------"
                                )
                        );

                        player.sendMessage(
                                color(
                                        "&b&lMORPERX &7» &fHoş geldin &e"
                                                + player.getName()
                                                + "&f!"
                                )
                        );

                        player.sendMessage(
                                color(
                                        "&7Sunucu seçmek için &b/sunucu"
                                                + " &7yazabilirsin."
                                )
                        );

                        player.sendMessage(
                                color(
                                        "&8&m--------------------------------"
                                )
                        );

                    } else {

                        String savedMode =
                                getPlayerMode(player);

                        teleportToMode(
                                player,
                                savedMode
                        );
                    }

                    updatePlayerScoreboard(player);
                    updatePlayerTab(player);

                    String bossMessage =
                            getConfig().getString(
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
                2L
        );
    }

    // =========================================================
    // PLAYER QUIT
    // =========================================================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (getConfig().getBoolean(
                "join-quit.enabled",
                true
        )) {

            String quitMessage =
                    getConfig().getString(
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

        String bossMessage =
                getConfig().getString(
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
    // TELEPORT / MOD SYSTEM
    // =========================================================

    private void teleportToMode(
            Player player,
            String mode
    ) {

        mode = mode.toLowerCase();

        Location location;

        switch (mode) {

            case "lobby" -> {

                location = getConfiguredLocation(
                        "lobby"
                );

                player.setGameMode(
                        org.bukkit.GameMode.ADVENTURE
                );

                player.setAllowFlight(false);

                player.teleport(location);

                savePlayerMode(
                        player,
                        "lobby"
                );

                player.sendMessage(
                        color(
                                "&a✓ &fLobby'ye ışınlandın."
                        )
                );
            }

            case "survival" -> {

                location = getConfiguredLocation(
                        "survival"
                );

                player.setGameMode(
                        org.bukkit.GameMode.SURVIVAL
                );

                player.setAllowFlight(false);

                player.teleport(location);

                savePlayerMode(
                        player,
                        "survival"
                );

                player.sendMessage(
                        color(
                                "&c⚔ &fSurvival'a ışınlandın."
                        )
                );
            }

            case "skyblock" -> {

                location = getConfiguredLocation(
                        "skyblock"
                );

                player.setGameMode(
                        org.bukkit.GameMode.SURVIVAL
                );

                player.setAllowFlight(false);

                player.teleport(location);

                savePlayerMode(
                        player,
                        "skyblock"
                );

                player.sendMessage(
                        color(
                                "&b☁ &fSkyblock'a ışınlandın."
                        )
                );
            }

            default -> {

                location = getConfiguredLocation(
                        "lobby"
                );

                player.setGameMode(
                        org.bukkit.GameMode.ADVENTURE
                );

                player.teleport(location);

                savePlayerMode(
                        player,
                        "lobby"
                );
            }
        }

        updatePlayerScoreboard(player);
        updatePlayerTab(player);
    }

    private void savePlayerMode(
            Player player,
            String mode
    ) {

        player.getPersistentDataContainer().set(
                modeKey,
                PersistentDataType.STRING,
                mode
        );
    }

    private String getPlayerMode(
            Player player
    ) {

        String mode =
                player.getPersistentDataContainer().get(
                        modeKey,
                        PersistentDataType.STRING
                );

        if (mode == null || mode.isBlank()) {
            return "lobby";
        }

        if (!mode.equals("lobby")
                && !mode.equals("survival")
                && !mode.equals("skyblock")) {

            return "lobby";
        }

        return mode;
    }

    // =========================================================
    // SAFE SPAWN
    // =========================================================

    private void ensureSafePlayerPosition(
            Player player
    ) {

        Location location =
                player.getLocation();

        if (location.getBlock().isPassable()
                && location.clone()
                .add(0, 1, 0)
                .getBlock()
                .isPassable()) {

            return;
        }

        String mode =
                getPlayerMode(player);

        player.teleport(
                getConfiguredLocation(mode)
        );
    }

    // =========================================================
    // WORLD / LOCATION
    // =========================================================

    private Location getConfiguredLocation(
            String mode
    ) {

        String worldName =
                getConfig().getString(
                        "locations."
                                + mode
                                + ".world",
                        "world"
                );

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {

            world = Bukkit.createWorld(
                    new WorldCreator(worldName)
            );
        }

        String path =
                "locations."
                        + mode;

        double x =
                getConfig().getDouble(
                        path + ".x"
                );

        double y =
                getConfig().getDouble(
                        path + ".y"
                );

        double z =
                getConfig().getDouble(
                        path + ".z"
                );

        float yaw =
                (float) getConfig().getDouble(
                        path + ".yaw",
                        0
                );

        float pitch =
                (float) getConfig().getDouble(
                        path + ".pitch",
                        0
                );

        return new Location(
                world,
                x + 0.5,
                y,
                z + 0.5,
                yaw,
                pitch
        );
    }

    // =========================================================
    // BOSSBAR
    // =========================================================

    private void setupBossBar() {

        if (!getConfig().getBoolean(
                "bossbar.enabled",
                true
        )) {
            return;
        }

        announcements.clear();

        List<String> configuredMessages =
                getConfig().getStringList(
                        "bossbar.announcements"
                );

        announcements.addAll(
                configuredMessages
        );

        if (announcements.isEmpty()) {

            announcements.add(
                    "&b&lMORPERX &7» &fdiscord.gg/morperx"
            );
        }

        bossBar = Bukkit.createBossBar(
                color(
                        getCurrentAnnouncement()
                ),
                BarColor.BLUE,
                BarStyle.SOLID
        );

        bossBar.setProgress(1.0);

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            bossBar.addPlayer(player);
        }
    }

    private void startAnnouncementTask() {

        if (!getConfig().getBoolean(
                "bossbar.enabled",
                true
        )) {
            return;
        }

        long intervalSeconds =
                Math.max(
                        1,
                        getConfig().getLong(
                                "bossbar.announcement-interval",
                                10
                        )
                );

        announcementTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                this,
                                () -> {

                                    if (bossBar == null
                                            || announcements.isEmpty()) {
                                        return;
                                    }

                                    announcementIndex++;

                                    if (announcementIndex
                                            >= announcements.size()) {

                                        announcementIndex = 0;
                                    }

                                    bossBar.setTitle(
                                            color(
                                                    getCurrentAnnouncement()
                                            )
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

    private void showPlayerEvent(
            String message
    ) {

        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(
                color(message)
        );

        Bukkit.getScheduler().runTaskLater(
                this,
                () -> {

                    if (bossBar == null) {
                        return;
                    }

                    bossBar.setTitle(
                            color(
                                    getCurrentAnnouncement()
                            )
                    );

                },
                100L
        );
    }

    private void addPlayerToBossBar(
            Player player
    ) {

        if (bossBar == null) {
            return;
        }

        bossBar.addPlayer(player);
    }

    // =========================================================
    // SCOREBOARD
    // =========================================================

    private void updatePlayerScoreboard(
            Player player
    ) {

        if (!getConfig().getBoolean(
                "scoreboard.enabled",
                true
        )) {

            player.setScoreboard(
                    Bukkit.getScoreboardManager()
                            .getNewScoreboard()
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

        objective.setDisplaySlot(
                DisplaySlot.SIDEBAR
        );

        List<String> lines =
                getConfig().getStringList(
                        "scoreboard.lines"
                );

        if (lines.isEmpty()) {

            lines =
                    Collections.singletonList(
                            "&bSunucu: &f%mode%"
                    );
        }

        int score = lines.size();

        for (String originalLine :
                lines) {

            String line =
                    replacePlaceholders(
                            originalLine,
                            player
                    );

            line = color(line);

            String uniqueLine =
                    makeUniqueScoreboardLine(
                            line,
                            score
                    );

            objective.getScore(
                    uniqueLine
            ).setScore(score);

            score--;
        }

        player.setScoreboard(
                scoreboard
        );
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
                codes[
                        Math.abs(number)
                                % codes.length
                ];

        return line + uniqueCode;
    }

    // =========================================================
    // TABLIST
    // =========================================================

    private void updatePlayerTab(
            Player player
    ) {

        if (!getConfig().getBoolean(
                "tablist.enabled",
                true
        )) {
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

        player.setPlayerListHeaderFooter(
                formatMultiline(
                        header,
                        player
                ),
                formatMultiline(
                        footer,
                        player
                )
        );
    }

    // =========================================================
    // GLOBAL UPDATE
    // =========================================================

    private void updateAllPlayers() {

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            updatePlayerScoreboard(player);
            updatePlayerTab(player);
        }
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
                                Bukkit.getOnlinePlayers()
                                        .size()
                        )
                )
                .replace(
                        "%max%",
                        String.valueOf(
                                Bukkit.getMaxPlayers()
                        )
                )
                .replace(
                        "%mode%",
                        getDisplayMode(
                                getPlayerMode(player)
                        )
                );
    }

    private String getDisplayMode(
            String mode
    ) {

        return switch (mode) {

            case "lobby" ->
                    "Lobby";

            case "survival" ->
                    "Survival";

            case "skyblock" ->
                    "Skyblock";

            default ->
                    "Lobby";
        };
    }

    private String formatMultiline(
            List<String> lines,
            Player player
    ) {

        if (lines == null
                || lines.isEmpty()) {

            return "";
        }

        List<String> formatted =
                new ArrayList<>();

        for (String line :
                lines) {

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

    private String color(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
