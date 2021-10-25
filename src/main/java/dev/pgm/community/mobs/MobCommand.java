package dev.pgm.community.mobs;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
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
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.PlayerComponent;
import tc.oc.pgm.util.text.TextFormatter;

@CommandAlias("mobs")
@Description("Manage mob spawning")
@CommandPermission(CommunityPermissions.MOB_SPAWN)
public class MobCommand extends CommunityCommand {

  @Dependency private MobFeature mobs;

  @Default
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

  @CommandAlias("mob")
  @Subcommand("spawn")
  @Description("Spawn a mob that attack nearby targets")
  @Syntax("[mob] [amount] [canDie (true/false)]")
  @CommandCompletion("@mobs * @range:100 true|false")
  public void mob(
      CommandAudience audience,
      Player sender,
      EntityType type,
      @Default("1") int amount,
      @Default("true") boolean canDie) {
    if (!type.isAlive() || !type.isSpawnable()) {
      audience.sendWarning(text("That entity type can't be spawned!"));
      return;
    }
    mobs.spawn(sender, type, amount, canDie);
    audience.sendMessage(
        text()
            .append(text("Spawned "))
            .append(text(amount, NamedTextColor.YELLOW))
            .append(text(" "))
            .append(text(WordUtils.capitalize(type.toString().toLowerCase()), NamedTextColor.GREEN))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("clear")
  @Description("Remove your spawned mobs from the world")
  public void clear(CommandAudience audience, Player sender) {
    int mobsRemoved = mobs.remove(sender);
    audience.sendMessage(
        text()
            .append(text("Removed "))
            .append(text(mobsRemoved, NamedTextColor.RED))
            .append(text(" mob" + (mobsRemoved != 1 ? "s" : "")))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("tphere|tph")
  @Description("Bring all of your spawned mobs to your location")
  public void tphere(CommandAudience audience, Player sender) {
    int mobsTeleported = mobs.tphere(sender);
    audience.sendMessage(
        text()
            .append(text("Teleported "))
            .append(text(mobsTeleported, NamedTextColor.GREEN))
            .append(text(" mob" + (mobsTeleported != 1 ? "s" : "") + " to your location"))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("heal")
  @Description("Replenish all spawned mobs health to their max value")
  public void heal(CommandAudience audience, Player sender) {
    int mobsHealed = mobs.heal(sender);
    audience.sendMessage(
        text()
            .append(text("Healed "))
            .append(text(mobsHealed, NamedTextColor.GREEN))
            .append(text(" mob" + (mobsHealed != 1 ? "s" : "")))
            .color(NamedTextColor.GRAY)
            .build());
  }

  @Subcommand("attack")
  @CommandCompletion("@players")
  @Description(
      "Toggle whether hostile targeting is enabled. When provided a player will set the target")
  @Syntax("[target] - Provide no target to toggle hostile targeting")
  public void attack(
      CommandAudience audience, Player sender, @Optional @Flags("other") Player target) {
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

  @Subcommand("follow")
  @Description("Toggle whether your spawned mobs are in follow mode or not")
  public void follow(CommandAudience audience, Player sender) {
    boolean following = mobs.toggleFollow(sender);
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

  @Subcommand("speed")
  @Description("Set the global speed at which followed mobs travel towards their target")
  public void speed(CommandAudience audience, Player sender, float speed) {
    mobs.setSpeed(speed);
    audience.sendMessage(
        text()
            .append(
                text("Follow speed has been set to ", NamedTextColor.GRAY)
                    .append(text(speed, NamedTextColor.GREEN)))
            .build());
  }
}
