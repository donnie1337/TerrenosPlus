package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class TerrenosGUI {

    public static final int SLOT_LIST = 11;
    public static final int SLOT_MANAGE = 13;
    public static final int SLOT_MARCOS = 15;

    public static final int SLOT_EXPAND = 11;
    public static final int SLOT_VISITOR_FLY = 13;
    public static final int SLOT_EXPLOSIONS = 15;
    public static final int SLOT_TRUSTED = 20;
    public static final int SLOT_BACK = 22;
    public static final int SLOT_TRUSTED_BACK = 49;

    private TerrenosGUI() {
    }

    public static Inventory main(Player player, TerrenoManager manager) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.MAIN, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Terrenos");
        holder.setInventory(inventory);

        List<Terreno> own = manager.getByOwner(player.getUniqueId());
        inventory.setItem(SLOT_LIST, item(
                Material.COMPASS,
                "&b&lʟɪsᴛᴀ ᴅᴇ ᴛᴇʀʀᴇɴᴏs",
                List.of(
                        "",
                        "&fVisualize todos os seus",
                        "&fterrenos protegidos",
                        "",
                        "&fSeus terrenos: &e" + own.size(),
                        "",
                        "&aClique para ver seus terrenos"
                )
        ));

        inventory.setItem(SLOT_MANAGE, item(
                Material.COMMAND_BLOCK,
                "&b&lɢᴇʀᴇɴᴄɪᴀʀ ᴛᴇʀʀᴇɴᴏ",
                List.of(
                        "",
                        "&fModifique permissões e controle",
                        "&fquem pode acessar seu terreno",
                        "",
                        "&aClique para gerenciar o terreno"
                )
        ));

        inventory.setItem(SLOT_MARCOS, item(
                Material.SUNFLOWER,
                "&b&lᴍᴀʀᴄᴏs",
                List.of(
                        "",
                        "&fMarcos permitem expandir",
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

    public static Inventory manage(Player player, Terreno terrain, MarcoManager marcos, TerrenosPlus plugin) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.MANAGE, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, "Gerenciar Terreno");
        holder.setInventory(inventory);

        int costPerBlock = Math.max(1, plugin.getConfig().getInt("claims.expansion.marcos-per-block", 1));
        long northSouthCost = (long) terrain.width() * costPerBlock;
        long eastWestCost = (long) terrain.depth() * costPerBlock;
        int balance = marcos.getBalance(player.getUniqueId());

        inventory.setItem(SLOT_EXPAND, item(
                Material.GRASS_BLOCK,
                "&b&lEXPANDIR TERRENO",
                List.of(
                        "",
                        "&fUse o comando:",
                        "&e/terreno expandir <direção> <quantidade>",
                        "",
                        "&7Exemplo: &f/terreno expandir sul 40",
                        "",
                        "&7Uma faixa completa representa:",
                        "&fNorte/Sul: &e" + northSouthCost + " blocos",
                        "&fLeste/Oeste: &e" + eastWestCost + " blocos",
                        "",
                        "&7Até completar 10x10: &aGRÁTIS",
                        "",
                        "&7Seu saldo: &e" + balance + " Marcos",
                        "",
                        "&aClique para ver a instrução no chat"
                )
        ));

        boolean visitorFly = terrain.visitorFlyEnabled();
        inventory.setItem(SLOT_VISITOR_FLY, item(
                visitorFly ? Material.FEATHER : Material.IRON_BARS,
                "&b&lFLY DE VISITANTES",
                List.of(
                        "",
                        "&fControla o fly de outros jogadores",
                        "&fque possuem permissão para voar.",
                        "",
                        "&7Dono do terreno: &asempre permitido",
                        "&7Staffs: &asempre permitidos",
                        "",
                        "&7Estado: " + (visitorFly ? "&aATIVADO" : "&cDESATIVADO"),
                        "",
                        "&eClique para " + (visitorFly ? "desativar" : "ativar")
                )
        ));

        boolean explosions = terrain.explosionsEnabled();
        inventory.setItem(SLOT_EXPLOSIONS, item(
                explosions ? Material.TNT : Material.OBSIDIAN,
                "&b&lEXPLOSÕES",
                List.of(
                        "",
                        "&fControle explosões de TNT, Creepers",
                        "&fe outras fontes dentro do terreno.",
                        "",
                        "&7Estado: " + (explosions ? "&aATIVADAS" : "&cDESATIVADAS"),
                        "",
                        "&eClique para " + (explosions ? "desativar" : "ativar")
                )
        ));

        inventory.setItem(SLOT_TRUSTED, item(
                terrain.trustedPlayers().isEmpty() ? Material.GRAY_DYE : Material.PLAYER_HEAD,
                "&b&lJOGADORES CONFIÁVEIS",
                List.of(
                        "",
                        "&fVeja quem possui trust",
                        "&fneste terreno.",
                        "",
                        "&7Jogadores com trust: &e" + terrain.trustedPlayers().size(),
                        "",
                        terrain.trustedPlayers().isEmpty()
                                ? "&7Nenhum jogador possui trust."
                                : "&aClique para visualizar"
                )
        ));

        inventory.setItem(SLOT_BACK, item(
                Material.ARROW,
                "&c&lVOLTAR",
                List.of("", "&7Clique para voltar.")
        ));

        return inventory;
    }

    public static Inventory trustedPlayers(Terreno terrain) {
        TerrenosGUIHolder holder = new TerrenosGUIHolder(TerrenosGUIHolder.View.TRUSTED, 0);
        Inventory inventory = Bukkit.createInventory(holder, 54, "Jogadores Confiáveis");
        holder.setInventory(inventory);

        List<OfflinePlayer> trusted = new ArrayList<>();
        for (UUID uuid : terrain.trustedPlayers()) {
            trusted.add(Bukkit.getOfflinePlayer(uuid));
        }
        trusted.sort(Comparator.comparing(
                player -> player.getName() == null ? player.getUniqueId().toString() : player.getName(),
                String.CASE_INSENSITIVE_ORDER
        ));

        if (trusted.isEmpty()) {
            inventory.setItem(22, item(
                    Material.GRAY_DYE,
                    "&7&lNENHUM JOGADOR",
                    List.of(
                            "",
                            "&7Nenhum jogador possui trust",
                            "&7neste terreno.",
                            "",
                            "&eUse /terreno trust <nickname>"
                    )
            ));
        } else {
            int slot = 0;
            for (OfflinePlayer trustedPlayer : trusted) {
                if (slot >= 45) break;
                inventory.setItem(slot++, trustedHead(trustedPlayer));
            }
        }

        inventory.setItem(SLOT_TRUSTED_BACK, item(
                Material.ARROW,
                "&c&lVOLTAR",
                List.of("", "&7Clique para voltar ao gerenciamento.")
        ));

        return inventory;
    }

    private static ItemStack trustedHead(OfflinePlayer player) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = stack.getItemMeta();
        if (rawMeta instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            String name = player.getName() == null
                    ? player.getUniqueId().toString().substring(0, 8)
                    : player.getName();
            meta.setDisplayName(color("&a" + name));
            meta.setLore(List.of(
                    color(""),
                    color("&7Este jogador possui acesso"),
                    color("&7confiável a este terreno."),
                    color(""),
                    color("&eRemova com /terreno untrust " + name)
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(TerrenosGUI::color).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
