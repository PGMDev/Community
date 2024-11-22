package dev.pgm.community.utils.ranks;

import dev.pgm.community.Community;
import java.util.List;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class RankUtils {

  @Nullable
  public static RanksConfig.Rank getHighestLevelRank(Player player) {
    List<RanksConfig.Rank> allRanks =
        Community.get().getServerConfig().getRanksConfig().getRanks();
    RanksConfig.Rank highestRank = null;

    for (RanksConfig.Rank rank : allRanks) {
      if (player.hasPermission(rank.getPermission())) {
        if (highestRank == null || rank.getWeight() > highestRank.getWeight()) {
          highestRank = rank;
        }
      }
    }

    return highestRank;
  }
}
