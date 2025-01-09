package dev.anhcraft.timedmmoitems.config;

import dev.anhcraft.config.meta.*;
import dev.anhcraft.config.meta.Optional;
import java.text.SimpleDateFormat;
import java.util.*;

public class Config {
  private static final long DAY = 60 * 60 * 24;
  private static final long HOUR = 60 * 60;
  private static final long MINUTE = 60;

  @Describe("Expiry period stat format")
  public String expiryPeriodFormat;

  @Describe("Expiry date stat format")
  public String expiryDateFormat;

  @Describe("Item expired message")
  public String itemExpired;

  @Describe("Item expired message placement")
  public String itemExpiredPlacement;

  @Describe("Should remove expired item")
  public boolean removeExpiredItem = true;

  @Describe("Expired item removed message")
  public String expiredItemRemoved;

  @Describe("Replaced item dropped on the ground message")
  public String replacedItemDropped;

  @Describe("Unit format")
  @Optional
  public UnitConfig unitFormat = new UnitConfig();

  @Describe("Item check interval in seconds")
  public int itemCheckInterval = 5;

  @Describe("Replace expiry period with expiry date instead of keeping both stats")
  public boolean replaceExpiryPeriod = true;

  @Describe("Date format")
  private String dateFormat;

  @Exclude public SimpleDateFormat simpleDateFormat;

  @Describe(
      "Inventory update is usually handled automatically. Enable this if you experience glitch!")
  public boolean forceUpdateInventory = false;

  public boolean enableBypassPermission = false;

  @Describe("Replace items of certain mi types with another bukkit or mi items upon expiration")
  @Optional
  public Map<String, List<ItemConfig>> expiredTypeReplace = new HashMap<>();

  @Describe(
      "Replace specific items with another bukkit or mi items upon expiration. Prioritized above"
          + " replacement by type.")
  @Optional
  public Map<String, List<ItemConfig>> expiredItemReplace = new HashMap<>();

  @Describe("Debug level")
  public int debugLevel;

  @Denormalizer(
      value = {"dateFormat"},
      strategy = Denormalizer.Strategy.AFTER)
  private void init(String dateFormat) {
    simpleDateFormat = new SimpleDateFormat(dateFormat);
  }

  public String formatDate(Date date) {
    return simpleDateFormat.format(date);
  }

  public String formatDuration(long seconds) {
    long days = seconds / DAY;
    seconds = Math.max(0, seconds - days * DAY);
    long hours = seconds / HOUR;
    seconds = Math.max(0, seconds - hours * HOUR);
    long minutes = seconds / MINUTE;
    seconds = Math.max(0, seconds - minutes * MINUTE);

    List<String> args = new ArrayList<>();
    if (days == 1) args.add(unitFormat.day.replace("%d", Long.toString(days)));
    else if (days > 0) args.add(unitFormat.days.replace("%d", Long.toString(days)));

    if (hours == 1) args.add(unitFormat.hour.replace("%d", Long.toString(hours)));
    else if (hours > 0) args.add(unitFormat.hours.replace("%d", Long.toString(hours)));

    if (minutes == 1) args.add(unitFormat.minute.replace("%d", Long.toString(minutes)));
    else if (minutes > 0) args.add(unitFormat.minutes.replace("%d", Long.toString(minutes)));

    if (seconds == 1) args.add(unitFormat.second.replace("%d", Long.toString(seconds)));
    else if (seconds > 0) args.add(unitFormat.seconds.replace("%d", Long.toString(seconds)));

    return String.join(" ", args);
  }
}
