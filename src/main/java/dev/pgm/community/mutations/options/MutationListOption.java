package dev.pgm.community.mutations.options;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.Material;

public class MutationListOption<T> extends MutationOption {

  private List<T> options;
  private int valueIndex;
  private T def;

  public MutationListOption(
      String name,
      String description,
      Material iconMaterial,
      boolean prerequisite,
      List<T> options) {
    super(name, description, iconMaterial, prerequisite);
    this.options = options;
    this.valueIndex = 0;
    this.def = options.get(0);
  }

  public ImmutableList<T> getOptions() {
    return ImmutableList.copyOf(options);
  }

  public T getDefaultValue() {
    return def;
  }

  public T getValue() {
    return options.get(valueIndex);
  }

  public void toggle(boolean forward) {
    this.valueIndex = valueIndex + (forward ? 1 : -1);

    if (valueIndex >= options.size()) {
      valueIndex = 0;
    }

    if (valueIndex < 0) {
      valueIndex = options.size() - 1;
    }
  }
}
