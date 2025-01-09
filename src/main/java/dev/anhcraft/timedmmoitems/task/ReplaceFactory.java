package dev.anhcraft.timedmmoitems.task;

import dev.anhcraft.timedmmoitems.config.ItemConfig;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.manager.ItemManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ReplaceFactory {
  private ReplaceFactory() {}

  public static ItemStack createItemStack(ItemConfig itemConfig, int multiplier) {
    if (itemConfig.id.toLowerCase().startsWith("minecraft:")) {
      Material material = Material.matchMaterial(itemConfig.id.substring(9).toUpperCase());
      if (material == null) {
        throw new IllegalArgumentException(
            "Couldn't replace expired item. Invalid Bukkit material: "
                + itemConfig.id.toLowerCase());
      }
      return new ItemStack(material, itemConfig.amount * multiplier);

    } else if (itemConfig.id.toLowerCase().startsWith("mi:")) { // MMOItem
      String[] miItem = itemConfig.id.split(":");

      ItemManager itemManager = MMOItems.plugin.getItems();
      MMOItem mmoitem =
          itemManager.getMMOItem(MMOItems.plugin.getTypes().get(miItem[1]), miItem[2]);

      if (mmoitem != null) {
        ItemStack itemStack = mmoitem.newBuilder().build();
        itemStack.setAmount(itemConfig.amount * multiplier);
        return itemStack;
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Couldn't replace expired item. %s:%s not found. Item TYPE and ID are case"
                    + " sensitive!",
                miItem[1], miItem[2]));
      }
    } else {
      throw new IllegalArgumentException(
          String.format(
              "Couldn't replace expired item. %s is invalid." + " sensitive!", itemConfig.id));
    }
  }
}
