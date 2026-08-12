package me.morperx.core;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MorperXCore extends JavaPlugin implements Listener, TabExecutor {

    private static final String MODE_GUI_TITLE =
            "§0§lMORPERX NETWORK";

    private static final String MODE_SELECTOR_NAME =
            "§b§lMorperX Mod Seçici";

    private BossBar bossBar;
    private BukkitTask announcementTask;

    private final List<String> announcements = new ArrayList<>();
    private int announcementIndex = 0;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("mod") != null) {
            getCommand("mod").setExecutor(this);
            getCommand("mod").setTabCompleter(this);
        }

        setupBossBar();
        startAnnouncementTask();

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(player);
            updatePlayerTab(player);
        }

        getLogger().info("======================================");
        getLogger().info("       MorperX_Core v1.1.0");
        getLogger().info("======================================");
        getLogger().info("MorperX Core başarıyla aktif edildi.");
        getLogger().info("Mod Seçici: AKTİF");
        getLogger().info("GUI: AKTİF");
        getLogger().info("Teleport Sistemi: AKTİF");
        getLogger().info("Skyblock Sistemi: AKTİF");
        getLogger().info("Scoreboard: " +
                getConfig().getBoolean("scoreboard.enabled"));
        getLogger().info("BossBar: " +
                getConfig().getBoolean("bossbar.enabled"));
        getLogger().info("Tablist: " +
                getConfig().getBoolean("tablist.enabled"));
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
                getConfig().getStringList(
                        "bossbar.announcements"
                );

        if (configuredMessages != null) {
            announcements.addAll(configuredMessages);
        }

        if (announcements.isEmpty()) {
            announcements.add(
                    "&b&lMORPERX &7» &fdiscord.gg/morperx"
            );
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
                Math.max(
                        1,
                        getConfig().getLong(
                                "bossbar.announcement-interval",
                                10
                        )
                );

        announcementTask =
                Bukkit.getScheduler().runTaskTimer(
                        this,
                        () -> {

                            if (bossBar == null ||
                                    announcements.isEmpty()) {
                                return;
                            }

                            announcementIndex++;

                            if (announcementIndex >=
                                    announcements.size()) {

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

        if (getConfig().getBoolean(
                "join-quit.enabled",
                true
        )) {

            String joinMessage =
                    getConfig().getString(
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

                    giveModeSelector(player);

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

                    /*
                     * Oyuncu sunucuya girdikten kısa süre sonra
                     * mod seçim menüsünü açıyoruz.
                     */
                    Bukkit.getScheduler().runTaskLater(
                            this,
                            () -> {

                                if (player.isOnline()) {
                                    openModeMenu(player);
                                }

                            },
                            10L
                    );

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
    // MOD SELECTOR ITEM
    // =========================================================

    private void giveModeSelector(Player player) {

        if (!getConfig().getBoolean(
                "mode-selector.enabled",
                true
        )) {
            return;
        }

        /*
         * Aynı itemden varsa tekrar vermiyoruz.
         */
        for (ItemStack item :
                player.getInventory().getContents()) {

            if (isModeSelector(item)) {
                return;
            }
        }

        ItemStack selector =
                new ItemStack(Material.COMPASS);

        ItemMeta meta =
                selector.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    MODE_SELECTOR_NAME
            );

            List<String> lore =
                    new ArrayList<>();

            lore.add(
                    "§7MorperX oyun modlarını aç."
            );

            lore.add("");
            lore.add(
                    "§eSağ tıkla!"
            );

            meta.setLore(lore);

            selector.setItemMeta(meta);
        }

        int slot =
                getConfig().getInt(
                        "mode-selector.slot",
                        4
                );

        if (slot < 0 || slot > 8) {
            slot = 4;
        }

        player.getInventory().setItem(
                slot,
                selector
        );
    }

    private boolean isModeSelector(ItemStack item) {

        if (item == null ||
                item.getType() != Material.COMPASS ||
                !item.hasItemMeta()) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        return meta != null &&
                MODE_SELECTOR_NAME.equals(
                        meta.getDisplayName()
                );
    }

    // =========================================================
    // MODE SELECTOR INTERACTION
    // =========================================================

    @EventHandler
    public void onPlayerInteract(
            PlayerInteractEvent event
    ) {

        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {

            return;
        }

        ItemStack item =
                event.getItem();

        if (!isModeSelector(item)) {
            return;
        }

        event.setCancelled(true);

        openModeMenu(event.getPlayer());
    }

    // =========================================================
    // MODE GUI
    // =========================================================

    private void openModeMenu(Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        MODE_GUI_TITLE
                );

        /*
         * Dekoratif camlar
         */
        ItemStack filler =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        /*
         * Lobby
         */
        inventory.setItem(
                11,
                createItem(
                        Material.GRASS_BLOCK,
                        "§a§l🏠 Lobby",
                        "§7MorperX merkezine git.",
                        "",
                        "§eTıklamak için seç!"
                )
        );

        /*
         * Survival
         */
        inventory.setItem(
                13,
                createItem(
                        Material.IRON_SWORD,
                        "§c§l🌲 Survival",
                        "§7Survival dünyasına git.",
                        "",
                        "§eTıklamak için seç!"
                )
        );

        /*
         * Skyblock
         */
        inventory.setItem(
                15,
                createItem(
                        Material.GRASS_BLOCK,
                        "§b§l☁ Skyblock",
                        "§7Gökyüzündeki adana git.",
                        "",
                        "§eTıklamak için seç!"
                )
        );

        player.openInventory(inventory);

        player.playSound(
                player.getLocation(),
                Sound.BLOCK_CHEST_OPEN,
                1.0f,
                1.0f
        );
    }

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(
                        List.of(lore)
                );
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    // =========================================================
    // GUI CLICK
    // =========================================================

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!MODE_GUI_TITLE.equals(
                event.getView().getTitle()
        )) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot =
                event.getRawSlot();

        if (slot == 11) {

            player.closeInventory();

            teleportToMode(
                    player,
                    "lobby"
            );

        } else if (slot == 13) {

            player.closeInventory();

            teleportToMode(
                    player,
                    "survival"
            );

        } else if (slot == 15) {

            player.closeInventory();

            teleportToMode(
                    player,
                    "skyblock"
            );
        }
    }

    // =========================================================
    // MODE TELEPORT
    // =========================================================

    private void teleportToMode(
            Player player,
            String mode
    ) {

        String path =
                "locations." + mode;

        String worldName =
                getConfig().getString(
                        path + ".world",
                        "world"
                );

        World world =
                Bukkit.getWorld(worldName);

        if (world == null) {

            player.sendMessage(
                    color(
                            "&cMorperX: &f" +
                            mode +
                            " dünyası bulunamadı."
                    )
            );

            getLogger().warning(
                    "Dünya bulunamadı: " +
                    worldName
            );

            return;
        }

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

        /*
         * Skyblock seçildiyse ada oluşturuluyor.
         */
        if (mode.equalsIgnoreCase("skyblock")) {

            createSkyblockIsland(
                    world,
                    x,
                    y,
                    z
            );
        }

        Location location =
                new Location(
                        world,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                );

        player.teleport(location);

        player.sendMessage(
                color(
                        "&b&lMORPERX &7» &f" +
                        getModeDisplayName(mode) +
                        " &7moduna geçtin."
                )
        );

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ENDERMAN_TELEPORT,
                1.0f,
                1.0f
        );

        updatePlayerScoreboard(player);
        updatePlayerTab(player);
    }

    private String getModeDisplayName(
            String mode
    ) {

        return switch (mode.toLowerCase()) {

            case "lobby" -> "§aLobby";

            case "survival" -> "§cSurvival";

            case "skyblock" -> "§bSkyblock";

            default -> mode;
        };
    }

    // =========================================================
    // SKYBLOCK ISLAND
    // =========================================================

    private void createSkyblockIsland(
            World world,
            double centerX,
            double centerY,
            double centerZ
    ) {

        int x =
                (int) Math.floor(centerX);

        int y =
                (int) Math.floor(centerY);

        int z =
                (int) Math.floor(centerZ);

        /*
         * Eğer merkez zaten bir Skyblock adası
         * gibi görünüyorsa tekrar oluşturmuyoruz.
         */
        Block center =
                world.getBlockAt(
                        x,
                        y,
                        z
                );

        if (center.getType() != Material.AIR) {
            return;
        }

        /*
         * 7x7 temel ada
         */
        for (int dx = -3; dx <= 3; dx++) {

            for (int dz = -3; dz <= 3; dz++) {

                Block grass =
                        world.getBlockAt(
                                x + dx,
                                y,
                                z + dz
                        );

                grass.setType(
                        Material.GRASS_BLOCK
                );

                /*
                 * Toprak katmanları
                 */
                for (int depth = 1; depth <= 2; depth++) {

                    Block dirt =
                            world.getBlockAt(
                                    x + dx,
                                    y - depth,
                                    z + dz
                            );

                    dirt.setType(
                            Material.DIRT
                    );
                }
            }
        }

        /*
         * Ağaç gövdesi
         */
        for (int i = 1; i <= 4; i++) {

            world.getBlockAt(
                    x,
                    y + i,
                    z
            ).setType(
                    Material.OAK_LOG
            );
        }

        /*
         * Yapraklar
         */
        for (int dx = -2; dx <= 2; dx++) {

            for (int dz = -2; dz <= 2; dz++) {

                if (Math.abs(dx) +
                        Math.abs(dz) <= 3) {

                    world.getBlockAt(
                            x + dx,
                            y + 5,
                            z + dz
                    ).setType(
                            Material.OAK_LEAVES
                    );
                }
            }
        }

        /*
         * Başlangıç sandığı
         */
        Block chestBlock =
                world.getBlockAt(
                        x + 2,
                        y + 1,
                        z
                );

        chestBlock.setType(
                Material.CHEST
        );

        if (chestBlock.getState()
                instanceof Chest chest) {

            chest.getBlockInventory()
                    .addItem(
                            new ItemStack(
                                    Material.LAVA_BUCKET
                            )
                    );

            chest.getBlockInventory()
                    .addItem(
                            new ItemStack(
                                    Material.ICE,
                                    2
                            )
                    );

            chest.getBlockInventory()
                    .addItem(
                            new ItemStack(
                                    Material.OAK_SAPLING
                            )
                    );

            chest.update();
        }

        getLogger().info(
                "Skyblock adası oluşturuldu: " +
                x + ", " +
                y + ", " +
                z
        );
    }

    // =========================================================
    // SKYBLOCK PROTECTION
    // =========================================================

    @EventHandler
    public void onBlockBreak(
            BlockBreakEvent event
    ) {

        /*
         * Şimdilik özel bir koruma uygulamıyoruz.
         * Oyuncu Skyblock adasını kırabilsin.
         *
         * Gelecekte oyuncuya özel ada sistemi
         * geldiğinde burada sahiplik kontrolü yapacağız.
         */
    }

    // =========================================================
    // COMMAND
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!command.getName()
                .equalsIgnoreCase("mod")) {

            return false;
        }

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    "Bu komut sadece oyuncular içindir."
            );

            return true;
        }

        openModeMenu(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        return Collections.emptyList();
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
                            "&bdiscord.gg/morperx"
                    );
        }

        int score =
                lines.size();

        for (String originalLine : lines) {

            String line =
                    replacePlaceholders(
                            originalLine,
                            player
                    );

            line =
                    color(line);

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

        for (Player player :
                Bukkit.getOnlinePlayers()) {

            updatePlayerScoreboard(player);
            updatePlayerTab(player);
        }
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

        String mode =
                getPlayerMode(player);

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
                        mode
                );
    }

    private String getPlayerMode(
            Player player
    ) {

        String worldName =
                player.getWorld().getName();

        String lobbyWorld =
                getConfig().getString(
                        "locations.lobby.world",
                        "world"
                );

        String survivalWorld =
                getConfig().getString(
                        "locations.survival.world",
                        "world"
                );

        String skyblockWorld =
                getConfig().getString(
                        "locations.skyblock.world",
                        "world"
                );

        /*
         * Aynı world üzerinde olduğumuz için
         * koordinat bölgesine bakıyoruz.
         */
        if (worldName.equalsIgnoreCase(
                skyblockWorld
        )) {

            Location sky =
                    getConfiguredLocation(
                            "skyblock",
                            player.getWorld()
                    );

            if (sky != null &&
                    player.getLocation()
                            .distanceSquared(sky)
                            < 2500) {

                return "Skyblock";
            }
        }

        if (worldName.equalsIgnoreCase(
                survivalWorld
        )) {

            return "Survival";
        }

        if (worldName.equalsIgnoreCase(
                lobbyWorld
        )) {

            Location lobby =
                    getConfiguredLocation(
                            "lobby",
                            player.getWorld()
                    );

            if (lobby != null &&
                    player.getLocation()
                            .distanceSquared(lobby)
                            < 2500) {

                return "Lobby";
            }
        }

        return "Survival";
    }

    private Location getConfiguredLocation(
            String mode,
            World world
    ) {

        String path =
                "locations." + mode;

        return new Location(
                world,
                getConfig().getDouble(
                        path + ".x"
                ),
                getConfig().getDouble(
                        path + ".y"
                ),
                getConfig().getDouble(
                        path + ".z"
                )
        );
    }

    private String formatMultiline(
            List<String> lines,
            Player player
    ) {

        if (lines == null ||
                lines.isEmpty()) {

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
