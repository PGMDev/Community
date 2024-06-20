package dev.pgm.community.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.pgm.community.utils.gson.types.DurationConverter;
import java.time.Duration;

public class GsonProvider {

  public static Gson get() {
    return new GsonBuilder().registerTypeAdapter(Duration.class, new DurationConverter()).create();
  }
}
