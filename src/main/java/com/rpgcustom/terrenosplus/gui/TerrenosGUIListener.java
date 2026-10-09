package com.rpgcustom.terrenosplus.gui;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.listener.VisitorFlyListener;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Optional;

public final class TerrenosGUIListener implements Listener {

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final MarcoManager marcos;
    private final VisitorFlyListener visitorFlyListener;

    public TerrenosGUIListener(TerrenosPlus plugin, TerrenoManager manager, MarcoManager marcos,
                               VisitorFlyListener visitorFlyListener) {
        this.plugin = plugin;
        this.manager = manager;
        this.marcos = marcos;
        this.visitorFlyListener = visitorFlyListener;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TerrenosGUIHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder.view() == TerrenosGUIHolder.View.MAIN) {
            if (slot == TerrenosGUI.SLOT_LIST) {
                player.openInventory(TerrenosListGUI.build(player, manager, 0));
                return;
            }

            if (slot == TerrenosGUI.SLOT_MANAGE) {
                Optional<Terreno> current = manager.find(player.getLocation());
                if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                    player.sendMessage(color("&a[Terrenos] &r&cFique dentro de um terreno seu para gerenciá-lo."));
                    return;
                }

                Terreno t = current.get();
                player.openInventory(TerrenosGUI.manage(player, t, marcos, plugin));
                return;
            }

            if (slot == TerrenosGUI.SLOT_MARCOS) {
                player.openInventory(MarcosGUI.main(player, marcos));
            }
            return;
        }

        if (holder.view() == TerrenosGUIHolder.View.MANAGE) {
            Optional<Terreno> current = manager.find(player.getLocation());
            if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                player.closeInventory();
                player.sendMessage(color("&a[Terrenos] &r&cFique dentro de um terreno seu para gerenciá-lo."));
                return;
            }

            Terreno terrain = current.get();

            if (slot == TerrenosGUI.SLOT_BACK) {
                player.openInventory(TerrenosGUI.main(player, manager));
                return;
            }

            if (slot == TerrenosGUI.SLOT_TRUSTED) {
                player.openInventory(TerrenosGUI.trustedPlayers(terrain));
                return;
            }

            if (slot == TerrenosGUI.SLOT_VISITOR_FLY) {
                boolean enabled = !terrain.visitorFlyEnabled();
                if (!manager.setVisitorFlyEnabled(terrain, enabled)) {
                    plugin.send(player, "messages.visitor-fly-toggle-failed");
                    return;
                }

                visitorFlyListener.refreshTerrain(terrain);
                plugin.send(player,
                        enabled ? "messages.visitor-fly-enabled" : "messages.visitor-fly-disabled");
                player.openInventory(TerrenosGUI.manage(player, terrain, marcos, plugin));
                return;
            }

            if (slot == TerrenosGUI.SLOT_EXPLOSIONS) {
                boolean enabled = !terrain.explosionsEnabled();
                if (!manager.setExplosionsEnabled(terrain, enabled)) {
                    plugin.send(player, "messages.explosion-toggle-failed");
                    return;
                }
                plugin.send(player, enabled
                        ? "messages.explosions-enabled"
                        : "messages.explosions-disabled");
                player.openInventory(TerrenosGUI.manage(player, terrain, marcos, plugin));
                return;
            }

            if (slot == TerrenosGUI.SLOT_EXPAND) {
                player.closeInventory();

                Component command = Component.text("/terreno expandir ")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand("/terreno expandir "))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Clique para preencher o comando")
                                        .color(NamedTextColor.GREEN)));

                Component back = Component.text("AQUI")
                        .color(NamedTextColor.RED)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/terreno"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Clique para voltar ao menu de terrenos")
                                        .color(NamedTextColor.YELLOW)));

                player.sendMessage(Component.empty());
                player.sendMessage(
                        Component.text("[Terrenos]").color(NamedTextColor.GREEN)
                                .append(Component.text(" Expansão de terreno")
                                        .color(NamedTextColor.WHITE))
                );
                player.sendMessage(Component.empty());
                player.sendMessage(
                        Component.text("Digite no chat o comando ").color(NamedTextColor.WHITE)
                                .append(command)
                                .append(Component.text("com a quantidade desejada.")
                                        .color(NamedTextColor.WHITE))
                );
                player.sendMessage(
                        Component.text("Exemplos: ").color(NamedTextColor.GRAY)
                                .append(Component.text("/terreno expandir 5").color(NamedTextColor.WHITE))
                                .append(Component.text(" ou ").color(NamedTextColor.GRAY))
                                .append(Component.text("/terreno expandir norte 5").color(NamedTextColor.WHITE))
                );
                player.sendMessage(Component.empty());
                player.sendMessage(
                        Component.text("Clique ").color(NamedTextColor.GRAY)
                                .append(back)
                                .append(Component.text(" para voltar ao menu.")
                                        .color(NamedTextColor.GRAY))
                );
                player.sendMessage(Component.empty());
                return;
            }

            return;
        }

        if (holder.view() == TerrenosGUIHolder.View.TRUSTED) {
            Optional<Terreno> current = manager.find(player.getLocation());
            if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                player.closeInventory();
                player.sendMessage(color("&a[Terrenos] &r&cFique dentro de um terreno seu para gerenciá-lo."));
                return;
            }

            if (slot == TerrenosGUI.SLOT_TRUSTED_BACK) {
                player.openInventory(TerrenosGUI.manage(player, current.get(), marcos, plugin));
            }
            return;
        }

        if (holder.view() == TerrenosGUIHolder.View.LIST) {
            if (slot == TerrenosListGUI.backSlot(event.getInventory().getSize())) {
                player.openInventory(TerrenosGUI.main(player, manager));
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TerrenosGUIHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
