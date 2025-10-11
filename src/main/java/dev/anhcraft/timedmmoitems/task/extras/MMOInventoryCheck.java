package dev.anhcraft.timedmmoitems.task.extras;

import org.bukkit.inventory.Inventory;
import ru.endlesscode.rpginventory.api.InventoryAPI;

public class MMOInventoryCheck implements InventoryCheck {
  public static final InventoryCheck INSTANCE = new MMOInventoryCheck();

  @Override
  public boolean matches(Inventory inventory) {
    return InventoryAPI.isRPGInventory(inventory);
  }
}
