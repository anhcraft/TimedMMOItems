package dev.anhcraft.timedmmoitems.task;

import static dev.anhcraft.timedmmoitems.TimedMMOItems.EXPIRY_DATE;
import static dev.anhcraft.timedmmoitems.TimedMMOItems.EXPIRY_PERIOD;

import dev.anhcraft.timedmmoitems.TimedMMOItems;
import dev.anhcraft.timedmmoitems.config.ItemConfig;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.*;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import org.bukkit.ChatColor;
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
      if (player.hasPermission("timeditems.bypass")) {
        continue;
      }

      itemsDroppedFlag = false;

      boolean needUpdate = false;
      int rmvCounter = 0;

      boolean needItemReplace = false;
      int rplItemsCounter = 0;
      HashMap<String, Integer> rplItemsMap = new HashMap<>();

      boolean needTypeReplace = false;
      int rplTypesCounter = 0;
      HashMap<String, Integer> rplTypesMap = new HashMap<>();

      List<ItemStack> newItems = new LinkedList<>();

      for (ItemStack item : player.getInventory().getContents()) {
        if (item == null
            || item.getType().isAir()
            || !item.hasItemMeta()
            || NBTItem.get(item).getType() == null) {
          newItems.add(item);
          continue;
        }

        LiveMMOItem mmo = new LiveMMOItem(item);

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

          newItems.add(mmo.newBuilder().build());
          needUpdate = true;
          continue;
        }

        if (plugin.config.removeExpiredItem
            && mmo.hasData(EXPIRY_DATE)
            && ((DoubleData) mmo.getData(EXPIRY_DATE)).getValue() < System.currentTimeMillis()) {
          rmvCounter += item.getAmount();
          needUpdate = true;
        }
        if (!plugin.config.expiredTypeReplace.isEmpty()) {
          Type type = mmo.getType();
          String typeName =
              type.toString()
                  .substring(type.toString().indexOf("id='") + 4, type.toString().lastIndexOf("'"));
          if (plugin.config.expiredTypeReplace.containsKey(typeName)) {
            rplTypesCounter += item.getAmount();
            rplTypesMap.put(typeName, rplTypesCounter);
            needTypeReplace = true;
            needUpdate = true;
          }
        }
        if (!plugin.config.expiredItemReplace.isEmpty()
            && plugin.config.expiredItemReplace.containsKey(mmo.getId())) {
          rplItemsCounter += item.getAmount();
          rplItemsMap.put(mmo.getId(), rplItemsCounter);
          needItemReplace = true;
          needUpdate = true;
        }
      }

      if (!needUpdate) return;
      player.getInventory().setContents(newItems.toArray(new ItemStack[0]));

      if (plugin.config.forceUpdateInventory) player.updateInventory();

      if (rmvCounter > 0) {
        String msg =
            plugin.config.expiredItemRemoved.replace("%amount%", Integer.toString(rmvCounter));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
      }
      if (needItemReplace) itemReplace(player, rplItemsMap, false);
      if (needTypeReplace) itemReplace(player, rplTypesMap, true);
      if (itemsDroppedFlag) {
        String msg = plugin.config.replacedItemDropped;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
      }
    }
  }

  private void itemReplace(Player player, HashMap<String, Integer> map, Boolean replaceByType) {
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      String itemId = entry.getKey();
      int rplCounter = entry.getValue();
      List<ItemConfig> itemConfigList;

      itemConfigList =
          Boolean.TRUE.equals(replaceByType)
              ? plugin.config.expiredTypeReplace.get(itemId)
              : plugin.config.expiredItemReplace.get(itemId);
      for (ItemConfig itemConfig : itemConfigList) {
        try {
          ItemStack itemStack = ReplaceFactory.createItemStack(itemConfig, rplCounter);
          // Attempt to add to inventory
          HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack);
          if (!leftovers.isEmpty()) {
            // Drop leftovers on the ground
            for (ItemStack leftover : leftovers.values()) {
              player.getWorld().dropItem(player.getLocation(), leftover);
            }
            itemsDroppedFlag = true;
          }
          TimedMMOItems.plugin.debug(
              3,
              "%s item (%s) has been replaced by (%s). Replacement by type: %b. Leftovers on the"
                  + " ground: %b",
              player.getName(),
              itemId,
              itemConfig,
              replaceByType,
              !leftovers.isEmpty());
        } catch (IllegalArgumentException e) {
          plugin.getLogger().severe(e.getMessage());
        }
      }
    }
  }
}
