package dev.pgm.community.info;

import static net.kyori.adventure.text.Component.text;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import tc.oc.pgm.util.Audience;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public class InfoCommandData {

  private static final String LINES_KEY = "lines";
  private static final String PERMISSION_KEY = "permission";

  private String name;
  private List<String> lines;
  private String permission;

  public InfoCommandData(String name, List<String> lines, String permission) {
    this.name = name;
    this.lines = lines;
    this.permission = permission;
  }

  public static InfoCommandData of(ConfigurationSection section) {
    return new InfoCommandData(
        section.getName(), section.getStringList(LINES_KEY), section.getString(PERMISSION_KEY));
  }

  public String getName() {
    return name;
  }

  public List<String> getLines() {
    return lines;
  }

  public String getPermission() {
    return permission;
  }

  public void sendCommand(CommandSender sender) {
    Audience viewer = Audience.get(sender);

    if (getPermission() != null && !getPermission().isEmpty()) {
      if (!sender.hasPermission(getPermission())) {
        viewer.sendWarning(text("You do not have permission for this command"));
        return; // TODO: Translate
      }
    }

    getLines().stream()
        .map(BukkitUtils::colorize)
        .map(msg -> text(msg))
        .forEach(viewer::sendMessage);
  }
}
