package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentFormats;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.NameUtils;
import dev.pgm.community.utils.PaginatedComponentResults;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Flag;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.player.PlayerComponent;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;

public class PunishmentCommand extends CommunityCommand {

  public static final Duration DEFAULT_TEMPBAN_LENGTH = Duration.ofDays(7); // TODO: Maybe config?

  private final ModerationFeature moderation;
  private final UsersFeature usernames;

  public PunishmentCommand() {
    this.moderation = Community.get().getFeatures().getModeration();
    this.usernames = Community.get().getFeatures().getUsers();
  }

  @CommandMethod("punishmenthistory|ph [page]")
  @CommandDescription("View a list of recent punishments")
  @CommandPermission(CommunityPermissions.PUNISH)
  public void viewRecentPunishments(
      CommandAudience audience,
      @Argument(value = "page", defaultValue = "1") int page,
      @Flag(value = "time", aliases = "l") Duration length) {
    moderation
        .getRecentPunishments(length != null ? length : Duration.ofHours(1))
        .thenAcceptAsync(
            punishments -> {
              sendPunishmentHistory(audience, null, punishments, page);
            });
  }

  @CommandMethod("repeatpunishment|rp <target>")
  @CommandDescription("Repeat the last punishment you performed for another player")
  @CommandPermission(CommunityPermissions.PUNISH)
  public void repeatPunishment(CommandAudience audience, @Argument("target") Player target) {
    audience
        .getId()
        .ifPresent(
            id -> {
              Optional<Punishment> last = moderation.getLastPunishment(id);
              if (last.isPresent()) {
                Punishment lastPunishment = last.get();
                PunishmentType type = lastPunishment.getType();
                String reason = lastPunishment.getReason();
                Duration length = ExpirablePunishment.getDuration(lastPunishment);

                if (target != null) {
                  moderation.punish(
                      type,
                      target.getUniqueId(),
                      audience,
                      reason,
                      length,
                      true,
                      isDisguised(audience));
                } else {
                  // No target supplied, show last punishment
                  PunishmentFormats.formatBroadcast(
                          lastPunishment, null, moderation.getStaffFormat(), usernames)
                      .thenAcceptAsync(
                          lpm -> {
                            Component lastPunishMsg =
                                text("Last punishment: ", NamedTextColor.GRAY).append(lpm);
                            audience.sendMessage(lastPunishMsg);
                          });
                }
              } else {
                audience.sendMessage(
                    text(
                        "You have not issued any recent punishments",
                        NamedTextColor.RED)); // TODO: Translate
              }
            });
  }

  @CommandMethod("unban|pardon|forgive <target>")
  @CommandDescription("Pardon all active punishments for a player")
  @CommandPermission(CommunityPermissions.UNBAN)
  public void unbanPlayer(CommandAudience audience, @Argument("target") TargetPlayer target) {
    moderation
        .isBanned(target.getIdentifier())
        .thenAcceptAsync(
            isBanned -> {
              if (isBanned) {
                moderation
                    .pardon(target.getIdentifier(), audience.getId())
                    .thenAcceptAsync(
                        pardon -> {
                          if (!pardon) {
                            audience.sendWarning(
                                text(target.getIdentifier(), NamedTextColor.DARK_AQUA)
                                    .append(text(" could not be ", NamedTextColor.GRAY))
                                    .append(text("unbanned"))
                                    .color(NamedTextColor.RED));
                          } else {
                            BroadcastUtils.sendAdminChatMessage(
                                text(target.getIdentifier(), NamedTextColor.DARK_AQUA)
                                    .append(text(" was unbanned by ", NamedTextColor.GRAY))
                                    .append(audience.getStyledName()),
                                Sounds.PUNISHMENT_PARDON,
                                CommunityPermissions.UNBAN);
                          }
                          // TODO: translate
                        });
              } else {
                audience.sendWarning(
                    text(target.getIdentifier(), NamedTextColor.AQUA)
                        .append(text(" has no active bans", NamedTextColor.GRAY)));
              }
            });
  }

  @CommandMethod("record|infractions|mypunishments [page]")
  @CommandDescription("View your own punishment history")
  @CommandPermission(CommunityPermissions.LOOKUP)
  public void viewOwnPunishmentHistory(
      CommandAudience audience,
      Player player,
      @Argument(value = "page", defaultValue = "1") int page) {
    viewPunishmentHistory(audience, new TargetPlayer(player), page);
  }

  @CommandMethod("lookup|l <target> [page]")
  @CommandDescription("View infraction history of a player")
  @CommandPermission(CommunityPermissions.LOOKUP_OTHERS)
  public void viewPunishmentHistory(
      CommandAudience audience,
      @Argument("target") TargetPlayer target,
      @Argument(value = "page", defaultValue = "1") int page) {
    moderation
        .query(target.getIdentifier())
        .thenAcceptAsync(
            punishments ->
                sendPunishmentHistory(audience, target.getIdentifier(), punishments, page));
  }

