package dev.pgm.community.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityCommand;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.utils.CommandAudience;
import net.kyori.text.TextComponent;

// TODO: Maybe move to a different place
@CommandAlias("community")
@Description("Manage the community plugin")
@CommandPermission(CommunityPermissions.RELOAD)
public class CommunityPluginCommand extends CommunityCommand {

  @Dependency private Community plugin;

  @Default
  public void reload(CommandAudience audience) {
    plugin.reload();
    audience.sendWarning(TextComponent.of("Community has been reloaded")); // TODO: translate
  }
}
