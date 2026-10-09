package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.service.MarcoManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class MarcosGUI {

    public static final int SLOT_PLAYTIME = 11;
    public static final int SLOT_DAILY = 13;
    public static final int SLOT_MISSIONS = 15;
    public static final int SLOT_PROFILE = 29;
    public static final int SLOT_BACK = 31;
    public static final int SLOT_INFO = 33;

    private MarcosGUI() {
    }

    public static Inventory main(Player player, MarcoManager marcos) {
        MarcosGUIHolder holder = new MarcosGUIHolder(MarcosGUIHolder.View.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 36, "Marcos");
        holder.setInventory(inventory);

        long seconds = marcos.getSecondsUntilHourlyReward(player.getUniqueId());
        long minutes = (seconds + 59L) / 60L;

        inventory.setItem(SLOT_PLAYTIME, TerrenosGUI.item(
                Material.CLOCK,
                "&bTEMPO JOGADO",
                List.of(
                        "",
                        "&fFique online no servidor para",
                        "&freceber Marcos automaticamente.",
                        "",
                        "&fPróxima recompensa: &e~" + minutes + " min"
                )
        ));

        inventory.setItem(SLOT_DAILY, TerrenosGUI.item(
                Material.SUNFLOWER,
                "&bRECOMPENSAS DIÁRIAS",
                List.of(
                        "",
                        "&fEntre todos os dias e avance",
                        "&fpela sequência de recompensas.",
                        "",
                        marcos.canClaimDaily(player.getUniqueId())
                                ? "&aRecompensa disponível!"
                                : "&7Recompensa de hoje coletada.",
                        "",
                        "&aClique para abrir"
                )
        ));

        inventory.setItem(SLOT_MISSIONS, TerrenosGUI.item(
                Material.WRITABLE_BOOK,
                "&bMISSÕES",
                List.of(
                        "",
                        "&fComplete objetivos de mineração,",
                        "&fagricultura, combate e pesca.",
                        "",
                        "&7Sistema de missões será integrado",
                        "&7ao progresso de Marcos."
                )
        ));

        inventory.setItem(SLOT_PROFILE, profile(player, marcos));

        inventory.setItem(SLOT_BACK, TerrenosGUI.item(
                Material.ARROW,
                "&cVoltar",
                List.of("", "&7Voltar ao menu de terrenos")
        ));

        inventory.setItem(SLOT_INFO, TerrenosGUI.item(
                Material.BOOK,
                "&e&lᴄᴏᴍᴏ ғᴜɴᴄɪᴏɴᴀᴍ ᴏs ᴍᴀʀᴄᴏs",
                List.of(
                        "",
                        "&fMarcos são usados para expandir",
                        "&fseus terrenos protegidos.",
                        "",
                        "&7Como conseguir:",
                        "&e▪ &fTempo jogado",
                        "&e▪ &fMissões e objetivos",
                        "&e▪ &fRecompensas diárias",
                        "",
                        "&7A cada 1 hora online:",
                        "&e25, 50 ou 75 Marcos",
                        "",
                        "&8Marcos não são comprados com Coins."
                )
        ));

        return inventory;
    }

    private static ItemStack profile(Player player, MarcoManager marcos) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(TerrenosGUI.color("&b" + player.getName()));
            meta.setLore(List.of(
                    "",
                    TerrenosGUI.color("&7Informações dos seus Marcos"),
                    "",
                    TerrenosGUI.color("&fSaldo atual: &e" + marcos.getBalance(player.getUniqueId()) + " Marcos"),
                    "",
                    TerrenosGUI.color("&8Marcos são usados para expandir terrenos.")
            ));
            head.setItemMeta(meta);
        }
        return head;
    }
}
