package xyz.exyron.fastcrystals;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastCrystalsPlugin extends JavaPlugin implements Listener {

  private final FileConfiguration configuration = this.getConfig();
  private final boolean currentStatus = this.configuration.getBoolean("defaultStatus");

  private final Set<UUID> playersUUID = new HashSet<>();

  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
      new FastCrystalsPlaceholder(this).register();
    }

    String disableMessage = this.colorize(Objects.requireNonNull(this.configuration.getString("disableMessage")));
    String enableMessage = this.colorize(Objects.requireNonNull(this.configuration.getString("enableMessage")));

    if (this.configuration.getBoolean("commandEnabled")) {
      Objects.requireNonNull(this.getCommand("fastcrystals")).setExecutor(((sender, command, label, args) -> {
        if (sender instanceof Player player) {
          UUID playerUniqueId = player.getUniqueId();

          if (this.playersUUID.contains(playerUniqueId)) {
            player.sendMessage(disableMessage);
            this.playersUUID.remove(playerUniqueId);
          } else {
            player.sendMessage(enableMessage);
            this.playersUUID.add(playerUniqueId);
          }
        } else {
          sender.sendMessage(this.colorize("&cYou cannot run this command from Console."));
        }
        return false;
      }));
    }

    this.getServer().getPluginManager().registerEvents(this, this);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player && event.getEntity() instanceof EnderCrystal crystal && this.isUsingFastCrystals(player.getUniqueId())) {
      final PacketContainer destroyEntity = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
      destroyEntity.getIntegers().writeSafely(0, 1);
      destroyEntity.getIntegerArrays().writeSafely(0, new int[]{crystal.getEntityId()});

      ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyEntity);
	}
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (this.currentStatus) {
      this.playersUUID.add(event.getPlayer().getUniqueId());
    }
  }

  public boolean isUsingFastCrystals(UUID uniqueId) {
    return this.playersUUID.contains(uniqueId);
  }

  private static final Pattern HEX_PATTERN = Pattern.compile("#[A-Fa-f0-9]{6}");

  private String colorize(String message) {
    Matcher matcher = HEX_PATTERN.matcher(message);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      matcher.appendReplacement(result, ChatColor.of(matcher.group()).toString());
    }

    matcher.appendTail(result);
    message = result.toString();

    return ChatColor.translateAlternateColorCodes('&', message.replace(">>", "»").replace("<<", "«"));
  }

  public FileConfiguration getConfiguration() {
    return configuration;
  }
}