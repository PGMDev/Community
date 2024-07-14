package dev.pgm.community.requests.commands.supervotes;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.requests.RequestProfile;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.requests.supervotes.SuperVoteComponents;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Argument;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Command;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.CommandDescription;
import tc.oc.pgm.lib.org.incendo.cloud.annotations.Permission;
import tc.oc.pgm.util.named.NameStyle;

@Command("supervotes")
public class SuperVoteAdminCommands extends CommunityCommand {

  private final UsersFeature users;
  private final RequestFeature requests;

  public SuperVoteAdminCommands() {
    this.users = Community.get().getFeatures().getUsers();
    this.requests = Community.get().getFeatures().getRequests();
  }

  @Command("[target]")
  @CommandDescription("Check your super vote balance")
  public void balance(CommandAudience audience, @Argument(value = "target") TargetPlayer target) {
    if (target != null && audience.hasPermission(CommunityPermissions.SUPER_VOTE_BALANCE)) {
      getTarget(target.getIdentifier(), users).thenAcceptAsync(uuid -> {
        if (uuid.isPresent()) {
          RequestProfile profile = requests.getRequestProfile(uuid.get()).join();
          if (profile == null) {
            audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
            return;
          }

          Component name = users.renderUsername(uuid, NameStyle.FANCY).join();
          audience.sendMessage(
              SuperVoteComponents.getSuperVoteBalance(name, profile.getSuperVotes()));
        } else {
          audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
        }
      });
    } else if (audience.isPlayer()) {
      Player player = audience.getPlayer();
      requests.getRequestProfile(player.getUniqueId()).thenAcceptAsync(profile -> {
        int superVotes = profile.getSuperVotes();
        audience.sendMessage(SuperVoteComponents.getSuperVoteBalance(superVotes));
      });
    } else {
      audience.sendWarning(text("Please provide a username to check the super vote balance of."));
    }
  }

  @Command("give <target> <amount>")
  @CommandDescription("Give the target player super votes")
  @Permission(CommunityPermissions.ADMIN)
  public void give(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument("amount") int amount) {
    getTarget(target.getIdentifier(), users).thenAcceptAsync(targetId -> {
      if (targetId.isPresent()) {
        RequestProfile profile = requests.getRequestProfile(targetId.get()).join();
        if (profile != null) {
          int total = profile.giveSuperVotes(amount);
          requests.update(profile);
          audience.sendMessage(text()
              .append(MessageUtils.VOTE)
              .append(space())
              .append(
                  users.renderUsername(profile.getPlayerId(), NameStyle.FANCY).join())
              .append(text(" now has "))
              .append(text(total, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
              .append(text(" super vote" + (total != 1 ? "s" : "")))
              .color(NamedTextColor.GRAY)
              .build());
          return;
        }
      }
      audience.sendWarning(formatNotFoundComponent(target.getIdentifier()));
    });
  }
}
