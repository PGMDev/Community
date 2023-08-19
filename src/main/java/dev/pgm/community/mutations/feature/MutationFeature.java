package dev.pgm.community.mutations.feature;

import static dev.pgm.community.utils.PGMUtils.isPGMEnabled;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import com.google.common.collect.Sets;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.FeatureBase;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationConfig;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.menu.MutationOptionsMenu;
import dev.pgm.community.mutations.menu.MutationToggleMenu;
import dev.pgm.community.mutations.types.arrows.ArrowTrailMutation;
import dev.pgm.community.mutations.types.arrows.EnderpearlMutation;
import dev.pgm.community.mutations.types.arrows.FireballBowMutation;
import dev.pgm.community.mutations.types.arrows.TNTBowMutation;
import dev.pgm.community.mutations.types.arrows.WebSlingersMutation;
import dev.pgm.community.mutations.types.gameplay.BlitzMutation;
import dev.pgm.community.mutations.types.gameplay.GhostMutation;
import dev.pgm.community.mutations.types.gameplay.RageMutation;
import dev.pgm.community.mutations.types.items.BreadMutation;
import dev.pgm.community.mutations.types.items.CannonSuppliesMutation;
import dev.pgm.community.mutations.types.items.ExplosionMutation;
import dev.pgm.community.mutations.types.items.FireworkMutation;
import dev.pgm.community.mutations.types.items.GrapplingHookMutation;
import dev.pgm.community.mutations.types.items.PotionMutation;
import dev.pgm.community.mutations.types.mechanics.BlindMutation;
import dev.pgm.community.mutations.types.mechanics.DoubleJumpMutation;
import dev.pgm.community.mutations.types.mechanics.FlyMutation;
import dev.pgm.community.mutations.types.mechanics.FriendlyFireMutation;
import dev.pgm.community.mutations.types.mechanics.HealthMutation;
import dev.pgm.community.mutations.types.mechanics.KnockbackMutation;
import dev.pgm.community.mutations.types.mechanics.MobMutation;
import dev.pgm.community.mutations.types.world.BlockDecayMutation;
import dev.pgm.community.mutations.types.world.StormMutation;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.Sounds;
import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.util.text.TextFormatter;

public class MutationFeature extends FeatureBase {

  private Set<Mutation> mutations;

  private final InventoryManager inventory;

  public MutationFeature(Configuration config, Logger logger, InventoryManager inventory) {
    super(new MutationConfig(config), logger, "Mutations (PGM)");
    this.inventory = inventory;
    this.mutations = Sets.newHashSet();

    if (getConfig().isEnabled() && isPGMEnabled()) {
      enable();
    }
  }

  public MutationConfig getMutationConfig() {
    return (MutationConfig) getConfig();
  }

  public boolean addMutation(CommandAudience sender, MutationType type, boolean broadcast) {
    if (!hasMutation(type)) {
      Mutation newMutation = getNewMutation(type);

      if (newMutation.canEnable(mutations)) {
        mutations.add(newMutation);

        if (getMatch().isRunning()) enableMutation(newMutation, broadcast);

        BroadcastUtils.sendAdminChatMessage(
            text()
                .append(sender.getStyledName())
                .append(text(" has enabled the "))
                .append(newMutation.getName())
                .append(text(" mutation"))
                .color(NamedTextColor.GRAY)
                .build(),
            CommunityPermissions.MUTATION);
        return true;
      } else {
        sender.sendWarning(
            text(type.getDisplayName()).append(text(" can not be enabled for this match")));
      }
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
              .build(),
          CommunityPermissions.MUTATION);

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
            mutation.getName(), text(" mutation has been disabled", NamedTextColor.GRAY), 3);
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
      case BREAD:
        return new BreadMutation(getMatch());
      case BLIND:
        return new BlindMutation(getMatch());
      case HEALTH:
        return new HealthMutation(getMatch());
      case GHOST:
        return new GhostMutation(getMatch());
      case STORM:
        return new StormMutation(getMatch());
      case FRIENDLY:
        return new FriendlyFireMutation(getMatch());
      case ARROW_TRAIL:
        return new ArrowTrailMutation(getMatch());
      case ENDERPEARL:
        return new EnderpearlMutation(getMatch());
      case BLOCK_DECAY:
        return new BlockDecayMutation(getMatch());
      case KNOCKBACK:
        return new KnockbackMutation(getMatch());
      case WEB_SLINGERS:
        return new WebSlingersMutation(getMatch());
      case MOBS:
        return new MobMutation(getMatch());
      case TNT_BOW:
        return new TNTBowMutation(getMatch());
      case FIREBALL_BOW:
        return new FireballBowMutation(getMatch());
      case CANNON_SUPPLIES:
        return new CannonSuppliesMutation(getMatch());
      case GRAPPLING_HOOK:
        return new GrapplingHookMutation(getMatch());
      default:
        logger.warning(type.getDisplayName() + " has not been implemented yet");
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
            .append(
                TextFormatter.horizontalLine(
                    NamedTextColor.DARK_GREEN, TextFormatter.MAX_CHAT_WIDTH))
            .append(newline())
            .append(TextFormatter.list(names, NamedTextColor.GRAY))
            .append(text(" mutation" + (single ? "" : "s")))
            .append(text(single ? " has " : " have "))
            .append(text("been enabled"))
            .append(newline())
            .append(
                TextFormatter.horizontalLine(
                    NamedTextColor.DARK_GREEN, TextFormatter.MAX_CHAT_WIDTH))
            .color(NamedTextColor.GRAY)
            .build();

    if (!mutations.isEmpty()) {
      BroadcastUtils.sendGlobalMessage(broadcast, Sounds.ALERT);
    }
  }

  @EventHandler
  public void onMatchEnd(MatchFinishEvent event) {
    mutations.forEach(Mutation::disable);
    mutations.clear();
  }

  public SmartInventory getMenu() {
    return SmartInventory.builder()
        .title(ChatColor.GREEN + "Toggle Mutations")
        .manager(inventory)
        .provider(new MutationToggleMenu(this))
        .size(4, 9)
        .build();
  }

  public SmartInventory getOptionMenu() {
    return SmartInventory.builder()
        .title(ChatColor.GOLD + "Mutation Options")
        .manager(inventory)
        .provider(new MutationOptionsMenu(this))
        .parent(getMenu())
        .size(4, 9)
        .build();
  }
}
