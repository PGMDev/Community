package dev.pgm.community.polls.response;

import static tc.oc.pgm.util.text.TextException.exception;

import dev.pgm.community.polls.ending.EndAction;
import java.util.List;

public class MultiChoiceResponseConverter {

  public static int convert(String input, List<EndAction> options) {
    for (int i = 0; i < options.size(); i++) {
      EndAction option = options.get(i);
      if (input.equalsIgnoreCase(option.getValue())) {
        return i;
      }
    }

    try {
      int selectedOption = Integer.parseInt(input);
      if (selectedOption >= 0 && selectedOption < options.size()) {
        return selectedOption;
      }
    } catch (NumberFormatException ignored) {
      throw exception("'" + input + "' is not a valid option!");
    }

    return -1; // No valid option was found!
  }
}
