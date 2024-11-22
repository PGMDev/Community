package dev.pgm.community.utils.ranks;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import tc.oc.pgm.util.text.TextFormatter;

public class RanksConfig {

  private static final String RANKS_KEY = "ranks";

  private List<Rank> ranks;

  public RanksConfig(Configuration config) {
    reload(config);
  }

  public List<Rank> getRanks() {
    return ranks;
  }

  public void reload(Configuration config) {
    ranks = Lists.newArrayList();

    Set<String> rankNames = config.getConfigurationSection(RANKS_KEY).getKeys(false);
    for (String rankName : rankNames) {
      String name = config.getString(RANKS_KEY + "." + rankName + ".name");
      String prefix = config.getString(RANKS_KEY + "." + rankName + ".prefix");
      String permission = config.getString(RANKS_KEY + "." + rankName + ".permission");
      String colorString = config.getString(RANKS_KEY + "." + rankName + ".color");
      ChatColor color = ChatColor.valueOf(colorString.toUpperCase());
      int multiplier = config.getInt(RANKS_KEY + "." + rankName + ".multiplier");
      int weight = config.getInt(RANKS_KEY + "." + rankName + ".weight");

      Rank rank = new Rank(name, prefix, permission, color, multiplier, weight);
      ranks.add(rank);
    }

    Collections.sort(ranks, Comparator.comparingInt(Rank::getWeight));
  }

  public static class Rank {
    private String name;
    private String prefix;
    private String permission;
    private ChatColor color;
    private int multiplier;
    private int weight;

    public Rank(
        String name,
        String prefix,
        String permission,
        ChatColor color,
        int multiplier,
        int weight) {
      this.name = name;
      this.prefix = prefix;
      this.color = color;
      this.multiplier = multiplier;
      this.weight = weight;
    }

    public String getName() {
      return name;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getPermission() {
      return permission;
    }

    public ChatColor getColor() {
      return color;
    }

    public NamedTextColor getTextColor() {
      return TextFormatter.convert(getColor());
    }

    public int getVoteMultiplier() {
      return multiplier;
    }

    public int getWeight() {
      return weight;
    }
  }
}