  public void sendPunishmentHistory(
      CommandAudience audience, String target, Collection<Punishment> punishmentData, int page) {
    Component headerResultCount = text(Long.toString(punishmentData.size()), NamedTextColor.RED);

    int perPage = 7;
    int pages = (punishmentData.size() + perPage - 1) / perPage;
    page = Math.max(1, Math.min(page, pages));

    Component pageNum =
        translatable(
            "command.simplePageHeader",
            NamedTextColor.GRAY,
            text(Integer.toString(page), NamedTextColor.RED),
            text(Integer.toString(pages), NamedTextColor.RED));

    Component targetName = empty();
    if (target != null) {
      UUID targetID = (!NameUtils.isMinecraftName(target) ? UUID.fromString(target) : null);
      if (targetID != null) {
        targetName = PlayerComponent.player(targetID, NameStyle.FANCY);
      } else {
        targetName = PlayerComponent.player(null, target, NameStyle.FANCY);
      }
    }

    Component header =
        translatable("moderation.records.history", NamedTextColor.GRAY)
            .append(
                target != null ? text(" - ", NamedTextColor.DARK_GRAY).append(targetName) : empty())
            .append(text(" (").append(headerResultCount).append(text(") » ")).append(pageNum))
            .color(NamedTextColor.GRAY);

    Component formattedHeader =
        TextFormatter.horizontalLineHeading(audience.getSender(), header, NamedTextColor.DARK_GRAY);
    new PaginatedComponentResults<Punishment>(formattedHeader, perPage) {
      @Override
      public Component format(Punishment data, int index) {
        TextComponent.Builder builder = text();

        // Punishments that can be removed (bans / mutes), show a small status indicator
        if (data.getType().canRescind()) {
          Component status = data.isActive() ? MessageUtils.ACCEPT : MessageUtils.DENY;
          builder
              .append(
                  status.hoverEvent(
                      HoverEvent.showText(
                          text(
                              data.isActive()
                                  ? "Punishment is active"
                                  : "Punishment is no longer active",
                              NamedTextColor.GRAY))))
              .append(space());
        } else {
          builder.append(MessageUtils.WARNING).append(space());
        }

        builder.append(
            data.formatBroadcast(
                usernames.renderUsername(data.getIssuerId(), NameStyle.FANCY).join(),
                usernames.renderUsername(Optional.of(data.getTargetId()), NameStyle.FANCY).join(),
                moderation.getStaffFormat()));

        TextComponent.Builder hover = text();
        hover
            .append(text("Issued ", NamedTextColor.GRAY))
            .append(
                TemporalComponent.relativePastApproximate(data.getTimeIssued())
                    .color(NamedTextColor.YELLOW));

        Duration length = data.getDuration();
        // When a punishments can expire, show expire time on hover
        if (length != null) {
          Instant endDate = data.getTimeIssued().plus(length);
          if (data.isActive()) {
            hover
                .append(newline())
                .append(text("Expires in ", NamedTextColor.GRAY))
                .append(
                    TemporalComponent.briefNaturalApproximate(Instant.now(), endDate)
                        .color(NamedTextColor.YELLOW));
          } else if (!data.wasUpdated()) {
            hover
                .append(newline())
                .append(text("Expired ", NamedTextColor.GRAY))
                .append(
                    TemporalComponent.relativePastApproximate(endDate)
                        .color(NamedTextColor.YELLOW));
          }
        }

        if (data.wasUpdated()) {
          hover
              .append(newline())
              .append(text("Infraction lifted by ", NamedTextColor.GRAY)) // TODO: translate
              .append(usernames.renderUsername(data.getLastUpdatedBy(), NameStyle.FANCY).join())
              .append(space())
              .append(
                  TemporalComponent.relativePastApproximate(data.getLastUpdated())
                      .color(NamedTextColor.YELLOW));
        }

        // If punishment was issued on a different service, add note to hover message
        if (!((ModerationConfig) moderation.getConfig())
            .getService()
            .equalsIgnoreCase(data.getService())) {
          hover
              .append(newline())
              .append(text("Service ", NamedTextColor.GRAY))
              .append(BroadcastUtils.RIGHT_DIV.color(NamedTextColor.GOLD))
              .append(space())
              .append(text(data.getService(), NamedTextColor.AQUA));
        }

        return builder.hoverEvent(HoverEvent.showText(hover.build())).build();
      }

      @Override
      public Component formatEmpty() {
        if (target != null) {
          TranslatableComponent noneFound =
              translatable("moderation.records.lookupNone", NamedTextColor.RED);
          try {
            UUID uuid = UUID.fromString(target);
            Component targetName =
                usernames.renderUsername(uuid, NameStyle.PLAIN).join().color(NamedTextColor.AQUA);
            return noneFound.args(targetName);
          } catch (IllegalArgumentException e) {
            // No-op
          }
          return noneFound.args(text(target, NamedTextColor.AQUA));
        }
        return text("There have been no recent punishments", NamedTextColor.RED);
      }
    }.display(
        audience.getAudience(),
        punishmentData.stream().sorted().collect(Collectors.toList()),
        page);
  }
}
