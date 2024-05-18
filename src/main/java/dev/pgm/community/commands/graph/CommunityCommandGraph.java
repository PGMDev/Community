package dev.pgm.community.commands.graph;

import dev.pgm.community.Community;
import dev.pgm.community.assistance.commands.PlayerHelpCommand;
import dev.pgm.community.assistance.commands.ReportCommands;
import dev.pgm.community.broadcast.BroadcastCommand;
import dev.pgm.community.chat.management.ChatManagementCommand;
import dev.pgm.community.commands.CommunityPluginCommand;
import dev.pgm.community.commands.ContainerCommand;
import dev.pgm.community.commands.FlightCommand;
import dev.pgm.community.commands.GamemodeCommand;
import dev.pgm.community.commands.ServerInfoCommand;
import dev.pgm.community.commands.StaffCommand;
import dev.pgm.community.commands.SudoCommand;
import dev.pgm.community.commands.VanishedCommand;
import dev.pgm.community.commands.injectors.CommandAudienceProvider;
import dev.pgm.community.commands.player.TargetPlayer;
import dev.pgm.community.commands.providers.GameModeParser;
import dev.pgm.community.commands.providers.TargetPlayerParser;
import dev.pgm.community.freeze.FreezeCommand;
import dev.pgm.community.friends.commands.FriendshipCommand;
import dev.pgm.community.mobs.MobCommand;
import dev.pgm.community.moderation.commands.BanCommand;
import dev.pgm.community.moderation.commands.KickCommand;
import dev.pgm.community.moderation.commands.MuteCommand;
import dev.pgm.community.moderation.commands.PunishmentCommand;
import dev.pgm.community.moderation.commands.ToolCommand;
import dev.pgm.community.moderation.commands.WarnCommand;
import dev.pgm.community.mutations.MutationType;
import dev.pgm.community.mutations.commands.MutationCommands;
import dev.pgm.community.nick.commands.NickCommands;
import dev.pgm.community.party.MapPartyCommands;
import dev.pgm.community.party.MapPartyType;
import dev.pgm.community.polls.PollThreshold;
import dev.pgm.community.polls.commands.PollManagementCommands;
import dev.pgm.community.polls.commands.PollVoteCommands;
import dev.pgm.community.requests.commands.RequestCommands;
import dev.pgm.community.requests.commands.SponsorCommands;
import dev.pgm.community.requests.commands.TokenCommands;
import dev.pgm.community.teleports.TeleportCommand;
import dev.pgm.community.users.commands.UserInfoCommands;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.injectors.MatchProvider;
import tc.oc.pgm.command.injectors.PlayerProvider;
import tc.oc.pgm.command.parsers.EnumParser;
import tc.oc.pgm.command.parsers.MapInfoParser;
import tc.oc.pgm.command.parsers.PartyParser;
import tc.oc.pgm.command.parsers.PlayerParser;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.lib.org.incendo.cloud.minecraft.extras.MinecraftHelp;
import tc.oc.pgm.lib.org.incendo.cloud.parser.standard.StringParser;
import tc.oc.pgm.util.Audience;

public class CommunityCommandGraph extends CommandGraph<Community> {

  public CommunityCommandGraph(Community plugin) throws Exception {
    super(plugin);
  }

  @Override
  protected MinecraftHelp<CommandSender> createHelp() {
    return MinecraftHelp.create("/community help", manager, Audience::get);
  }

  @Override
  protected void setupInjectors() {
    registerInjector(CommandAudience.class, new CommandAudienceProvider());
    registerInjector(PGM.class, PGM::get);
    registerInjector(Match.class, new MatchProvider());
    registerInjector(MatchPlayer.class, new MatchPlayerProvider());
    registerInjector(Player.class, new PlayerProvider());
  }

  @Override
  protected void setupParsers() {
    registerParser(MapInfo.class, MapInfoParser::new);
    registerParser(MapPartyType.class, new EnumParser<>(MapPartyType.class));
    registerParser(MutationType.class, new EnumParser<>(MutationType.class));
    registerParser(PollThreshold.class, new EnumParser<>(PollThreshold.class));
    registerParser(TargetPlayer.class, new TargetPlayerParser());
    registerParser(Player.class, new PlayerParser());
    registerParser(Party.class, PartyParser::new);
    registerParser(GameMode.class, new GameModeParser());
  }

  @Override
  protected void registerCommands() {
    // Assistance
    register(new PlayerHelpCommand());
    register(new ReportCommands());

    // Broadcast
    register(new BroadcastCommand());

    // Chat
    register(new ChatManagementCommand());

    // Freeze
    register(new FreezeCommand());

    // Friends
    register(new FriendshipCommand());

    // Mobs
    register(new MobCommand());

    // Moderation
    register(new BanCommand());
    register(new KickCommand());
    register(new MuteCommand());
    register(new PunishmentCommand());
    register(new ToolCommand());
    register(new WarnCommand());

    // Mutations
    register(new MutationCommands());

    // Nick
    register(new NickCommands());

    // Party
    register(new MapPartyCommands());

    // Polls
    register(new PollManagementCommands());
    register(new PollVoteCommands());

    // Requests
    register(new RequestCommands());
    register(new SponsorCommands());
    register(new TokenCommands());

    // Teleport
    register(new TeleportCommand());

    // Users
    register(new UserInfoCommands());

    // Etc. Commands
    register(new ContainerCommand());
    register(new FlightCommand());
    register(new GamemodeCommand());
    register(new ServerInfoCommand());
    register(new StaffCommand());
    register(new SudoCommand());
    register(new VanishedCommand());

    // Community plugin command
    register(new CommunityPluginCommand());

    // Help command
    manager.command(
        manager
            .commandBuilder("community")
            .literal("help")
            .optional("query", StringParser.greedyStringParser())
            .handler(
                context ->
                    minecraftHelp.queryCommands(
                        context.<String>optional("query").orElse(""), context.sender())));
  }
}
