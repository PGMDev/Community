package dev.pgm.community.mobs;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

@CommandMethod("mobs|mob")
public class MobCommand extends CommunityCommand {

  private final MobFeature mobs;

  public MobCommand() {
    this.mobs = Community.get().getFeatures().getMobs();
  }

  @CommandMethod("")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void viewMobs(CommandAudience audience, Player sender) {
    List<LivingEntity> ownedMobs = mobs.getOwnedMobs(sender);
    if (!ownedMobs.isEmpty()) {
      audience.sendMessage(
          TextFormatter.horizontalLineHeading(
              sender,
              text("Mobs", NamedTextColor.GREEN, TextDecoration.BOLD),
              NamedTextColor.DARK_GREEN));

      sendEntityTotals(audience, ownedMobs);
    } else {
      audience.sendWarning(text("You have not spawned any mobs! Use /mob to get started"));
      return;
    }

    Component mobActions =
        text()
            .append(
                button(
                    "Teleport",
                    NamedTextColor.GREEN,
                    "/mobs tphere",
                    "Click to teleport mobs here"))
            .append(space())
            .append(button("Clear", NamedTextColor.RED, "/mobs clear", "Click to remove all mobs"))
            .append(space())
            .append(button("Heal", NamedTextColor.LIGHT_PURPLE, "/mobs heal", "Click to heal mobs"))
            .build();

    Component mobModes =
        text()
            .append(
                button(
                    "Follow",
                    NamedTextColor.DARK_GREEN,
                    "/mobs follow",
                    "Click to toggle follow mode"))
            .append(space())
            .append(
                button(
                    "Attack",
                    NamedTextColor.DARK_RED,
                    "/mobs attack",
                    "Click to toggle mob targeting"))
            .build();
    audience.sendMessage(empty());
    audience.sendMessage(text("Actions:", NamedTextColor.GRAY));
    audience.sendMessage(mobActions);
    audience.sendMessage(text("Modes:", NamedTextColor.GRAY));
    audience.sendMessage(mobModes);
  }

  private void sendEntityTotals(CommandAudience audience, List<LivingEntity> mobs) {
    long total = 0;

    for (EntityType type : EntityType.values()) {
      long typeTotal = mobs.stream().filter(le -> le.getType().equals(type)).count();
      if (typeTotal > 0) {
        audience.sendMessage(
            text()
                .append(
                    text(
                        WordUtils.capitalizeFully(type.toString().toLowerCase()),
                        NamedTextColor.DARK_AQUA))
                .append(text(" : ", NamedTextColor.GREEN))
                .append(text(typeTotal, NamedTextColor.YELLOW))
                .build());
      }
      total += typeTotal;
    }
    audience.sendMessage(
        text()
            .append(text("Total mobs spawned", NamedTextColor.GRAY))
            .append(text(" : ", NamedTextColor.GREEN))
            .append(text(total, NamedTextColor.GOLD))
            .build());
  }

  @CommandMethod("spawn <mob> [amount]")
  @CommandDescription("Spawn a mob that attack nearby targets")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void mob(
      CommandAudience audience,
      Player sender,
      @Argument("mob") EntityType type,
      @Argument(value = "amount", defaultValue = "1") int amount,
      @Flag("i") boolean canDie) {
    if (!type.isAlive() || !type.isSpawnable()) {
      audience.sendWarning(text("That entity type can't be spawned!"));
      return;
    }
    mobs.spawn(sender, type, amount, !canDie);
    audience.sendMessage(
        text()
            .append(text("Spawned "))
            .append(text(amount, NamedTextColor.YELLOW))
            .append(text(" "))
            .append(text(WordUtils.capitalize(type.toString().toLowerCase()), NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @CommandMethod("clear")
  @CommandDescription("Remove your spawned mobs from the world")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void clear(CommandAudience audience, Player sender) {
    int mobsRemoved = mobs.remove(audience.getPlayer());
    audience.sendMessage(
        text()
            .append(text("Removed "))
            .append(text(mobsRemoved, NamedTextColor.RED))
            .append(text(" mob" + (mobsRemoved != 1 ? "s" : "")))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @CommandMethod("tphere|tph")
  @CommandDescription("Bring all of your spawned mobs to your location")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void tphere(CommandAudience audience, Player sender) {
    int mobsTeleported = mobs.tphere(audience.getPlayer());
    audience.sendMessage(
        text()
            .append(text("Teleported "))
            .append(text(mobsTeleported, NamedTextColor.GREEN))
            .append(text(" mob" + (mobsTeleported != 1 ? "s" : "") + " to your location"))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @CommandMethod("heal")
  @CommandDescription("Replenish all spawned mobs health to their max value")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void heal(CommandAudience audience, Player sender) {
    int mobsHealed = mobs.heal(audience.getPlayer());
    audience.sendMessage(
        text()
            .append(text("Healed "))
            .append(text(mobsHealed, NamedTextColor.GREEN))
            .append(text(" mob" + (mobsHealed != 1 ? "s" : "")))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @CommandMethod("attack [target]")
  @CommandDescription(
      "Toggle whether hostile targeting is enabled. When provided a player will set the target")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void attack(CommandAudience audience, Player sender, @Argument("target") Player target) {
    if (target != null) {
      mobs.attack(sender, target);
      audience.sendMessage(
          text()
              .append(text("Spawned mobs will now attack ", NamedTextColor.GRAY))
              .append(PlayerComponent.player(target, NameStyle.FANCY))
              .build());
    } else {
      boolean autoEnabled = mobs.toggleAutoAttack(sender);
      audience.sendMessage(
          text()
              .append(
                  text()
                      .append(text("Hostile targeting has been ", NamedTextColor.GRAY))
                      .append(
                          text(
                              autoEnabled ? "Enabled" : "Disabled",
                              autoEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)))
              .build());
    }
  }

  @CommandMethod("follow")
  @CommandDescription("Toggle whether your spawned mobs are in follow mode or not")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void follow(CommandAudience audience, Player sender) {
    boolean following = mobs.toggleFollow(audience.getPlayer());
    audience.sendMessage(
        text()
            .append(
                text()
                    .append(text("Mob following has been ", NamedTextColor.GRAY))
                    .append(
                        text(
                            following ? "Enabled" : "Disabled",
                            following ? NamedTextColor.GREEN : NamedTextColor.RED)))
            .build());
  }

  @CommandMethod("speed <speed>")
  @CommandDescription("Set the global speed at which followed mobs travel towards their target")
  @CommandPermission(CommunityPermissions.MOB_SPAWN)
  public void speed(CommandAudience audience, @Argument("speed") float speed) {
    mobs.setSpeed(speed);
    audience.sendMessage(
        text()
            .append(
                text("Follow speed has been set to ", NamedTextColor.GRAY)
                    .append(text(speed, NamedTextColor.GREEN)))
            .build());
  }
}
