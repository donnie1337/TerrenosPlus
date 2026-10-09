package com.rpgcustom.terrenosplus.command;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.gui.TerrenosGUI;
import com.rpgcustom.terrenosplus.gui.TerrenosListGUI;
import com.rpgcustom.terrenosplus.listener.TrackingStickListener;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class TerrenoCommand implements CommandExecutor {

    private static final DateTimeFormatter CREATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final TerrenosPlus plugin;
    private final TerrenoManager manager;
    private final TrackingStickListener trackingStickListener;
    private final MarcoManager marcoManager;

    public TerrenoCommand(TerrenosPlus plugin, TerrenoManager manager,
                          TrackingStickListener trackingStickListener,
                          MarcoManager marcoManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.trackingStickListener = trackingStickListener;
        this.marcoManager = marcoManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando disponível apenas para jogadores.");
            return true;
        }

        if (args.length == 0) {
            Optional<Terreno> current = manager.find(player.getLocation());
            if (current.isEmpty() || !current.get().ownerId().equals(player.getUniqueId())) {
                plugin.send(player, "messages.command-own-claim-only");
                return true;
            }

            player.openInventory(TerrenosGUI.main(player, manager));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno t = terreno.get();
            String createdAt = t.createdAt() > 0L
                    ? CREATION_DATE_FORMAT.format(Instant.ofEpochMilli(t.createdAt()))
                    : "Desconhecida";

            plugin.send(player, "messages.info-owner",
                    "{owner}", t.ownerName(),
                    "{area}", String.valueOf(t.area()),
                    "{created}", createdAt,
                    "{width}", String.valueOf(t.width()),
                    "{depth}", String.valueOf(t.depth()),
                    "{world}", t.world(),
                    "{id}", t.id().toString().substring(0, 8));
            return true;
        }

        if (args[0].equalsIgnoreCase("remover")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno t = terreno.get();
            if (!t.ownerId().equals(player.getUniqueId()) && !player.hasPermission("terrenosplus.admin")) {
                plugin.send(player, "messages.not-owner");
                return true;
            }

            manager.remove(t);
            trackingStickListener.onTerrainRemoved(player, t);
            plugin.send(player, "messages.removed");
            return true;
        }

        if (args[0].equalsIgnoreCase("explosao")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno t = terreno.get();
            if (!t.ownerId().equals(player.getUniqueId()) && !player.hasPermission("terrenosplus.admin")) {
                plugin.send(player, "messages.not-owner");
                return true;
            }

            boolean enabled = !t.explosionsEnabled();
            if (!manager.setExplosionsEnabled(t, enabled)) {
                plugin.send(player, "messages.explosion-toggle-failed");
                return true;
            }

            plugin.send(player,
                    enabled ? "messages.explosions-enabled" : "messages.explosions-disabled");
            return true;
        }

        if (args[0].equalsIgnoreCase("expandir")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno current = terreno.get();
            if (!current.ownerId().equals(player.getUniqueId())) {
                plugin.send(player, "messages.not-owner");
                return true;
            }

            if (args.length < 2) {
                plugin.send(player, "messages.expand-usage");
                return true;
            }

            int requestedMarcos;
            try {
                requestedMarcos = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                plugin.send(player, "messages.expand-invalid");
                return true;
            }

            if (requestedMarcos <= 0) {
                plugin.send(player, "messages.expand-invalid");
                return true;
            }

            TerrenoManager.Direction direction = facingDirection(player.getFacing());

            // O valor informado é a quantidade de Marcos que o jogador quer usar.
            // Para avançar a borda em 1 bloco, é necessário pagar toda a extensão
            // daquela borda: Leste/Oeste usa a profundidade; Norte/Sul usa a largura.
            long borderLength = switch (direction) {
                case EAST, WEST -> current.depth();
                case NORTH, SOUTH -> current.width();
            };

            int costPerBlock = Math.max(1, plugin.getConfig().getInt("claims.expansion.marcos-per-block", 1));
            long costPerLayer = borderLength * costPerBlock;
            if (costPerLayer <= 0L || costPerLayer > Integer.MAX_VALUE) {
                plugin.send(player, "messages.expand-invalid");
                return true;
            }

            if (requestedMarcos < costPerLayer) {
                plugin.send(player, "messages.expand-no-marcos",
                        "{cost}", String.valueOf(costPerLayer),
                        "{balance}", String.valueOf(marcoManager.getBalance(player.getUniqueId())));
                return true;
            }

            int expansionAmount = (int) Math.min(Integer.MAX_VALUE, requestedMarcos / costPerLayer);
            long rawCost = (long) expansionAmount * costPerLayer;
            if (expansionAmount <= 0 || rawCost > Integer.MAX_VALUE) {
                plugin.send(player, "messages.expand-invalid");
                return true;
            }
            int cost = (int) rawCost;

            if (marcoManager.getBalance(player.getUniqueId()) < cost) {
                plugin.send(player, "messages.expand-no-marcos",
                        "{cost}", String.valueOf(cost),
                        "{balance}", String.valueOf(marcoManager.getBalance(player.getUniqueId())));
                return true;
            }

            TerrenoManager.ExpandResult result = manager.expand(current, player, direction, expansionAmount);

            switch (result.type()) {
                case SUCCESS -> {
                    if (!marcoManager.take(player.getUniqueId(), cost)) {
                        plugin.getLogger().warning("Falha ao debitar Marcos após expansão de " + player.getName());
                    }
                    Terreno expanded = result.terreno();
                    trackingStickListener.showExpandedTerrain(player, expanded);
                    plugin.send(player, "messages.expanded",
                            "{amount}", String.valueOf(expansionAmount),
                            "{direction}", directionName(direction),
                            "{cost}", String.valueOf(cost),
                            "{width}", String.valueOf(expanded.width()),
                            "{depth}", String.valueOf(expanded.depth()),
                            "{balance}", String.valueOf(marcoManager.getBalance(player.getUniqueId())));
                }
                case OVERLAP -> plugin.send(player, "messages.expand-overlap",
                        "{owner}", result.overlap().ownerName());
                case TOO_LARGE -> plugin.send(player, "messages.expand-too-large",
                        "{max}", String.valueOf(result.value()));
                case NOT_OWNER -> plugin.send(player, "messages.not-owner");
                case INVALID -> plugin.send(player, "messages.expand-invalid");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("listar")) {
            player.openInventory(TerrenosListGUI.build(player, manager, 0));
            return true;
        }

        player.sendMessage("§e/terreno info §7- mostra o dono do local");
        player.sendMessage("§e/terreno remover §7- remove seu terreno atual");
        player.sendMessage("§e/terreno explosao §7- ativa ou desativa explosões no terreno");
        player.sendMessage("§e/terreno listar §7- lista seus terrenos");
        player.sendMessage("§e/terreno expandir <quantidade> §7- expande na direção que você está olhando");
        return true;
    }

    private TerrenoManager.Direction facingDirection(BlockFace facing) {
        return switch (facing) {
            case NORTH, NORTH_NORTH_EAST, NORTH_NORTH_WEST -> TerrenoManager.Direction.NORTH;
            case SOUTH, SOUTH_SOUTH_EAST, SOUTH_SOUTH_WEST -> TerrenoManager.Direction.SOUTH;
            case EAST, EAST_NORTH_EAST, EAST_SOUTH_EAST -> TerrenoManager.Direction.EAST;
            case WEST, WEST_NORTH_WEST, WEST_SOUTH_WEST -> TerrenoManager.Direction.WEST;
            default -> {
                int x = facing.getModX();
                int z = facing.getModZ();
                if (Math.abs(x) >= Math.abs(z)) {
                    yield x >= 0 ? TerrenoManager.Direction.EAST : TerrenoManager.Direction.WEST;
                }
                yield z >= 0 ? TerrenoManager.Direction.SOUTH : TerrenoManager.Direction.NORTH;
            }
        };
    }

    private String directionName(TerrenoManager.Direction direction) {
        return switch (direction) {
            case NORTH -> "Norte";
            case SOUTH -> "Sul";
            case EAST -> "Leste";
            case WEST -> "Oeste";
        };
    }
}
