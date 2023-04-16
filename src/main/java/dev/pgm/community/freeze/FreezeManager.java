package dev.pgm.community.freeze;

import static net.kyori.adventure.key.Key.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.title.Title.title;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.bukkit.ViaUtils;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;

/** FreezeManager - Handles freezing of players */
public class FreezeManager {

  private final Sound FREEZE_SOUND =
      sound(key("mob.enderdragon.growl"), Sound.Source.PLAYER, 1f, 1f);
  private final Sound THAW_SOUND = sound(key("mob.enderdragon.growl"), Sound.Source.PLAYER, 1f, 2f);

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

    // Don't allow freezing if player is exempt
    if (freezee.hasPermission(CommunityPermissions.FREEZE_EXEMPT)
        && !freezer.getSender().hasPermission(CommunityPermissions.FREEZE_FORCE)) {
      freezer.sendWarning(
          text()
              .append(PlayerComponent.player(freezee, NameStyle.FANCY))
              .append(text(" can not be frozen"))
              .build());
      return;
    }

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

    Component freeze = translatable("moderation.freeze.frozen");
    Component by = translatable("misc.by", senderName);

    TextComponent.Builder freezeTitle = text().append(freeze);
    if (!silent) {
      freezeTitle.append(space()).append(by);
    }
    freezeTitle.color(NamedTextColor.RED);
    if (isLegacy(freezee)) {
      Audience.get(freezee).sendWarning(freezeTitle.build());
    } else {
      Audience.get(freezee)
          .showTitle(
              title(
                  empty(),
                  freezeTitle.build(),
                  Times.of(Ticks.duration(5), Ticks.duration(9999), Ticks.duration(5))));
    }
    Audience.get(freezee).playSound(FREEZE_SOUND);

    BroadcastUtils.sendAdminChatMessage(
        createInteractiveBroadcast(senderName, freezee, true), CommunityPermissions.FREEZE);
  }

  private void thaw(Player freezee, Component senderName, boolean silent) {
    frozenPlayers.remove(freezee);

    Component thawed = translatable("moderation.freeze.unfrozen");
    Component by = translatable("misc.by", senderName);

    Component thawedTitle = thawed;
    if (!silent) {
      thawedTitle.append(space()).append(by);
    }

    freezee.resetTitle();
    Audience.get(freezee).playSound(THAW_SOUND);
    Audience.get(freezee).sendMessage(thawedTitle.color(NamedTextColor.GREEN));

    BroadcastUtils.sendAdminChatMessage(
        createInteractiveBroadcast(senderName, freezee, false), CommunityPermissions.FREEZE);
  }

  private Component createInteractiveBroadcast(
      Component senderName, Player freezee, boolean frozen) {
    return text()
        .append(
            translatable(
                String.format("moderation.freeze.broadcast.%s", frozen ? "frozen" : "thaw"),
                NamedTextColor.GRAY,
                senderName,
                PlayerComponent.player(freezee, NameStyle.FANCY)))
        .hoverEvent(
            HoverEvent.showText(
                translatable("moderation.freeze.broadcast.hover", NamedTextColor.GRAY)))
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
