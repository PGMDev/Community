package dev.pgm.community.polls.ending;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface EndAction {

  void execute(Player creator);

  Component getName();

  Component getPreviewValue();

  Component getDefaultQuestion();

  Component getButtonValue(boolean mixed);

  String getValue();

  String getTypeName();
}
