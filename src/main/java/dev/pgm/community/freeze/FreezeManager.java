package dev.pgm.community.freeze;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.Community;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.chat.Sound;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.types.PlayerComponent;

/** FreezeManager - Handles freezing of players */
public class FreezeManager {

  private final Sound FREEZE_SOUND = new Sound("mob.enderdragon.growl", 1f, 1f);
  private final Sound THAW_SOUND = new Sound("mob.enderdragon.growl", 1f, 2f);

  private final OnlinePlayerMapAdapter<Player> frozenPlayers;
  private final Cache<UUID, String> offlineFrozenCache =
      CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();

  public FreezeManager() {
    this.frozenPlayers = new OnlinePlayerMapAdapter<Player>(Community.get());
  }

  public void cachePlayer(Player player) {
    this.offlineFrozenCache.put(player.getUniqueId(), player.getName());
  }

  public void removeCachedPlayer(UUID uuid) {
    this.offlineFrozenCache.invalidate(uuid);
  }

  public boolean isCached(UUID uuid) {
    return this.offlineFrozenCache.getIfPresent(uuid) != null;
  }

  public String getOfflineFrozenNames() {
    return offlineFrozenCache.asMap().values().stream()
        .map(name -> ChatColor.DARK_AQUA + name)
        .collect(Collectors.joining(ChatColor.GRAY + ", "));
  }

  public int getOfflineCount() {
    return Math.toIntExact(offlineFrozenCache.size());
  }

  public int getOnlineCount() {
    return frozenPlayers.size();
  }

  public List<Player> getFrozenPlayers() {
    return frozenPlayers.values().stream().collect(Collectors.toList());
  }

  public boolean isFrozen(Entity player) {
    return player instanceof Player && frozenPlayers.containsKey(player);
  }

  public void setFrozen(
      CommandAudience freezer, Player freezee, boolean frozen, boolean silent, boolean pgm) {
    // Use PGM freeze if integration & plugin loaded
    if (pgm) {
      Match match = PGM.get().getMatchManager().getMatch(freezee);
      if (match != null) {
        MatchPlayer matchPlayer = match.getPlayer(freezee);
        if (matchPlayer != null) {
          matchPlayer.setFrozen(frozen);
        }
      }
    }

    if (frozen) {
      freeze(freezee, freezer.getStyledName(), silent);
    } else {
      thaw(freezee, freezer.getStyledName(), silent);
    }
  }

  private void freeze(Player freezee, Component senderName, boolean silent) {
    frozenPlayers.put(freezee, freezee);

    removeEntities(freezee.getLocation(), 10);

    Component freeze = TranslatableComponent.of("moderation.freeze.frozen");
    Component by = TranslatableComponent.of("misc.by", senderName);

    TextComponent.Builder freezeTitle = TextComponent.builder();
    freezeTitle.append(freeze);
    if (!silent) {
      freezeTitle.append(" ").append(by);
    }
    Component title = freezeTitle.color(TextColor.RED).build();
    if (isLegacy(freezee)) {
      Audience.get(freezee).sendWarning(title);
    } else {
      Audience.get(freezee).showTitle(TextComponent.empty(), title, 5, 9999, 5);
    }
    Audience.get(freezee).playSound(FREEZE_SOUND);

    BroadcastUtils.sendAdminChatMessage(createInteractiveBroadcast(senderName, freezee, true));
  }

  private void thaw(Player freezee, Component senderName, boolean silent) {
    frozenPlayers.remove(freezee);

    Component thawed = TranslatableComponent.of("moderation.freeze.unfrozen");
    Component by = TranslatableComponent.of("misc.by", senderName);

    TextComponent.Builder thawedTitle = TextComponent.builder().append(thawed);
    if (!silent) {
      thawedTitle.append(" ").append(by);
    }

    freezee.resetTitle();
    Audience.get(freezee).playSound(THAW_SOUND);
    Audience.get(freezee).sendMessage(thawedTitle.color(TextColor.GREEN).build());

    BroadcastUtils.sendAdminChatMessage(createInteractiveBroadcast(senderName, freezee, false));
  }

  private Component createInteractiveBroadcast(
      Component senderName, Player freezee, boolean frozen) {
    return TextComponent.builder()
        .append(
            TranslatableComponent.of(
                String.format("moderation.freeze.broadcast.%s", frozen ? "frozen" : "thaw"),
                TextColor.GRAY,
                senderName,
                PlayerComponent.of(freezee, NameStyle.CONCISE)))
        .hoverEvent(
            HoverEvent.showText(
                TranslatableComponent.of("moderation.freeze.broadcast.hover", TextColor.GRAY)))
        .clickEvent(ClickEvent.runCommand("/f " + freezee.getName()))
        .build();
  }

  // Borrowed from WorldEdit
  private void removeEntities(Location origin, double radius) {
    if (radius <= 0) return;

    double radiusSq = radius * radius;
    for (Entity ent : origin.getWorld().getEntities()) {
      if (origin.distanceSquared(ent.getLocation()) > radiusSq) continue;

      if (ent instanceof TNTPrimed) {
        ent.remove();
      }
    }
  }

  private boolean isLegacy(Player player) {
    return ViaUtils.enabled() && ViaUtils.getProtocolVersion(player) <= ViaUtils.VERSION_1_7;
  }
}
