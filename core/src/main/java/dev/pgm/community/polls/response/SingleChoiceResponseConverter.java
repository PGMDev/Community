package dev.pgm.community.polls.response;

import static tc.oc.pgm.util.text.TextException.exception;

public class SingleChoiceResponseConverter {

  public static boolean convert(String input) {
    String cleanInput = input.trim().toLowerCase();

    return switch (cleanInput) {
      case "true", "yes", "y", "affirmative", "ok", "okay", "yeah" -> true;
      case "false", "no", "n", "negative", "nope", "not okay" -> false;
      default ->
        throw exception(
            "Invalid input: '" + input + "'! Please provide a valid 'yes' or 'no' response.");
    };
  }
}
