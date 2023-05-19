package dev.pgm.community.commands.injectors;

import dev.pgm.community.utils.CommandAudience;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.lib.cloud.commandframework.annotations.AnnotationAccessor;
import tc.oc.pgm.lib.cloud.commandframework.annotations.injection.ParameterInjector;
import tc.oc.pgm.lib.cloud.commandframework.context.CommandContext;

public final class CommandAudienceProvider
    implements ParameterInjector<CommandSender, CommandAudience> {

  @Override
  public @NotNull CommandAudience create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    return new CommandAudience(context.getSender());
  }
}
