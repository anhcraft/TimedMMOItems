package dev.anhcraft.timedmmoitems.config;

import dev.anhcraft.config.meta.Validate;

public class ItemConfig {
  @Validate(value = "not-null")
  public String id;

  public int amount;

  public ItemConfig() {}

  public ItemConfig(String type, String id, int amount) {
    this.id = id;
    this.amount = amount;
  }

  @Override
  public String toString() {
    return "ItemConfig{id='" + id + "', amount=" + amount + "}";
  }
}
