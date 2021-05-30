package dev.pgm.community.moderation.tools;

import static tc.oc.pgm.util.bukkit.BukkitUtils.colorize;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class ToolBase implements Tool {

  private Player viewer;

  private String name;
  private List<String> lore;
  private Material material;
  private int amount;

  public ToolBase(Player viewer, String name, List<String> lore, Material material, int amount) {
    this.viewer = viewer;
    this.name = name;
    this.lore = lore;
    this.material = material;
    this.amount = amount;
  }

  @Override
  public Player getViewer() {
    return viewer;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getLore() {
    return lore;
  }

  @Override
  public Material getMaterial() {
    return material;
  }

  @Override
  public int getAmount() {
    return amount;
  }

  @Override
  public ItemStack getIcon() {
    return getItem(getName(), getMaterial(), getLore(), getAmount());
  }

  protected ItemStack getItem(String text, Material material, List<String> lore, int amount) {
    ItemStack stack = new ItemStack(material, amount);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(colorize(text));
    meta.setLore(lore);
    meta.addItemFlags(ItemFlag.values());
    stack.setItemMeta(meta);
    return stack;
  }
}
