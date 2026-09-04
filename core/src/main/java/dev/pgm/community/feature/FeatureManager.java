package dev.pgm.community.feature;

import dev.pgm.community.alts.feature.AltRiskFeature;
import dev.pgm.community.assistance.feature.AssistanceFeature;
import dev.pgm.community.assistance.feature.types.AssistanceFeatureCore;
import dev.pgm.community.audit.CommandAuditFeature;
import dev.pgm.community.broadcast.BroadcastFeature;
import dev.pgm.community.chat.management.ChatManagementFeature;
import dev.pgm.community.chat.network.NetworkChatFeature;
import dev.pgm.community.freeze.FreezeFeature;
import dev.pgm.community.friends.feature.FriendshipFeature;
import dev.pgm.community.friends.feature.types.FriendshipFeatureCore;
import dev.pgm.community.history.MatchHistoryFeature;
import dev.pgm.community.info.InfoCommandsFeature;
import dev.pgm.community.mobs.MobFeature;
import dev.pgm.community.moderation.feature.ModerationFeature;
import dev.pgm.community.moderation.feature.types.ModerationFeatureCore;
import dev.pgm.community.motd.MotdFeature;
import dev.pgm.community.mutations.feature.MutationFeature;
import dev.pgm.community.network.feature.NetworkFeature;
import dev.pgm.community.network.types.RedisNetworkFeature;
import dev.pgm.community.nick.feature.NickFeature;
import dev.pgm.community.nick.feature.types.NickFeatureCore;
import dev.pgm.community.party.feature.MapPartyFeature;
import dev.pgm.community.polls.feature.PollFeature;
import dev.pgm.community.requests.feature.RequestFeature;
import dev.pgm.community.requests.feature.types.RequestFeatureCore;
import dev.pgm.community.sessions.feature.SessionFeature;
import dev.pgm.community.sessions.feature.types.SessionFeatureCore;
import dev.pgm.community.settings.feature.SettingsFeature;
import dev.pgm.community.settings.feature.types.SettingsFeatureCore;
import dev.pgm.community.squads.SquadFeature;
import dev.pgm.community.store.StoreFactory;
import dev.pgm.community.store.Stores;
import dev.pgm.community.teleports.TeleportFeature;
import dev.pgm.community.teleports.TeleportFeatureBase;
import dev.pgm.community.users.feature.UsersFeature;
import dev.pgm.community.users.feature.types.UsersFeatureCore;
import fr.minuskube.inv.InventoryManager;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;

/** Manages all {@link Feature}s of the plugin */
public class FeatureManager {

  private final AssistanceFeature reports;
  private final ModerationFeature moderation;
  private final UsersFeature users;
  private final FriendshipFeature friends;
  private final NetworkFeature network;
  private final NickFeature nick;
  private final RequestFeature requests;
  private final SessionFeature sessions;
  private final AltRiskFeature altRisk;
  private final SettingsFeature settings;

  private final TeleportFeature teleports;
  private final InfoCommandsFeature infoCommands;
  private final ChatManagementFeature chatManagement;
  private final NetworkChatFeature chatNetwork;
  private final MotdFeature motd;
  private final FreezeFeature freeze;
  private final MutationFeature mutation;
  private final BroadcastFeature broadcast;
  private final CommandAuditFeature commandAudit;
  private final MobFeature mob;
  private final MapPartyFeature party;
  private final PollFeature polls;
  private final SquadFeature squads;
  private final MatchHistoryFeature history;

  public FeatureManager(Configuration config, Logger logger, InventoryManager inventory) {
    // Networking
    this.network = new RedisNetworkFeature(config, logger);
    Stores stores = StoreFactory.create(config, logger);

    // DB Features
    this.users = new UsersFeatureCore(config, logger, stores.users());
    this.sessions = new SessionFeatureCore(users, logger, stores.sessions());
    this.reports =
        new AssistanceFeatureCore(config, logger, users, network, inventory, stores.assistance());
    this.moderation =
        new ModerationFeatureCore(config, logger, users, network, stores.moderation());
    this.settings = new SettingsFeatureCore(config, logger, stores.settings(), network);
    this.friends =
        new FriendshipFeatureCore(config, logger, users, stores.friends(), settings, network);
    this.nick = new NickFeatureCore(config, logger, users, stores.nicks());
    this.requests = new RequestFeatureCore(config, logger, users, stores.requests());
    this.altRisk = new AltRiskFeature(config, logger, users, sessions, moderation);

    // TODO: 1. Support non-sql databases?
    // Ex. FileReportFeature, MongoReportFeature, RedisReportFeature...
    // Not a priority

    // Non-DB Features
    this.teleports = new TeleportFeatureBase(config, logger);
    this.infoCommands = new InfoCommandsFeature(config, logger);
    this.chatManagement = new ChatManagementFeature(config, logger);
    this.motd = new MotdFeature(config, logger);
    this.freeze = new FreezeFeature(config, logger);
    this.mutation = new MutationFeature(config, logger, inventory);
    this.broadcast = new BroadcastFeature(config, logger);
    this.commandAudit = new CommandAuditFeature(config, logger);
    this.chatNetwork = new NetworkChatFeature(config, logger, network);
    this.mob = new MobFeature(config, logger);
    this.party = new MapPartyFeature(config, logger);
    this.polls = new PollFeature(config, logger);
    this.squads = new SquadFeature(config, logger, settings);
    this.history = new MatchHistoryFeature(config, logger);
  }

