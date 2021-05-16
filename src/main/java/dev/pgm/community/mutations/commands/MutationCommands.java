package dev.pgm.community.mutations.commands;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.mutations.Mutation;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.utils.CommandAudience;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

@CommandAlias("mutate|mutation|mt")
@Description("Manage match mutations")
public class MutationCommands extends CommunityCommand {

  @Dependency private MutationFeature mutations;

  @Subcommand("add|enable|a")
  @Description("Add a mutation to the match")
  @CommandCompletion("@addMutations")
  @CommandPermission(CommunityPermissions.MUTATION)
  public void addMutation(CommandAudience audience, MutationType type) {
    checkForMatch();
    if (!mutations.addMutation(audience, type, true)) {
      audience.sendWarning(
          text()
              .append(text(type.getDisplayName(), NamedTextColor.YELLOW))
              .append(text(" has already been added to the match."))
              .build());
    }
  }

  @Subcommand("remove|rm|disable")
  @Description("Remove an active mutation from the match")
  @CommandCompletion("@removeMutations")
  @CommandPermission(CommunityPermissions.MUTATION)
  public void removeMutation(CommandAudience audience, MutationType type) {
    checkForMatch();
    if (!mutations.removeMutation(audience, type)) {
      audience.sendWarning(
          text()
              .append(text(type.getDisplayName(), NamedTextColor.YELLOW))
              .append(text(" can not be removed from the match."))
              .build());
    }
  }

  @Default
  @Subcommand("list|ls")
  @Description("View a list of mutations")
  public void list(CommandAudience audience, @Default("1") int page) {
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

  private void checkForMatch() {
    if (mutations.getMatch() == null || mutations.getMatch().isFinished()) {
      throw new InvalidCommandArgument("Mutations can not be adjusted at this time", false);
    }
  }
}
