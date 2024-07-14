package dev.pgm.community.polls.response;

import static tc.oc.pgm.util.text.TextException.exception;

public class SingleChoiceResponseConverter {

  public static boolean convert(String input) {
    String cleanInput = input.trim().toLowerCase();

    switch (cleanInput) {
      case "true":
      case "yes":
      case "y":
      case "affirmative":
      case "ok":
      case "okay":
      case "yeah":
        return true;
      case "false":
      case "no":
      case "n":
      case "negative":
      case "nope":
      case "not okay":
        return false;
      default:
        throw exception(
            "Invalid input: '" + input + "'! Please provide a valid 'yes' or 'no' response.");
    }
  }
}
