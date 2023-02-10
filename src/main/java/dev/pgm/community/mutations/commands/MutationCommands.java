package dev.pgm.community.mutations.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.utils.CommandAudience;
import dev.pgm.community.utils.PaginatedComponentResults;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.util.text.TextException;
import tc.oc.pgm.util.text.TextFormatter;

public class MutationCommands extends CommunityCommand {

  private final MutationFeature mutations;

  public MutationCommands() {
    this.mutations = Community.get().getFeatures().getMutations();
  }

  @CommandMethod("mutate|mutation|mt [page]")
  @CommandDescription("View a list of mutations")
  public void list(
      CommandAudience audience, @Argument(value = "page", defaultValue = "1") int page) {
    checkForMatch();

    if (audience.isPlayer() && audience.getPlayer().hasPermission(CommunityPermissions.MUTATION)) {
      mutations.getMenu().open(audience.getPlayer());
    } else {

      Set<Mutation> mts = mutations.getMutations();

      Component headerResultCount = text(Integer.toString(mts.size()), NamedTextColor.DARK_GREEN);

      int perPage = 7;
      int pages = (mts.size() + perPage - 1) / perPage;
      page = Math.max(1, Math.min(page, pages));

      NamedTextColor featureColor = NamedTextColor.DARK_GREEN;

      Component pageNum =
          translatable(
              "command.simplePageHeader",
              NamedTextColor.GRAY,
              text(Integer.toString(page), featureColor),
              text(Integer.toString(pages), featureColor));

      Component header =
          text()
              .append(text("Active Mutations", featureColor))
              .append(text(" ("))
              .append(headerResultCount)
              .append(text(") » "))
              .append(pageNum)
              .colorIfAbsent(NamedTextColor.GRAY)
              .build();

      Component formattedHeader =
          TextFormatter.horizontalLineHeading(audience.getSender(), header, NamedTextColor.YELLOW);

      new PaginatedComponentResults<Mutation>(formattedHeader, perPage) {

        @Override
        public Component format(Mutation data, int index) {
          return text().append(text("- ", NamedTextColor.GOLD)).append(data.getName()).build();
        }

        @Override
        public Component formatEmpty() {
          // TODO: Translate
          return text("No mutations are enabled", NamedTextColor.RED);
        }
      }.display(audience.getAudience(), mutations.getMutations(), page);
    }
  }

  @CommandMethod("mutate|mutation|mt add <type>")
  @CommandDescription("Add a mutation to the match")
  @CommandPermission(CommunityPermissions.MUTATION)
  public void addMutation(CommandAudience audience, @Argument("type") MutationType type) {
    checkForMatch();
    if (!mutations.addMutation(audience, type, true)) {
      audience.sendWarning(
          text()
              .append(text(type.getDisplayName(), NamedTextColor.YELLOW))
              .append(text(" has already been added to the match."))
              .build());
    }
  }

  @CommandMethod("mutate|mutation|mt remove <type>")
  @CommandDescription("Remove an active mutation from the match")
  @CommandPermission(CommunityPermissions.MUTATION)
  public void removeMutation(CommandAudience audience, @Argument("type") MutationType type) {
    checkForMatch();
    if (!mutations.removeMutation(audience, type)) {
      audience.sendWarning(
          text()
              .append(text(type.getDisplayName(), NamedTextColor.YELLOW))
              .append(text(" can not be removed from the match."))
              .build());
    }
  }

  private void checkForMatch() {
    if (mutations.getMatch() == null || mutations.getMatch().isFinished()) {
      throw TextException.exception("Mutations can not be adjusted at this time!");
    }
  }
}
