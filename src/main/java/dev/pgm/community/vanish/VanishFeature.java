package dev.pgm.community.vanish;

import com.google.common.collect.Sets;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.utils.PGMUtils;
import dev.pgm.community.vanish.commands.VanishCommand;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

public class VanishFeature extends FeatureBase {

  private final Set<UUID> vanishedPlayers;
  private final NickFeature nicks;

  private @Nullable PGMVanishIntegration integration;

  public VanishFeature(Configuration config, Logger logger, NickFeature nicks) {
    super(new VanishConfig(config), logger, "Vanish");

    this.vanishedPlayers = Sets.newHashSet();
    this.nicks = nicks;

    if (getConfig().isEnabled()) {
      enable();
    }
  }

  public VanishConfig getVanishConfig() {
    return (VanishConfig) getConfig();
  }

  public NickFeature getNicks() {
    return nicks;
  }

  @Override
  public void enable() {
    super.enable();
    integrate();
  }

  private void integrate() {
    if (isPGMEnabled()) {
      this.integration = new PGMVanishIntegration(this);
      Community.get().registerListener(integration);
    }
  }

  @Override
  public void disable() {
    if (integration != null) {
      integration.disable();
    }
  }

  public Collection<Player> getVanished() {
    return Bukkit.getOnlinePlayers().stream().filter(this::isVanished).collect(Collectors.toList());
  }

  public boolean isVanished(UUID playerId) {
    return vanishedPlayers.contains(playerId);
  }

  public boolean isVanished(Player player) {
    return isVanished(player.getUniqueId());
  }

  public Set<UUID> getVanishedPlayers() {
    return vanishedPlayers;
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return Sets.newHashSet(new VanishCommand());
  }

  private boolean isPGMEnabled() {
    return PGMUtils.isPGMEnabled() && getVanishConfig().isIntegrationEnabled();
  }
}
