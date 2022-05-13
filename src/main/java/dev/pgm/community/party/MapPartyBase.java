package dev.pgm.community.party;

import static dev.pgm.community.utils.MessageUtils.color;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.Community;
import dev.pgm.community.party.events.MapPartyEndEvent;
import dev.pgm.community.party.events.MapPartyRestartEvent;
import dev.pgm.community.party.events.MapPartyStartEvent;
import dev.pgm.community.party.hosts.MapPartyHosts;
import dev.pgm.community.party.settings.MapPartySettings;
import java.time.Duration;
import java.time.Instant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class MapPartyBase implements MapParty {

  private String name;
  private String description;
  private Duration length;
  private MapPartyHosts hosts;
  private MapPartySettings settings;
  private boolean running;
  private boolean setup;

  private Instant startTime;

  public MapPartyBase(
      String name,
      String description,
      Duration length,
      MapPartyHosts hosts,
      MapPartySettings settings) {
    this.name = name;
    this.description = description;
    this.length = length;
    this.hosts = hosts;
    this.settings = settings;
    this.running = false;
    this.setup = false;
    this.startTime = null;
  }

  @Override
  public Component getStyledName() {
    Component colorName = color(getName());
    Component colorDesc = color(getDescription());
    return text()
        .append(color(getName()))
        .hoverEvent(
            HoverEvent.showText(text().append(colorName).append(newline()).append(colorDesc)))
        .build();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public Duration getLength() {
    return length;
  }

  public void setLength(Duration length) {
    this.length = length;
  }

  @Override
  public Instant getStartTime() {
    return startTime;
  }

  @Override
  public MapPartyHosts getHosts() {
    return hosts;
  }

  @Override
  public MapPartySettings getSettings() {
    return settings;
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isSetup() {
    return setup;
  }

  @Override
  public void setSetup(boolean setup) {
    this.setup = setup;
  }

  @Override
  public void start(CommandSender sender) {
    this.running = true;
    this.startTime = Instant.now();
    Community.get().callEvent(new MapPartyStartEvent(this, sender));
  }

  @Override
  public void stop(CommandSender sender) {
    this.running = false;
    Community.get().callEvent(new MapPartyEndEvent(this, sender));
  }

  @Override
  public void restart(CommandSender sender) {
    this.startTime = Instant.now();
    Community.get().callEvent(new MapPartyRestartEvent(this, sender));
  }

  @Override
  public void onLogin(PlayerJoinEvent event) {
    this.hosts.onLogin(event.getPlayer());
  }

  @Override
  public void onQuit(PlayerQuitEvent event) {
    this.hosts.onQuit(event.getPlayer());
  }

  private static final String DEFAULT_NAME = "Map Party";
  private static final String DEFAULT_DESC = "It's party time!";

  protected static String formatDefaultName(String creator) {
    return creator + "'s " + DEFAULT_NAME;
  }

  protected static String formatDefaultDescription() {
    return DEFAULT_DESC;
  }
}
