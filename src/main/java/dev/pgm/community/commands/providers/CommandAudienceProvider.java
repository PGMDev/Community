package dev.pgm.community.commands.providers;

import cloud.commandframework.annotations.AnnotationAccessor;
import cloud.commandframework.annotations.injection.ParameterInjector;
import cloud.commandframework.context.CommandContext;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class CommandAudienceProvider
    implements ParameterInjector<CommandSender, CommandAudience> {

  @Override
  public @NotNull CommandAudience create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    return new CommandAudience(context.getSender());
  }
}
