package dev.pgm.community.squads;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static tc.oc.pgm.util.text.TextException.exception;

import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.channels.Channel;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.util.named.NameStyle;

public class SquadChannel implements Channel<Squad> {

  public static final SquadChannel INSTANCE = new SquadChannel();

  private static final List<String> ALIASES = List.of("sc", "pc");

  public static final TextComponent PREFIX =
      text().append(text("(Party) ", NamedTextColor.YELLOW)).build();

  private SquadFeature squads;
  private MatchManager matchManager;

  public void init(SquadFeature squadFeature) {
    this.squads = squadFeature;
    this.matchManager = PGM.get().getMatchManager();
  }

  @Override
  public String getDisplayName() {
    return "squad";
  }

  @Override
  public List<String> getAliases() {
    return ALIASES;
  }

  @Override
  public String getLoggerFormat(Squad target) {
    return "(Party) %s: %s";
  }

  @Override
  public Squad getTarget(MatchPlayer matchPlayer, CommandContext<CommandSender> commandContext) {
    Squad squad = squads.getSquadByPlayer(matchPlayer);
    if (squad == null) throw exception("squad.err.memberOnly");

    return squad;
  }

  @Override
  public Collection<MatchPlayer> getViewers(Squad squad) {
    return squad.getPlayers().stream().map(uuid -> matchManager.getPlayer(uuid)).toList();
  }

  @Override
  public Component formatMessage(Squad squad, @Nullable MatchPlayer sender, Component message) {
    return text()
        .append(PREFIX)
        .append(
            sender != null
                ? text()
                    .append(sender.getName(NameStyle.VERBOSE))
                    .append(text(": ", NamedTextColor.WHITE))
                    .build()
                : empty())
        .append(message)
        .build();
  }
}
