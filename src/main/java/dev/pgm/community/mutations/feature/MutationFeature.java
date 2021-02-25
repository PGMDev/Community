package dev.pgm.community.mutations.feature;

import static dev.pgm.community.utils.PGMUtils.isPGMEnabled;
import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.InvalidCommandArgument;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationConfig;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.commands.MutationCommands;
import dev.pgm.community.mutations.types.BlitzMutation;
import dev.pgm.community.mutations.types.DoubleJumpMutation;
import dev.pgm.community.mutations.types.ExplosionMutation;
import dev.pgm.community.mutations.types.FireworkMutation;
import dev.pgm.community.mutations.types.FlyMutation;
import dev.pgm.community.mutations.types.PotionMutation;
import dev.pgm.community.mutations.types.RageMutation;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.util.text.TextFormatter;

public class MutationFeature extends FeatureBase {

  private Set<Mutation> mutations;

  public MutationFeature(Configuration config, Logger logger) {
    super(new MutationConfig(config), logger, "Mutations (PGM)");
    this.mutations = Sets.newHashSet();

    if (getConfig().isEnabled() && isPGMEnabled()) {
      enable();
    }
  }

  public MutationConfig getMutationConfig() {
    return (MutationConfig) getConfig();
  }

  @Override
  public Set<CommunityCommand> getCommands() {
    return getConfig().isEnabled() ? Sets.newHashSet(new MutationCommands()) : Sets.newHashSet();
  }

  public boolean addMutation(CommandAudience sender, MutationType type, boolean broadcast) {
    if (!hasMutation(type)) {
      Mutation newMutation = getNewMutation(type);

      if (newMutation.canEnable()) {
        mutations.add(newMutation);

        if (getMatch().isRunning()) enableMutation(newMutation, broadcast);

        BroadcastUtils.sendAdminChatMessage(
            text()
                .append(sender.getStyledName())
                .append(text(" has enabled the "))
                .append(newMutation.getName())
                .append(text(" mutation"))
                .color(NamedTextColor.GRAY)
                .build());

      } else {
        throw new InvalidCommandArgument(
            type.getDisplayName() + " can not be enabled for this match", false);
      }

      return true;
    }
    return false;
  }

  public boolean removeMutation(CommandAudience sender, MutationType type) {
    Optional<Mutation> mutation = getMutation(type);
    if (mutation.isPresent()) {
      disableMutation(mutation.get(), getMatch().isRunning());

      BroadcastUtils.sendAdminChatMessage(
          text()
              .append(sender.getStyledName())
              .append(text(" has disabled the "))
              .append(mutation.get().getName())
              .append(text(" mutation"))
              .color(NamedTextColor.GRAY)
              .build());

      mutations.remove(mutation.get());
      return true;
    }
    return false;
  }

  public void enableMutation(Mutation mutation, boolean broadcast) {
    if (!mutation.isEnabled()) {
      mutation.enable();
      if (broadcast) {
        BroadcastUtils.sendGlobalTitle(
            mutation.getName(), text(" mutation has been enabled", NamedTextColor.GRAY), 3);
      }
    }
  }

  public void disableMutation(Mutation mutation, boolean broadcast) {
    if (mutation.isEnabled()) {
      mutation.disable();
      if (broadcast) {
        BroadcastUtils.sendGlobalTitle(
            mutation.getName(), text(" has been disabled", NamedTextColor.GRAY), 3);
      }
    }
  }

  public Optional<Mutation> getMutation(MutationType type) {
    return mutations.stream().filter(m -> m.getType().equals(type)).findAny();
  }

  public boolean hasMutation(MutationType type) {
    return getMutation(type).isPresent();
  }

  public Set<Mutation> getMutations() {
    return mutations;
  }

  private Mutation getNewMutation(MutationType type) {
    switch (type) {
      case BLITZ:
        return new BlitzMutation(getMatch());
      case RAGE:
        return new RageMutation(getMatch());
      case EXPLOSION:
        return new ExplosionMutation(getMatch());
      case FLY:
        return new FlyMutation(getMatch());
      case JUMP:
        return new DoubleJumpMutation(getMatch());
      case FIREWORK:
        return new FireworkMutation(getMatch());
      case POTION:
        return new PotionMutation(getMatch());
    }

    return null;
  }

  public Match getMatch() {
    return PGM.get().getMatchManager().getMatches().hasNext()
        ? PGM.get().getMatchManager().getMatches().next()
        : null;
  }

  // Events
  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    mutations.forEach(Mutation::enable);

    Set<Component> names = mutations.stream().map(Mutation::getName).collect(Collectors.toSet());
    boolean single = names.size() == 1;
    Component broadcast =
        text()
            .append(TextFormatter.list(names, NamedTextColor.GRAY))
            .append(text(" mutation" + (single ? "" : "s")))
            .append(text(single ? " has " : " have "))
            .append(text("been enabled"))
            .color(NamedTextColor.GRAY)
            .build();

    if (!mutations.isEmpty()) {
      BroadcastUtils.sendGlobalWarning(broadcast);
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    mutations.forEach(Mutation::disable);
    mutations.clear();
  }
}
