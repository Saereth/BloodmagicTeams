package com.breakinblocks.neovitaeteams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.breakinblocks.neovitae.api.soul.SoulTicket;
import com.breakinblocks.neovitae.common.datacomponent.SoulNetwork;
import com.breakinblocks.neovitae.util.helper.SoulNetworkHelper;
import com.breakinblocks.neovitaeteams.NeoVitaeTeams;
import com.breakinblocks.neovitaeteams.data.PlayerBindingData;
import com.breakinblocks.neovitaeteams.data.PlayerBindingData.BindingMode;
import com.breakinblocks.neovitaeteams.integration.TeamsIntegration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Commands for managing binding mode preferences.
 */
public class NVTeamsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neovitaeteams")
                .then(Commands.literal("self")
                        .executes(NVTeamsCommands::setSelfBinding))
                .then(Commands.literal("team")
                        .executes(NVTeamsCommands::setTeamBinding)
                        .then(Commands.argument("teamId", StringArgumentType.word())
                                .suggests(NVTeamsCommands::suggestTeams)
                                .executes(NVTeamsCommands::setSpecificTeamBinding)))
                .then(Commands.literal("reset")
                        .executes(NVTeamsCommands::resetPreferences))
                .then(Commands.literal("status")
                        .executes(NVTeamsCommands::showStatus))
                .then(Commands.literal("network")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("team", StringArgumentType.word())
                                .suggests(NVTeamsCommands::suggestAllTeams)
                                .then(Commands.literal("query")
                                        .executes(NVTeamsCommands::networkQuery))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                .executes(NVTeamsCommands::networkSet)))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                .executes(NVTeamsCommands::networkAdd)))
                                .then(Commands.literal("reset")
                                        .executes(NVTeamsCommands::networkReset))))
        );

        NeoVitaeTeams.LOGGER.debug("Registered /neovitaeteams command");
    }

    private static int setSelfBinding(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.player_only"));
            return 0;
        }

        PlayerBindingData.setBindingMode(player, BindingMode.PERSONAL, true);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.set_self"), false);
        return 1;
    }

    private static int setTeamBinding(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.player_only"));
            return 0;
        }

        if (!TeamsIntegration.isOnTeam(player)) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.not_on_team"));
            return 0;
        }

        PlayerBindingData.setBindingMode(player, BindingMode.TEAM, true);
        Optional<String> teamName = TeamsIntegration.getTeamName(player);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.set_team", teamName.orElse("Unknown")), false);
        return 1;
    }

    private static int setSpecificTeamBinding(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.player_only"));
            return 0;
        }

        String teamIdStr = StringArgumentType.getString(context, "teamId");
        UUID teamId;
        try {
            teamId = UUID.fromString(teamIdStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.invalid_team_id"));
            return 0;
        }

        // Verify team exists
        String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
        if (teamName == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.team_not_found"));
            return 0;
        }

        PlayerBindingData.setBindingMode(player, BindingMode.TEAM, true, teamId);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.set_specific_team", teamName), false);
        return 1;
    }

    private static int resetPreferences(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.player_only"));
            return 0;
        }

        PlayerBindingData.resetPreferences(player);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.reset_success"), false);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.player_only"));
            return 0;
        }

        PlayerBindingData.BindingPreference pref = PlayerBindingData.getPreference(player);

        if (pref == null || !pref.dontAsk) {
            context.getSource().sendSuccess(() ->
                    Component.translatable("commands.neovitaeteams.status_ask"), false);
        } else if (pref.mode == BindingMode.PERSONAL) {
            context.getSource().sendSuccess(() ->
                    Component.translatable("commands.neovitaeteams.status_self"), false);
        } else if (pref.targetTeamId != null) {
            String teamName = TeamsIntegration.getTeamNameByUuid(pref.targetTeamId);
            String displayName = teamName != null ? teamName : pref.targetTeamId.toString();
            context.getSource().sendSuccess(() ->
                    Component.translatable("commands.neovitaeteams.status_specific_team", displayName), false);
        } else {
            context.getSource().sendSuccess(() ->
                    Component.translatable("commands.neovitaeteams.status_team"), false);
        }

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestTeams(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> teamIds = getAvailableTeamIds(context.getSource());
        return SharedSuggestionProvider.suggest(teamIds, builder);
    }

    private static List<String> getAvailableTeamIds(CommandSourceStack source) {
        List<String> teams = new ArrayList<>();

        // Add the player's own team if they're on one
        ServerPlayer player = source.getPlayer();
        if (player != null && TeamsIntegration.isOnTeam(player)) {
            TeamsIntegration.getTeamId(player).ifPresent(id -> teams.add(id.toString()));
        }

        return teams;
    }

    // --- Network subcommand methods (OP only) ---

    private static CompletableFuture<Suggestions> suggestAllTeams(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Map<UUID, String> allTeams = TeamsIntegration.getAllTeams();
        List<String> suggestions = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : allTeams.entrySet()) {
            suggestions.add(entry.getKey().toString());
        }
        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    /**
     * Resolves the team UUID from the "team" argument string.
     * Accepts either a UUID directly or a team name.
     * @return the team UUID, or null if not found (sends failure message)
     */
    private static UUID resolveTeamId(CommandContext<CommandSourceStack> context) {
        String teamArg = StringArgumentType.getString(context, "team");

        // Try parsing as UUID first
        try {
            UUID teamId = UUID.fromString(teamArg);
            String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
            if (teamName != null) {
                return teamId;
            }
        } catch (IllegalArgumentException ignored) {
        }

        // Try matching by team name
        Map<UUID, String> allTeams = TeamsIntegration.getAllTeams();
        for (Map.Entry<UUID, String> entry : allTeams.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(teamArg)) {
                return entry.getKey();
            }
        }

        context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.team_not_found"));
        return null;
    }

    private static int networkQuery(CommandContext<CommandSourceStack> context) {
        UUID teamId = resolveTeamId(context);
        if (teamId == null) return 0;

        String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
        SoulNetwork network = SoulNetworkHelper.getSoulNetwork(teamId);
        if (network == null) {
            context.getSource().sendSuccess(() ->
                    Component.translatable("commands.neovitaeteams.network.query", teamName, 0), true);
            return 1;
        }

        int amount = network.getCurrentEssence();
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.network.query", teamName, amount), true);
        return 1;
    }

    private static int networkSet(CommandContext<CommandSourceStack> context) {
        UUID teamId = resolveTeamId(context);
        if (teamId == null) return 0;

        String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        SoulNetwork network = SoulNetworkHelper.getSoulNetwork(teamId);
        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.network.error"));
            return 0;
        }

        int setAmount = network.set(SoulTicket.create(amount), Integer.MAX_VALUE);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.network.set", teamName, setAmount), true);
        return 1;
    }

    private static int networkAdd(CommandContext<CommandSourceStack> context) {
        UUID teamId = resolveTeamId(context);
        if (teamId == null) return 0;

        String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        SoulNetwork network = SoulNetworkHelper.getSoulNetwork(teamId);
        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.network.error"));
            return 0;
        }

        int added = network.add(SoulTicket.create(amount), Integer.MAX_VALUE);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.network.add", added, teamName), true);
        return 1;
    }

    private static int networkReset(CommandContext<CommandSourceStack> context) {
        UUID teamId = resolveTeamId(context);
        if (teamId == null) return 0;

        String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
        SoulNetwork network = SoulNetworkHelper.getSoulNetwork(teamId);
        if (network == null) {
            context.getSource().sendFailure(Component.translatable("commands.neovitaeteams.network.error"));
            return 0;
        }

        network.set(SoulTicket.create(0), Integer.MAX_VALUE);
        context.getSource().sendSuccess(() ->
                Component.translatable("commands.neovitaeteams.network.reset", teamName), true);
        return 1;
    }
}
