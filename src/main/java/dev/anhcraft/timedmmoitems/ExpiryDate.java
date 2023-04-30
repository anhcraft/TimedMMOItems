package dev.anhcraft.timedmmoitems;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemRestriction;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExpiryDate extends DoubleStat implements ItemRestriction {
    public ExpiryDate() {
        super(
                "EXPIRY_DATE", Material.PAINTING, "Expiry Date",
                new String[]{"Defines the expiry date", "The value is in second(s) since Unix Epoch"},
                new String[]{"!block", "all"}
        );
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DoubleData data) {
        double val = data.getValue();
        if (val > 0) {
            String date = TimedMMOItems.plugin.formatDate((long) val);
            String format = Objects.requireNonNull(TimedMMOItems.plugin.config.expiryDateFormat);
            item.getLore().insert(this.getPath(), format.replace("%value%", date));
            item.addItemTag(this.getAppliedNBT(data));
        }
    }

    @Override
    public boolean canUse(RPGPlayer rpgPlayer, NBTItem nbtItem, boolean b) {
        double v = nbtItem.getDouble(getNBTPath());
        if(v > 0 && v < System.currentTimeMillis()) {
            if (rpgPlayer.getPlayer().hasPermission("timeditems.bypass")) {
                return true;
            }
            String t = TimedMMOItems.plugin.config.itemExpiredPlacement;
            String m = ChatColor.translateAlternateColorCodes('&', TimedMMOItems.plugin.config.itemExpired);
            if(t.equalsIgnoreCase("action-bar")) {
                rpgPlayer.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(m));
            } else if(t.equalsIgnoreCase("chat")) {
                rpgPlayer.getPlayer().sendMessage(m);
            }
            rpgPlayer.getPlayer().playSound(rpgPlayer.getPlayer().getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
            return false;
        }
        return true;
    }
}
