package dev.pgm.community.commands.graph;

import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import dev.pgm.community.Community;
import dev.pgm.community.assistance.commands.PlayerHelpCommand;
import dev.pgm.community.assistance.commands.ReportCommands;
import dev.pgm.community.broadcast.BroadcastCommand;
import dev.pgm.community.chat.management.ChatManagementCommand;
import dev.pgm.community.commands.ContainerCommand;
import dev.pgm.community.commands.FlightCommand;
import dev.pgm.community.commands.GamemodeCommand;
import dev.pgm.community.commands.ServerInfoCommand;
import dev.pgm.community.commands.StaffCommand;
import dev.pgm.community.commands.SudoCommand;
import dev.pgm.community.commands.providers.CommandAudienceProvider;
import dev.pgm.community.freeze.FreezeCommand;
import dev.pgm.community.friends.commands.FriendshipCommand;
import dev.pgm.community.mobs.MobCommand;
import dev.pgm.community.moderation.commands.BanCommand;
import dev.pgm.community.moderation.commands.KickCommand;
import dev.pgm.community.moderation.commands.MuteCommand;
import dev.pgm.community.moderation.commands.PunishmentCommand;
import dev.pgm.community.moderation.commands.ToolCommand;
import dev.pgm.community.moderation.commands.WarnCommand;
import dev.pgm.community.mutations.commands.MutationCommands;
import dev.pgm.community.nick.commands.NickCommands;
import dev.pgm.community.party.MapPartyCommands;
import dev.pgm.community.requests.commands.RequestCommands;
import dev.pgm.community.requests.commands.SponsorCommands;
import dev.pgm.community.requests.commands.TokenCommands;
import dev.pgm.community.teleports.TeleportCommand;
import dev.pgm.community.users.commands.UserInfoCommands;
import dev.pgm.community.utils.CommandAudience;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.command.injectors.MatchPlayerProvider;
import tc.oc.pgm.command.injectors.MatchProvider;
import tc.oc.pgm.command.util.CommandGraph;
import tc.oc.pgm.util.Audience;

public class CommunityCommandGraph extends CommandGraph<Community> {

  public CommunityCommandGraph(Community plugin) throws Exception {
    super(plugin);
  }

  @Override
  protected MinecraftHelp<CommandSender> createHelp() {
    return new MinecraftHelp<>("/community help", Audience::get, manager);
  }

  @Override
  protected CommandConfirmationManager<CommandSender> createConfirmationManager() {
    return null;
  }

  @Override
  protected void setupInjectors() {
    registerInjector(PGM.class, PGM::get);
    registerInjector(Match.class, new MatchProvider());
    registerInjector(MatchPlayer.class, new MatchPlayerProvider());
    registerInjector(CommandAudience.class, new CommandAudienceProvider());
  }

  @Override
  protected void setupParsers() {}

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
  }
}
