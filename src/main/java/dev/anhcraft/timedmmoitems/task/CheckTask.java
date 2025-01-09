package dev.anhcraft.timedmmoitems.task;

import static dev.anhcraft.timedmmoitems.TimedMMOItems.EXPIRY_DATE;
import static dev.anhcraft.timedmmoitems.TimedMMOItems.EXPIRY_PERIOD;

import dev.anhcraft.timedmmoitems.TimedMMOItems;
import dev.anhcraft.timedmmoitems.config.ItemConfig;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.*;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CheckTask extends BukkitRunnable {
  private final TimedMMOItems plugin;
  private boolean itemsDroppedFlag;

  public CheckTask(TimedMMOItems plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      if (plugin.config.enableBypassPermission && player.hasPermission("timeditems.bypass")) {
        continue;
      }

      boolean dirtyInventory = false;
      int rmvCounter = 0; // This count item to-be-removed (excluding those to-be-replaced)

      Map<String, Integer> rplItemsMap = new HashMap<>(3);
      Map<String, Integer> rplTypesMap = new HashMap<>(3);
      List<ItemStack> newItems = new ArrayList<>(player.getInventory().getSize());

      for (ItemStack item : player.getInventory().getContents()) {
        if (item == null
            || item.getType().isAir()
            // || !item.hasItemMeta() // custom NBT tag might not create ItemMeta
            || NBTItem.get(item).getType() == null) {
          newItems.add(item);
          continue;
        }

        // If mmo become null, the item would be removed
        // If dirty is true, the item would be updated
        LiveMMOItem mmo = new LiveMMOItem(item);
        boolean dirty = false;

        // Check 1: Add EXPIRY_DATE and possibly remove EXPIRY_PERIOD (if required)
        if (mmo.hasData(EXPIRY_PERIOD) && !mmo.hasData(EXPIRY_DATE)) {
          double expiryPeriod = ((DoubleData) mmo.getData(EXPIRY_PERIOD)).getValue();
          double expiryDate = System.currentTimeMillis();
          expiryDate += expiryPeriod * 1000d;
          mmo.setData(EXPIRY_DATE, new DoubleData(expiryDate));

          if (plugin.config.replaceExpiryPeriod) {
            mmo.removeData(EXPIRY_PERIOD);
            TimedMMOItems.plugin.debug(
                1,
                "%s item has EXPIRY_PERIOD(%.1f) but no EXPIRY_DATE => Create EXPIRY_DATE(%.1f),"
                    + " and remove EXPIRY_PERIOD",
                player.getName(),
                expiryPeriod,
                expiryDate);
          } else {
            TimedMMOItems.plugin.debug(
                1,
                "%s item has EXPIRY_PERIOD(%.1f) but no EXPIRY_DATE => Create EXPIRY_DATE(%.1f)",
                player.getName(),
                expiryPeriod,
                expiryDate);
          }

          dirty = true;
          // Do not terminate here. Continue with the next check.
        }

        // Check 2: Handle expired items
        if (mmo.hasData(EXPIRY_DATE)) {
          double expiryDate = ((DoubleData) mmo.getData(EXPIRY_DATE)).getValue();

          if (expiryDate < System.currentTimeMillis()) {

            // Check 2.a. Replace item based on item ID (more specific than type ID)
            if (!plugin.config.expiredItemReplace.isEmpty()) {
              String itemId = mmo.getId();

              if (plugin.config.expiredItemReplace.containsKey(itemId)) {
                rplItemsMap.put(itemId, rplItemsMap.getOrDefault(itemId, 0) + item.getAmount());
                mmo = null;

                TimedMMOItems.plugin.debug(
                    1,
                    "%s item has EXPIRY_DATE(%.1f) before the current (%d) => Replace by ItemId"
                        + " (%s)",
                    player.getName(),
                    expiryDate,
                    System.currentTimeMillis(),
                    itemId);
              }
            }

            // Check 2.b. Replace item based on type ID
            if (mmo != null && !plugin.config.expiredTypeReplace.isEmpty()) {
              String typeId = mmo.getType().getId();

              if (plugin.config.expiredTypeReplace.containsKey(typeId)) {
                rplTypesMap.put(typeId, rplTypesMap.getOrDefault(typeId, 0) + item.getAmount());
                mmo = null;

                TimedMMOItems.plugin.debug(
                    1,
                    "%s item has EXPIRY_DATE(%.1f) before the current (%d) => Replace by TypeId"
                        + " (%s)",
                    player.getName(),
                    expiryDate,
                    System.currentTimeMillis(),
                    typeId);
              }
            }

            // Check 2.c. Remove expired item
            if (mmo != null && plugin.config.removeExpiredItem) {
              rmvCounter += item.getAmount();
              mmo = null;

              TimedMMOItems.plugin.debug(
                  1,
                  "%s item has EXPIRY_DATE(%.1f) before the current (%d) => Remove",
                  player.getName(),
                  expiryDate,
                  System.currentTimeMillis());
            }
          }
        }

        // Apply changes
        if (mmo == null) {
          newItems.add(new ItemStack(Material.AIR));
          dirtyInventory = true;
        } else if (dirty) {
          newItems.add(mmo.newBuilder().build());
          dirtyInventory = true;
        } else {
          newItems.add(item);
        }
      }

      if (dirtyInventory) {
        player.getInventory().setContents(newItems.toArray(new ItemStack[0]));
        if (plugin.config.forceUpdateInventory) player.updateInventory();
      }

      if (rmvCounter > 0) {
        String msg =
            plugin.config.expiredItemRemoved.replace("%amount%", Integer.toString(rmvCounter));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
      }

      int itemDropped = itemReplace(player, rplItemsMap, false);
      itemDropped |= itemReplace(player, rplTypesMap, true);

      if (itemDropped == 1) {
        String msg = plugin.config.replacedItemDropped;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
      }
    }
  }

  private int itemReplace(Player player, Map<String, Integer> map, boolean replaceByType) {
    int itemDropped = 0;

    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      String id = entry.getKey();
      int count = entry.getValue();
      List<ItemConfig> itemConfigList;

      itemConfigList =
          replaceByType
              ? plugin.config.expiredTypeReplace.get(id)
              : plugin.config.expiredItemReplace.get(id);

      for (ItemConfig itemConfig : itemConfigList) {
        try {
          ItemStack itemStack = ReplaceFactory.createItemStack(itemConfig, count);
          HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);

          if (!leftovers.isEmpty()) {
            for (ItemStack leftover : leftovers.values()) {
              player.getWorld().dropItem(player.getLocation(), leftover);
            }
            itemDropped |= 1;
          }

          TimedMMOItems.plugin.debug(
              3,
              "%s item (%s) has been replaced by (%s). Replacement by type: %b. Leftovers on the"
                  + " ground: %d",
              player.getName(),
              id,
              itemConfig,
              replaceByType,
              leftovers.size());
        } catch (IllegalArgumentException e) {
          plugin.getLogger().severe(e.getMessage());
        }
      }
    }

    return itemDropped;
  }
}
