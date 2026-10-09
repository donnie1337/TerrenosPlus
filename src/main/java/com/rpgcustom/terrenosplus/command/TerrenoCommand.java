package com.rpgcustom.terrenosplus.command;

import com.rpgcustom.terrenosplus.TerrenosPlus;
import com.rpgcustom.terrenosplus.model.Terreno;
import com.rpgcustom.terrenosplus.gui.TerrenosGUI;
import com.rpgcustom.terrenosplus.gui.TerrenosListGUI;
import com.rpgcustom.terrenosplus.listener.TrackingStickListener;
import com.rpgcustom.terrenosplus.service.MarcoManager;
import com.rpgcustom.terrenosplus.service.TerrenoManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TerrenoCommand implements CommandExecutor, TabCompleter {

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

        if (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) {
            Optional<Terreno> terreno = manager.find(player.getLocation());
            if (terreno.isEmpty()) {
                plugin.send(player, "messages.no-claim");
                return true;
            }

            Terreno t = terreno.get();
            if (!t.ownerId().equals(player.getUniqueId())) {
                plugin.send(player, "messages.not-owner");
                return true;
            }

            boolean trust = args[0].equalsIgnoreCase("trust");
            if (args.length < 2) {
                plugin.send(player, trust ? "messages.trust-usage" : "messages.untrust-usage");
                return true;
            }

            String targetName = args[1];
            Player onlineTarget = plugin.getServer().getPlayerExact(targetName);
            OfflinePlayer target = onlineTarget != null
                    ? onlineTarget
                    : plugin.getServer().getOfflinePlayer(targetName);

            if (!target.isOnline() && !target.hasPlayedBefore()) {
                plugin.send(player, "messages.trust-player-not-found", "{player}", targetName);
                return true;
            }

            UUID targetId = target.getUniqueId();
            String resolvedName = target.getName() != null ? target.getName() : targetName;

            if (targetId.equals(player.getUniqueId())) {
                plugin.send(player, "messages.trust-self");
                return true;
            }

            if (trust) {
                if (t.isTrusted(targetId)) {
                    plugin.send(player, "messages.trust-already", "{player}", resolvedName);
                    return true;
                }
                if (!manager.trust(t, targetId)) {
                    plugin.send(player, "messages.trust-failed", "{player}", resolvedName);
                    return true;
                }
                plugin.send(player, "messages.trust-added", "{player}", resolvedName);
            } else {
                if (!t.isTrusted(targetId)) {
                    plugin.send(player, "messages.untrust-not-trusted", "{player}", resolvedName);
                    return true;
                }
                if (!manager.untrust(t, targetId)) {
                    plugin.send(player, "messages.trust-failed", "{player}", resolvedName);
                    return true;
                }
                plugin.send(player, "messages.trust-removed", "{player}", resolvedName);
            }
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

            if (args.length < 3) {
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

            TerrenoManager.Direction direction = parseDirection(args[2]);
            if (direction == null) {
                plugin.send(player, "messages.expand-direction-invalid");
                return true;
            }

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

            int starterMax = Math.max(1,
                    plugin.getConfig().getInt("claims.initial-maximum-width", 10));
            int currentDimension = switch (direction) {
                case EAST, WEST -> current.width();
                case NORTH, SOUTH -> current.depth();
            };
            int freeLayers = Math.max(0, starterMax - currentDimension);

            // O comando continua usando <marcos> como orçamento equivalente.
            // Porém, enquanto a dimensão ainda não chegou ao tamanho inicial
            // máximo (10 por padrão), as camadas correspondentes são gratuitas.
            int expansionAmount;
            if (requestedMarcos < costPerLayer) {
                if (freeLayers <= 0) {
                    plugin.send(player, "messages.expand-no-marcos",
                            "{cost}", String.valueOf(costPerLayer),
                            "{balance}", String.valueOf(marcoManager.getBalance(player.getUniqueId())));
                    return true;
                }
                expansionAmount = 1;
            } else {
                expansionAmount = (int) Math.min(Integer.MAX_VALUE, requestedMarcos / costPerLayer);
            }

            if (expansionAmount <= 0) {
                plugin.send(player, "messages.expand-invalid");
                return true;
            }

            int paidLayers = Math.max(0, expansionAmount - freeLayers);
            long rawCost = (long) paidLayers * costPerLayer;
            if (rawCost > Integer.MAX_VALUE) {
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
                    if (cost > 0 && !marcoManager.take(player.getUniqueId(), cost)) {
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
        player.sendMessage("§e/terreno trust <nickname> §7- dá acesso ao jogador neste terreno");
        player.sendMessage("§e/terreno untrust <nickname> §7- remove o acesso do jogador");
        player.sendMessage("§e/terreno explosao §7- ativa ou desativa explosões no terreno");
        player.sendMessage("§e/terreno listar §7- lista seus terrenos");
        player.sendMessage("§e/terreno expandir <marcos> <norte|sul|leste|oeste> §7- expande o terreno");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            return filterSuggestions(args[0],
                    List.of("info", "remover", "listar", "trust", "untrust", "explosao", "expandir"));
        }

        if ((args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))
                && args.length == 2) {
            Optional<Terreno> terrain = manager.find(player.getLocation());
            if (terrain.isEmpty() || !terrain.get().ownerId().equals(player.getUniqueId())) {
                return List.of();
            }

            Terreno t = terrain.get();
            List<String> names = new ArrayList<>();

            if (args[0].equalsIgnoreCase("trust")) {
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.getUniqueId().equals(player.getUniqueId())) continue;
                    if (t.isTrusted(online.getUniqueId())) continue;
                    names.add(online.getName());
                }
            } else {
                for (UUID trustedId : t.trustedPlayers()) {
                    OfflinePlayer trusted = plugin.getServer().getOfflinePlayer(trustedId);
                    if (trusted.getName() != null) names.add(trusted.getName());
                }
            }

            names.sort(String.CASE_INSENSITIVE_ORDER);
            return filterSuggestions(args[1], names);
        }

        if (!args[0].equalsIgnoreCase("expandir")) return List.of();

        if (args.length == 2) {
            Optional<Terreno> terrain = manager.find(player.getLocation());
            if (terrain.isEmpty() || !terrain.get().ownerId().equals(player.getUniqueId())) {
                return List.of();
            }

            Terreno t = terrain.get();
            int costPerBlock = Math.max(1, plugin.getConfig().getInt("claims.expansion.marcos-per-block", 1));
            long northSouth = (long) t.width() * costPerBlock;
            long eastWest = (long) t.depth() * costPerBlock;

            Set<String> values = new LinkedHashSet<>();
            addAmountSuggestion(values, northSouth);
            addAmountSuggestion(values, eastWest);
            addAmountSuggestion(values, northSouth * 2L);
            addAmountSuggestion(values, eastWest * 2L);
            return filterSuggestions(args[1], new ArrayList<>(values));
        }

        if (args.length == 3) {
            return filterSuggestions(args[2], List.of("norte", "sul", "leste", "oeste"));
        }

        return List.of();
    }

    private void addAmountSuggestion(Set<String> values, long amount) {
        if (amount > 0L && amount <= Integer.MAX_VALUE) {
            values.add(String.valueOf(amount));
        }
    }

    private List<String> filterSuggestions(String input, List<String> values) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private TerrenoManager.Direction parseDirection(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "norte", "north", "n" -> TerrenoManager.Direction.NORTH;
            case "sul", "south", "s" -> TerrenoManager.Direction.SOUTH;
            case "leste", "east", "e" -> TerrenoManager.Direction.EAST;
            case "oeste", "west", "o" -> TerrenoManager.Direction.WEST;
            default -> null;
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
