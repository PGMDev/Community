package dev.pgm.community.vanish;

import static dev.pgm.community.utils.PGMUtils.getMatch;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.utils.PGMUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.event.PlayerVanishEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.VanishManager;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.listeners.PGMListener;

public class VanishFeature extends FeatureBase implements VanishManager {

  private static final String VANISH_KEY = "isVanished";
  private static final MetadataValue VANISH_VALUE = new FixedMetadataValue(PGM.get(), true);

  private final Set<UUID> vanishedPlayers;
  private final NickFeature nicks;

  private Future<?>
      hotbarTask; // Task is run every second to ensure vanished players retain hotbar message
  private boolean hotbarFlash;

  public VanishFeature(Configuration config, Logger logger, NickFeature nicks) {
    super(new VanishConfig(config), logger);

    this.vanishedPlayers = Sets.newHashSet();
    this.nicks = nicks;
    this.hotbarFlash = false;

    if (getConfig().isEnabled()
        && getVanishConfig().isIntegrationEnabled()
        && PGMUtils.isPGMEnabled()) {
      PGM.get().getVanishManager().setManager(this);
      this.hotbarTask =
          PGM.get()
              .getExecutor()
              .scheduleAtFixedRate(
                  () -> {
                    getOnlineVanished().forEach(p -> sendHotbarVanish(p, hotbarFlash));
                    hotbarFlash = !hotbarFlash; // Toggle boolean so we get a nice flashing effect
                  },
                  0,
                  1,
                  TimeUnit.SECONDS);

      enable();
    }
  }

  public VanishConfig getVanishConfig() {
    return (VanishConfig) getConfig();
  }

  @Override
  public void disable() {
    hotbarTask.cancel(true);
  }

  @Override
  public boolean isVanished(UUID uuid) {
    return vanishedPlayers.contains(uuid);
  }

  @Override
  public List<MatchPlayer> getOnlineVanished() {
    return vanishedPlayers.stream()
        .filter(u -> getMatch().getPlayer(u) != null)
        .map(u -> getMatch().getPlayer(u))
        .collect(Collectors.toList());
  }

  @Override
  public VanishManager getManager() {
    return this;
  }

  @Override
  public void setManager(VanishManager manager) {}

  @Override
  public boolean setVanished(MatchPlayer player, boolean vanish, boolean quiet) {
    // Keep track of the UUID and apply/remove META data, so we can detect vanish status from other
    // projects (i.e utils)
    if (vanish) {
      addVanished(player);
    } else {
      removeVanished(player);
    }

    final Match match = player.getMatch();

    // Ensure player is an observer
    match.setParty(player, match.getDefaultParty());

    // Set vanish status in match player
    player.setVanished(vanish);

    // Reset visibility to hide/show player
    player.resetVisibility();

    // Broadcast join/quit message
    if (!quiet) {
      PGMListener.announceJoinOrLeave(player, !vanish, false);
    }

    match.callEvent(new PlayerVanishEvent(player, vanish));

    return isVanished(player.getId());
  }

  private void addVanished(MatchPlayer player) {
    if (!isVanished(player.getId())) {
      this.vanishedPlayers.add(player.getId());
      player.getBukkit().setMetadata(VANISH_KEY, VANISH_VALUE);
    }
  }

  private void removeVanished(MatchPlayer player) {
    this.vanishedPlayers.remove(player.getId());
    player.getBukkit().removeMetadata(VANISH_KEY, VANISH_VALUE.getOwningPlugin());
  }

  /* Events */
  private final Cache<UUID, String> loginSubdomains =
      CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.SECONDS).build();
  private final List<UUID> tempVanish =
      Lists.newArrayList(); // List of online UUIDs who joined via "vanish" subdomain

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPreJoin(PlayerLoginEvent event) {
    Player player = event.getPlayer();
    loginSubdomains.invalidate(player.getUniqueId());
    if (player.hasPermission(Permissions.VANISH)
        && !isVanished(player.getUniqueId())
        && isVanishSubdomain(event.getHostname())) {
      loginSubdomains.put(player.getUniqueId(), event.getHostname());
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onJoin(PlayerJoinEvent event) {
    MatchPlayer player = getMatch().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.getParty() instanceof Competitor) return; // Do not vanish players on a team
    if (!player.getBukkit().hasPermission(Permissions.VANISH)) return; // No perms
    if (checkVanishSubdomain(player)) return; // Login via vanish.<ip> will force vanish

    if (nicks.isNicked(player.getId()) || nicks.isAutoNicked(player.getId())) {
      if (isVanished(player.getId())) {
        removeVanished(player); // Unvanish nicked players
      }
      return; // No vanish for nick
    }

    if (isVanished(player.getId())) { // Player is already vanished
      player.setVanished(true);
      return;
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onQuit(PlayerQuitEvent event) {
    MatchPlayer player = getMatch().getPlayer(event.getPlayer());
    // If player is vanished & joined via "vanish" subdomain. Remove vanish status on quit
    if (isVanished(player.getId()) && tempVanish.contains(player.getId())) {
      setVanished(player, false, true);
      // Temporary vanish status is removed before quit,
      // so prevent regular quit msg and forces a staff only broadcast
      event.setQuitMessage(null);
      PGMListener.announceJoinOrLeave(player, false, true, true);
    }
  }

  @EventHandler
  public void onUnvanish(PlayerVanishEvent event) {
    // If player joined via "vanish" subdomain, but unvanishes while online
    // stop tracking them for auto-vanish removal
    if (!event.isVanished() && tempVanish.contains(event.getPlayer().getId())) {
      tempVanish.remove(event.getPlayer().getId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void checkMatchPlayer(MatchPlayerAddEvent event) {
    MatchPlayer player = event.getPlayer();
    // Player is joining to a team so broadcast join
    if (event.getInitialParty() instanceof Competitor) {
      setVanished(player, false, false);
    }

    player.setVanished(isVanished(player.getId()));
  }

  private boolean checkVanishSubdomain(MatchPlayer player) {
    if (player
        .getBukkit()
        .hasPermission(Permissions.VANISH)) { // Player is not vanished, but has permission to

      // Automatic vanish if player logs in via a "vanish" subdomain
      String domain = loginSubdomains.getIfPresent(player.getId());
      if (domain != null) {
        loginSubdomains.invalidate(player.getId());
        tempVanish.add(player.getId());
        setVanished(player, true, true);

        if (nicks.isNicked(player.getId())) {
          nicks.removeOnlineNick(player.getId());
          player.sendMessage(text("You have forcefully vanished (Nick removed)"));
        }
        return true;
      }
    }

    return false;
  }

  private boolean isVanishSubdomain(String address) {
    return address.startsWith("vanish.");
  }

  private void sendHotbarVanish(MatchPlayer player, boolean flashColor) {
    Component warning = text(" \u26a0 ", flashColor ? NamedTextColor.YELLOW : NamedTextColor.GOLD);
    Component vanish = translatable("vanish.hotbar", NamedTextColor.RED, TextDecoration.BOLD);
    Component message = text().append(warning).append(vanish).append(warning).build();
    player.sendActionBar(message);
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet(); // Vanish command is in PGM
  }
}