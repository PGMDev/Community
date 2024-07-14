package dev.pgm.community.requests.supervotes;

import static net.kyori.adventure.text.Component.text;

import dev.pgm.community.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class SuperVoteComponents {

  public static Component getSuperVoteBalance(int superVotes) {
    return getSuperVoteBalance(null, superVotes);
  }

  public static Component getSuperVoteBalance(Component name, int superVotes) {
    Component prefix = MessageUtils.VOTE;

    Component player = name == null
        ? text("You have ", NamedTextColor.GRAY)
        : text().append(name).append(text(" has ", NamedTextColor.GRAY)).build();

    Component amount = text(superVotes, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD);

    Component votes = text(" super vote" + (superVotes != 1 ? "s" : ""), NamedTextColor.GRAY);

    Component remain = text(" remaining", NamedTextColor.GRAY);

    return text()
        .append(prefix)
        .appendSpace()
        .append(player)
        .append(amount)
        .append(votes)
        .append(remain)
        .build();
  }
}
