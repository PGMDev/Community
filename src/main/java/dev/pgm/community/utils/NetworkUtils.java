package dev.pgm.community.utils;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class NetworkUtils {

  public static String getServer() {
    return Community.get().getServerConfig().getServerId();
  }

  public static Component formatServer(String server) {
    return text()
        .append(text("["))
        .append(text(server, NamedTextColor.AQUA))
        .append(text("] "))
        .hoverEvent(
            HoverEvent.showText(
                text()
                    .append(text("Click to join "))
                    .append(text(server, NamedTextColor.AQUA))
                    .color(NamedTextColor.GRAY)
                    .build()))
        .clickEvent(ClickEvent.runCommand("/server " + server))
        .build();
  }

  public static Component server(String server) {
    return isLocal(server) ? empty() : formatServer(server);
  }

  private static boolean isLocal(String request) {
    return request == null || request.equalsIgnoreCase(getServer());
  }

  @Nullable
  public static String getServerVar(String def) {
    String id = System.getenv("SERVER_NAME");
    return (id != null && !id.isEmpty()) ? id : def;
  }
}
