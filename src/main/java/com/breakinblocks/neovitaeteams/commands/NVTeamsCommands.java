package com.breakinblocks.neovitaeteams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
}
