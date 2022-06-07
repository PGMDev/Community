package dev.pgm.community.party;

import static tc.oc.pgm.util.text.TextParser.parseDuration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.pgm.community.Community;
import dev.pgm.community.CommunityPermissions;
import dev.pgm.community.feature.config.FeatureConfigImpl;
import dev.pgm.community.party.presets.MapPartyPreset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class MapPartyConfig extends FeatureConfigImpl {

  private static final String KEY = "party";

  private Duration duration;
  private List<MapPartyPreset> presets;

  // Event Welcome / goodbye message
  private String welcomeLine;
  private String welcomeHover;
  private String welcomeCommand;
  private String goodbyeLine;

  // Extra server command
  private boolean extraServerEnabled;
  private String openExtraCommand;
  private String closeExtraCommand;

  // Broadcasts
  private boolean broadcastsEnabled;
  private Duration broadcastInterval;
  private String broadcastPrefix;
  private List<String> broadcastMessages;

  // Raindrop Command
  private String raindropCommand;

  // Default Party Settings
  private boolean showLoginMessage;
  private boolean showPartyNotifications;

  // Custom MOTD
  private String motdFormat;

  // Host Permissions
  private Permission hostPermissions;

  public MapPartyConfig(Configuration config) {
    super(KEY, config);
  }

  public Duration getDuration() {
    return duration;
  }

  public List<MapPartyPreset> getPresets() {
    return presets;
  }

  public boolean showLoginMessage() {
    return showLoginMessage;
  }

  public boolean showPartyNotifications() {
    return showPartyNotifications;
  }

  public String getWelcomeLine() {
    return welcomeLine;
  }

  public String getWelcomeHover() {
    return welcomeHover;
  }

  public String getWelcomeCommand() {
    return welcomeCommand;
  }

  public String getRaindropCommand() {
    return raindropCommand;
  }

  public String getGoodbyeMessage() {
    return goodbyeLine;
  }

  public boolean isExtraServerEnabled() {
    return extraServerEnabled;
  }

  public String getOpenExtraCommand() {
    return openExtraCommand;
  }

  public String getCloseExtraCommand() {
    return closeExtraCommand;
  }

  public boolean isBroadcastEnabled() {
    return broadcastsEnabled;
  }

  public Duration getBroadcastInterval() {
    return broadcastInterval;
  }

  public String getBroadcastPrefix() {
    return broadcastPrefix;
  }

  public List<String> getBroadcastMessages() {
    return broadcastMessages;
  }

  public String getMotdFormat() {
    return motdFormat;
  }

  public Permission getHostPermissions() {
    return hostPermissions;
  }

  @Override
  public void reload(Configuration config) {
    super.reload(config);
    this.duration = parseDuration(config.getString(KEY + ".duration"));
    this.presets = parsePresets(config.getConfigurationSection(KEY + ".presets"));
    this.showLoginMessage = config.getBoolean(KEY + ".settings.login-message");
    this.showPartyNotifications = config.getBoolean(KEY + ".settings.party-notifications");
    this.welcomeLine = config.getString(KEY + ".welcome.line");
    this.welcomeHover = config.getString(KEY + ".welcome.hover");
    this.welcomeCommand = config.getString(KEY + ".welcome.command");
    this.goodbyeLine = config.getString(KEY + ".goodbye.line");
    this.raindropCommand = config.getString(KEY + ".settings.raindrop-command");
    this.extraServerEnabled = config.getBoolean(KEY + ".extra.enabled");
    this.openExtraCommand = config.getString(KEY + ".extra.open-command");
    this.closeExtraCommand = config.getString(KEY + ".extra.close-command");
    this.broadcastsEnabled = config.getBoolean(KEY + ".broadcasts.enabled");
    this.broadcastInterval = parseDuration(config.getString(KEY + ".broadcasts.interval"));
    this.broadcastPrefix = config.getString(KEY + ".broadcasts.prefix");
    this.broadcastMessages = config.getStringList(KEY + ".broadcasts.lines");
    this.motdFormat = config.getString(KEY + ".motd-format");
    this.hostPermissions = parsePermissions(config.getStringList(KEY + ".host-permissions"));
  }

  private List<MapPartyPreset> parsePresets(ConfigurationSection section) {
    List<MapPartyPreset> presetList = Lists.newArrayList();
    for (String key : section.getKeys(false)) {
      presetList.add(MapPartyPreset.of(section.getConfigurationSection(key)));
    }
    return presetList;
  }

  private Permission parsePermissions(List<String> permissions) {
    Map<String, Boolean> nodes = Maps.newHashMap();
    for (String node : permissions) {
      nodes.put(node, !node.startsWith("-"));
    }
    Permission permission =
        new Permission(CommunityPermissions.PARTY_HOST, PermissionDefault.FALSE, nodes);

    // Register permission
    try {
      Community.get().getServer().getPluginManager().addPermission(permission);
    } catch (Throwable t) {
    }
    return permission;
  }
}