  public AssistanceFeature getReports() {
    return reports;
  }

  public ModerationFeature getModeration() {
    return moderation;
  }

  public UsersFeature getUsers() {
    return users;
  }

  public SessionFeature getSessions() {
    return sessions;
  }

  public AltRiskFeature getAltRisk() {
    return altRisk;
  }

  public SettingsFeature getSettings() {
    return settings;
  }

  public TeleportFeature getTeleports() {
    return teleports;
  }

  public InfoCommandsFeature getInfoCommands() {
    return infoCommands;
  }

  public ChatManagementFeature getChatManagement() {
    return chatManagement;
  }

  public FriendshipFeature getFriendships() {
    return friends;
  }

  public MotdFeature getMotd() {
    return motd;
  }

  public FreezeFeature getFreeze() {
    return freeze;
  }

  public MutationFeature getMutations() {
    return mutation;
  }

  public NickFeature getNick() {
    return nick;
  }

  public BroadcastFeature getBroadcast() {
    return broadcast;
  }

  public CommandAuditFeature getCommandAudit() {
    return commandAudit;
  }

  public NetworkChatFeature getNetworkChat() {
    return chatNetwork;
  }

  public RequestFeature getRequests() {
    return requests;
  }

  public MobFeature getMobs() {
    return mob;
  }

  public MapPartyFeature getParty() {
    return party;
  }

  public PollFeature getPolls() {
    return polls;
  }

  public SquadFeature getSquads() {
    return squads;
  }

  public MatchHistoryFeature getHistory() {
    return history;
  }

  public void reloadConfig(Configuration config) {
    // Reload all config values here
    getReports().getConfig().reload(config);
    getModeration().getConfig().reload(config);
    if (getModeration().getTools() != null) {
      getModeration().getTools().reload(getModeration().getModerationConfig());
    }
    getUsers().getConfig().reload(config);
    getSessions().getConfig().reload(config);
    getSettings().getConfig().reload(config);
    getAltRisk().getConfig().reload(config);
    getTeleports().getConfig().reload(config);
    getInfoCommands().getConfig().reload(config);
    getChatManagement().getConfig().reload(config);
    getMotd().getConfig().reload(config);
    getFreeze().getConfig().reload(config);
    getMutations().getConfig().reload(config);
    getBroadcast().getConfig().reload(config);
    getNick().getConfig().reload(config);
    getCommandAudit().getConfig().reload(config);
    getNetworkChat().getConfig().reload(config);
    getRequests().getConfig().reload(config);
    getMobs().getConfig().reload(config);
    getParty().getConfig().reload(config);
    getPolls().getConfig().reload(config);
    getSquads().getConfig().reload(config);
    getHistory().getConfig().reload(config);

    // TODO: Look into maybe unregister commands for features that have been disabled
    // commands#unregisterCommand
    // Will need to check isEnabled
  }

  public void disable() {
    if (getReports().isEnabled()) getReports().disable();
    if (getModeration().isEnabled()) getModeration().disable();
    if (getUsers().isEnabled()) getUsers().disable();
    if (getSessions().isEnabled()) getSessions().disable();
    if (getSettings().isEnabled()) getSettings().disable();
    if (getAltRisk().isEnabled()) getAltRisk().disable();
    if (getTeleports().isEnabled()) getTeleports().disable();
    if (getInfoCommands().isEnabled()) getInfoCommands().disable();
    if (getChatManagement().isEnabled()) getChatManagement().disable();
    if (getMotd().isEnabled()) getMotd().disable();
    if (getFreeze().isEnabled()) getFreeze().disable();
    if (getMutations().isEnabled()) getMutations().disable();
    if (getBroadcast().isEnabled()) getBroadcast().disable();
    if (getNick().isEnabled()) getNick().disable();
    if (getCommandAudit().isEnabled()) getCommandAudit().disable();
    if (getNetworkChat().isEnabled()) getNetworkChat().disable();
    if (getRequests().isEnabled()) getRequests().disable();
    if (getMobs().isEnabled()) getMobs().disable();
    if (getParty().isEnabled()) getParty().disable();
    if (getPolls().isEnabled()) getPolls().disable();
    if (getSquads().isEnabled()) getSquads().disable();
    if (getHistory().isEnabled()) getHistory().disable();
  }
}
