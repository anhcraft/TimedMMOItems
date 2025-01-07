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
    if (itemConfig.type.equalsIgnoreCase("bukkit")) {
      Material material = Material.matchMaterial(itemConfig.id.toUpperCase());
      if (material == null) {
        throw new IllegalArgumentException(
            "Couldn't replace expired item. Invalid Bukkit material: minecraft:"
                + itemConfig.id.toLowerCase());
      }
      return new ItemStack(material, itemConfig.amount * multiplier);
    } else { // MMOItem
      ItemManager itemManager = MMOItems.plugin.getItems();
      MMOItem mmoitem =
          itemManager.getMMOItem(MMOItems.plugin.getTypes().get(itemConfig.type), itemConfig.id);

      if (mmoitem != null) {
        ItemStack itemStack = mmoitem.newBuilder().build();
        itemStack.setAmount(itemConfig.amount * multiplier);
        return itemStack;
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Couldn't replace expired item. %s:%s not found. Item TYPE and ID are case"
                    + " sensitive!",
                itemConfig.type, itemConfig.id));
      }
    }
  }
}
