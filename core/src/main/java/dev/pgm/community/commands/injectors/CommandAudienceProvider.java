package dev.pgm.community.commands.injectors;

import dev.pgm.community.utils.CommandAudience;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.injection.ParameterInjector;
import tc.oc.pgm.lib.org.incendo.cloud.util.annotation.AnnotationAccessor;

public final class CommandAudienceProvider
    implements ParameterInjector<CommandSender, CommandAudience> {

  @Override
  public @NotNull CommandAudience create(
      CommandContext<CommandSender> context, @NotNull AnnotationAccessor annotations) {
    return new CommandAudience(context.sender());
  }
}
