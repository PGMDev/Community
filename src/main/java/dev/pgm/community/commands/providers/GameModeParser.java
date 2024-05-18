package dev.pgm.community.commands.providers;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import tc.oc.pgm.command.parsers.EnumParser;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandContext;
import tc.oc.pgm.lib.org.incendo.cloud.context.CommandInput;
import tc.oc.pgm.lib.org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public class GameModeParser extends EnumParser<GameMode>
    implements BlockingSuggestionProvider.Strings<CommandSender> {

  public GameModeParser() {
    super(GameMode.class);
  }

  @Override
  protected GameMode bestMatch(CommandContext<CommandSender> context, String input) {
    if (StringUtils.isNumeric(input)) {
      int index = Integer.parseInt(input);
      GameMode[] enumValues = enumClass.getEnumConstants();

      if (index >= 0 && index < enumValues.length) {
        return GameMode.getByValue(index);
      }
    }

    return super.bestMatch(context, input);
  }

  @Override
  public List<String> stringSuggestions(CommandContext<CommandSender> context, CommandInput input) {
    int totalGamemodes = GameMode.values().length;
    List<String> suggestions = super.stringSuggestions(context, input);
    List<String> indexedSuggestions = new ArrayList<>(totalGamemodes * 2);

    // Add gamemode names
    indexedSuggestions.addAll(suggestions);

    // Add index values
    for (int i = 0; i < totalGamemodes; i++) {
      indexedSuggestions.add(String.valueOf(i));
    }

    return indexedSuggestions;
  }
}
