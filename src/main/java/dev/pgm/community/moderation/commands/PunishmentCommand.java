package dev.pgm.community.moderation.commands;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.moderation.ModerationConfig;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.punishments.Punishment;
import dev.pgm.community.moderation.punishments.PunishmentFormats;
import dev.pgm.community.moderation.punishments.PunishmentType;
import dev.pgm.community.moderation.punishments.types.ExpirablePunishment;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.utils.BroadcastUtils;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.MessageUtils;
import dev.pgm.community.utils.Sounds;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.api.text.PlayerComponent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

public class PunishmentCommand extends CommunityCommand {

  public static final Duration DEFAULT_TEMPBAN_LENGTH = Duration.ofDays(7); // TODO: Maybe config?

  @Dependency private ModerationFeature moderation;
  @Dependency private UsersFeature usernames;
  @Dependency private NickFeature nicks;

  @CommandAlias("punishmenthistory|ph")
  @Description("View a list of recent punishments")
  @Syntax("[page]")
  @CommandPermission(CommunityPermissions.PUNISH)
  public void viewRecentPunishments(
      CommandAudience audience, @Default("1") int page, @Default("1h") Duration length) {
    moderation
        .getRecentPunishments(length)
        .thenAcceptAsync(
            punishments -> {
              sendPunishmentHistory(audience, null, punishments, page);
            });
  }

  @CommandAlias("repeatpunishment|rp")
  @Description("Repeat the last punishment you performed for another player")
  @Syntax("[player]")
  @CommandCompletion("@players")
  @CommandPermission(CommunityPermissions.PUNISH)
  public void repeatPunishment(
      CommandAudience audience, @co.aikar.commands.annotation.Optional OnlinePlayer target) {
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
                      target.getPlayer().getUniqueId(),
                      audience,
                      reason,
                      length,
                      true,
                      isDisguised(audience, nicks));
                } else {
                  // No target supplied, show last punishment
                  PunishmentFormats.formatBroadcast(lastPunishment, usernames)
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

  @CommandAlias("unban|pardon|forgive")
  @Description("Pardon all active punishments for a player")
  @Syntax("[player]")
  @CommandCompletion("*")
  @CommandPermission(CommunityPermissions.UNBAN)
  public void unbanPlayer(CommandAudience audience, String target) {
    moderation
        .isBanned(target)
        .thenAcceptAsync(
            isBanned -> {
              if (isBanned) {
                moderation
                    .pardon(target, audience.getId())
                    .thenAcceptAsync(
                        pardon -> {
                          if (!pardon) {
                            audience.sendWarning(
                                text(target, NamedTextColor.DARK_AQUA)
                                    .append(text(" could not be ", NamedTextColor.GRAY))
                                    .append(text("unbanned"))
                                    .color(NamedTextColor.RED));
                          } else {
                            BroadcastUtils.sendAdminChatMessage(
                                text(target, NamedTextColor.DARK_AQUA)
                                    .append(text(" was unbanned by ", NamedTextColor.GRAY))
                                    .append(audience.getStyledName()),
                                Sounds.PUNISHMENT_PARDON);
                          }
                          // TODO: translate
                        });
              } else {
                audience.sendWarning(
                    text(target, NamedTextColor.AQUA)
                        .append(text(" has no active bans", NamedTextColor.GRAY)));
              }
            });
  }

  @CommandAlias("lookup|l")
  @Description("View infraction history of a player")
  @Syntax("[player] [page]")
  @CommandCompletion("@players *")
  @CommandPermission(CommunityPermissions.LOOKUP)
  public void viewPunishmentHistory(
      CommandAudience audience, String target, @Default("1") int page) {
    moderation
        .query(target)
        .thenAcceptAsync(punishments -> sendPunishmentHistory(audience, target, punishments, page));
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
      UUID targetID =
          (!UsersFeature.USERNAME_REGEX.matcher(target).matches() ? UUID.fromString(target) : null);
      if (targetID != null) {
        targetName =
            PlayerComponent.player(
                targetID, usernames.getStoredUsername(targetID).join(), NameStyle.FANCY);
      } else {
        targetName = PlayerComponent.player(null, target, NameStyle.FANCY, null);
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
                usernames.renderUsername(data.getIssuerId()).join(),
                usernames.renderUsername(Optional.of(data.getTargetId())).join()));

        TextComponent.Builder hover = text();
        hover
            .append(text("Issued ", NamedTextColor.GRAY))
            .append(
                TemporalComponent.relativePastApproximate(data.getTimeIssued())
                    .color(NamedTextColor.YELLOW));

        Duration length = ExpirablePunishment.getDuration(data);
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
              .append(usernames.renderUsername(data.getLastUpdatedBy()).join())
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
        return target != null
            ? translatable(
                "moderation.records.lookupNone",
                NamedTextColor.RED,
                text(target, NamedTextColor.AQUA))
            : text("There have been no recent punishments", NamedTextColor.RED);
      }
    }.display(
        audience.getAudience(),
        punishmentData.stream().sorted().collect(Collectors.toList()),
        page);
  }
}
