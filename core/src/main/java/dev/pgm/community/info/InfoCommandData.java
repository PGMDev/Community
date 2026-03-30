package dev.pgm.community.info;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.utils.MessageUtils;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.util.Audience;

public record InfoCommandData(String name, List<Component> lines, String permission) {

  private static final String LINES_KEY = "lines";
  private static final String PERMISSION_KEY = "permission";

  public static InfoCommandData of(ConfigurationSection section) {
    return new InfoCommandData(
        section.getName(),
        section.getStringList(LINES_KEY).stream()
            .map(MessageUtils::parseComponentWithURL)
            .collect(Collectors.toList()),
        section.getString(PERMISSION_KEY));
  }

  public void sendCommand(CommandSender sender) {
    Audience viewer = Audience.get(sender);

    if (permission() != null && !permission().isEmpty()) {
      if (!sender.hasPermission(permission())) {
        viewer.sendWarning(text("You do not have permission for this command"));
        return; // TODO: Translate
      }
    }

    lines().forEach(viewer::sendMessage);
  }
}
