package dev.pgm.community.util;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Predicate;
import org.bukkit.Server;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Supports.List.class)
public @interface Supports {
  Supports.Variant value();

  String minVersion() default "";

  String maxVersion() default "";

  Supports.Priority priority() default Supports.Priority.MEDIUM;

  enum Variant {
    SPORTPAPER(sv -> sv.getVersion().contains("SportPaper")),
    PAPER(sv -> sv.getName().equalsIgnoreCase("Paper"));

    public final Predicate<Server> matcher;

    Variant(Predicate<Server> matches) {
      this.matcher = matches;
    }
  }

  enum Priority {
    LOWEST,
    LOW,
    MEDIUM,
    HIGH,
    HIGHEST
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface List {
    Supports[] value();
  }
}
