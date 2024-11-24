package dev.pgm.community.squads;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static tc.oc.pgm.util.player.PlayerComponent.player;
import static tc.oc.pgm.util.text.TextException.exception;
import static tc.oc.pgm.util.text.TextFormatter.horizontalLineHeading;

import co.aikar.commands.annotation.CommandPermission;
import com.google.common.collect.ImmutableList;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Flag;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.util.Players;
import tc.oc.pgm.util.PrettyPaginatedComponentResults;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;

@Command("party|squad|p")
public class SquadCommands {

  private final SquadFeature manager;

  public SquadCommands() {
    this.manager = Community.get().getFeatures().getSquads();
  }

  @Command("")
  @CommandDescription("List party members")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void listDefault(MatchPlayer sender) {
    checkEnabled();
    list(sender, false);
  }

  @Command("<player>")
  @CommandDescription("Send a squad invitation")
  @CommandPermission(CommunityPermissions.SQUAD_CREATE)
  public void directInvite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    checkEnabled();
    invite(sender, invited);
  }

  @Command("create")
  @CommandDescription("Create a squad")
  @CommandPermission(CommunityPermissions.SQUAD_CREATE)
  public void create(MatchPlayer sender) {
    checkEnabled();
    manager.createSquad(sender);
    sender.sendMessage(translatable("squad.create.success", NamedTextColor.YELLOW));
  }

  @Command("invite <player>")
  @CommandDescription("Send a squad invitation")
  @CommandPermission(CommunityPermissions.SQUAD_CREATE)
  public void invite(MatchPlayer sender, @Argument("player") MatchPlayer invited) {
    checkEnabled();
    manager.createInvite(invited, sender);

    sender.sendMessage(translatable(
        "squad.invite.sent", NamedTextColor.YELLOW, invited.getName(NameStyle.VERBOSE)));

    String leaderName = Players.getVisibleName(invited.getBukkit(), sender.getBukkit());

    invited.sendMessage(translatable(
            "squad.invite.received",
            NamedTextColor.YELLOW,
            sender.getName(NameStyle.VERBOSE),
            translatable("squad.invite.accept", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/party accept " + leaderName)),
            translatable("squad.invite.deny", NamedTextColor.RED, TextDecoration.BOLD))
        .clickEvent(ClickEvent.runCommand("/party deny " + leaderName)));
  }

  @Command("accept <player>")
  @CommandDescription("Accept a squad invitation")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void accept(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    checkEnabled();
    manager.acceptInvite(sender, leader);

    leader.sendMessage(translatable(
        "squad.accept.leader", NamedTextColor.GREEN, sender.getName(NameStyle.VERBOSE)));
    sender.sendMessage(translatable(
        "squad.accept.invited", NamedTextColor.GREEN, leader.getName(NameStyle.VERBOSE)));
  }

  @Command("deny <player>")
  @CommandDescription("Deny a squad invitation")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void deny(MatchPlayer sender, @Argument("player") MatchPlayer leader) {
    checkEnabled();
    manager.expireInvite(sender, leader);

    leader.sendMessage(
        translatable("squad.deny.leader", NamedTextColor.RED, sender.getName(NameStyle.VERBOSE)));
    sender.sendMessage(
        translatable("squad.deny.invited", NamedTextColor.RED, leader.getName(NameStyle.VERBOSE)));
  }

  @Command("leave")
  @CommandDescription("Leave your current party")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void leave(MatchPlayer sender) {
    checkEnabled();
    manager.leaveSquad(sender);
    sender.sendMessage(translatable("squad.leave.success", NamedTextColor.YELLOW));
  }

  @Command("list")
  @CommandDescription("List party members")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void list(MatchPlayer sender, @Flag(value = "all", aliases = "a") boolean all) {
    checkEnabled();

    if (all && sender.getBukkit().hasPermission(CommunityPermissions.SQUAD_ADMIN)) {
      Component header = horizontalLineHeading(
          sender.getBukkit(), translatable("squad.list.all"), NamedTextColor.BLUE);

      new PrettyPaginatedComponentResults<Squad>(header, manager.getSquads().size()) {
        @Override
        public Component format(Squad squad, int index) {
          return text()
              .append(text(index + 1))
              .append(text(". "))
              .append(player(squad.getLeader(), NameStyle.VERBOSE))
              .append(text(": "))
              .append(TextFormatter.list(
                  squad.getPlayers().stream()
                      .skip(1)
                      .map(uuid -> player(uuid, NameStyle.VERBOSE))
                      .toList(),
                  NamedTextColor.GRAY))
              .build();
        }
      }.display(sender, ImmutableList.copyOf(manager.getSquads()), 1);

      return;
    }

    Squad squad = manager.getSquadByPlayer(sender);
    if (squad == null) throw exception("squad.err.memberOnly");

    boolean isLeader = Objects.equals(sender.getId(), squad.getLeader());

    Component header = horizontalLineHeading(
        sender.getBukkit(),
        translatable("squad.list.header", player(squad.getLeader(), NameStyle.VERBOSE)),
        NamedTextColor.BLUE);

    new PrettyPaginatedComponentResults<UUID>(header, squad.totalSize()) {
      @Override
      public Component format(UUID player, int index) {
        TextComponent.Builder builder = text()
            .append(text(index + 1))
            .append(text(". "))
            .append(player(player, NameStyle.VERBOSE));
        if (squad.getLeader().equals(player)) {
          builder
              .append(text(" "))
              .append(
                  translatable("squad.list.leader", NamedTextColor.GRAY, TextDecoration.ITALIC));
        } else {
          if (index >= squad.size()) {
            builder
                .append(text(" "))
                .append(
                    translatable("squad.list.pending", NamedTextColor.GRAY, TextDecoration.ITALIC));
          }
          if (isLeader) {
            builder
                .append(text(" "))
                .append(text("\u2715", NamedTextColor.DARK_RED)
                    .hoverEvent(
                        showText(translatable("squad.list.removeHover", NamedTextColor.RED)))
                    .clickEvent(runCommand("/party kick " + player)));
          }
        }
        return builder.build();
      }
    }.display(sender, ImmutableList.copyOf(squad.getAllPlayers()), 1);
  }

  @Command("chat [message]")
  @CommandDescription("Sends a message to your party")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void chat(
      CommandContext<CommandSender> context,
      MatchPlayer sender,
      @Argument(value = "message", suggestions = "players") String message) {
    checkEnabled();
    if (message == null) {
      PGM.get().getChatManager().setChannel(sender, SquadChannel.INSTANCE);
    } else {
      PGM.get().getChatManager().process(SquadChannel.INSTANCE, sender, context);
    }
  }

  @Command("kick <player>")
  @CommandDescription("Kick a player from your party")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void kick(MatchPlayer sender, @Argument("player") OfflinePlayer player) {
    checkEnabled();
    MatchPlayer target = PGM.get().getMatchManager().getPlayer(player.getUniqueId());
    manager.kickPlayer(target, player.getUniqueId(), sender);
    sender.sendMessage(translatable(
        "squad.kicked.leader",
        NamedTextColor.YELLOW,
        player(player.getUniqueId(), NameStyle.VERBOSE)));
    if (target != null)
      target.sendMessage(translatable("squad.kicked.player", NamedTextColor.RED, sender.getName()));
  }

  @Command("disband")
  @CommandDescription("Disband your current party")
  @CommandPermission(CommunityPermissions.SQUAD)
  public void disband(MatchPlayer sender) {
    checkEnabled();
    manager.disband(sender);
    sender.sendMessage(translatable("squad.disband.success", NamedTextColor.YELLOW));
  }

  private void checkEnabled() {
    if (!manager.isEnabled()) {
      throw exception("squad.err.notEnabled");
    }
  }
}
