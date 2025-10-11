package dev.anhcraft.timedmmoitems.task.extras;

import org.bukkit.inventory.Inventory;

public interface InventoryCheck {
  boolean matches(Inventory inventory);
}
