package dev.pgm.community.usernames;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.pgm.community.CommunityCommand;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.util.chat.Audience;
import tc.oc.pgm.util.text.formatting.PaginatedComponentResults;

/** NOTE: This command is just for testing. Please ignore. Will be removed in final commit */
public class UsernameDebugCommand extends CommunityCommand {

  @Dependency private UsernameService service;

  @CommandAlias("insertname")
  public void insertName(CommandSender sender, String name, String uuid) {
    service.setName(UUID.fromString(uuid), name);
    sender.sendMessage(format("&aInserted &b%s &ainto name cache", name));
  }

  @CommandAlias("lookupname")
  @Syntax("[name or uuid]")
  public void lookupName(CommandSender sender, String input) {
    sender.sendMessage("Started Lookup...");
    if (UsernameService.USERNAME_REGEX.matcher(input).matches()) {
      service
          .getStoredId(input)
          .thenAcceptAsync(
              uuid -> {
                if (uuid.isPresent()) {
                  sender.sendMessage(
                      String.format("Matching UUID for %s is %s", input, uuid.get().toString()));
                } else {
                  sender.sendMessage(
                      String.format("No matching UUID for %s could be found!", input));
                }
              });
    } else {
      service
          .getStoredUsername(UUID.fromString(input))
          .thenAcceptAsync(
              name -> {
                sender.sendMessage(String.format("Found username: %s", name));
              });
    }
  }

  @CommandAlias("names")
  public void listNames(final CommandSender sender, final @Default("1") int page) {
    Set<CachedUsernames> names = Sets.newHashSet();
    service.getAllNamesDebug().forEach((id, name) -> names.add(new CachedUsernames(id, name)));

    service
        .getStoredNamesDebug()
        .whenComplete(
            (results, error) -> {
              List<CachedUsernames> dbNames = Lists.newArrayList();
              results.forEach((id, name) -> dbNames.add(new CachedUsernames(id, name)));
              new PaginatedComponentResults<CachedUsernames>(
                  TextComponent.of(String.format("DB Names: (%d)", results.size()))) {

                @Override
                public Component format(CachedUsernames data, int index) {
                  return TextComponent.of(
                      String.format(
                          "DB - %d. %s - %s", index + 1, data.getId().toString(), data.getName()));
                }
              }.display(Audience.get(sender), dbNames, page);
            });

    if (names.isEmpty()) {
      sender.sendMessage("No cached usernames!");
      return;
    }

    new PaginatedComponentResults<CachedUsernames>(TextComponent.of("Cached Names:")) {
      @Override
      public Component format(CachedUsernames data, int index) {
        return TextComponent.of(
            String.format("C - %d. %s - %s", index + 1, data.getId().toString(), data.getName()));
      }
    }.display(Audience.get(sender), names, page);
  }

  public static class CachedUsernames {
    public CachedUsernames(UUID uuid, String name) {
      super();
      this.uuid = uuid;
      this.name = name;
    }

    private UUID uuid;
    private String name;

    public UUID getId() {
      return uuid;
    }

    public String getName() {
      return name;
    }
  }
}
