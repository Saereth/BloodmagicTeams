package dev.ftb.bloodmagicteams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.data.PlayerBindingData;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingMode;
import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
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
public class BMTeamsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bloodmagicteams")
                .then(Commands.literal("self")
                        .executes(BMTeamsCommands::setSelfBinding))
                .then(Commands.literal("team")
                        .executes(BMTeamsCommands::setTeamBinding)
                        .then(Commands.argument("teamId", StringArgumentType.word())
                                .suggests(BMTeamsCommands::suggestTeams)
                                .executes(BMTeamsCommands::setSpecificTeamBinding)))
                .then(Commands.literal("reset")
                        .executes(BMTeamsCommands::resetPreferences))
                .then(Commands.literal("status")
                        .executes(BMTeamsCommands::showStatus))
        );
        
        BloodMagicTeams.LOGGER.debug("Registered /bloodmagicteams command");
    }

    private static int setSelfBinding(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.player_only"));
            return 0;
        }

        PlayerBindingData.setBindingMode(player, BindingMode.PERSONAL, true);
        context.getSource().sendSuccess(() -> 
                Component.translatable("commands.bloodmagicteams.set_self"), false);
        return 1;
    }

    private static int setTeamBinding(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.player_only"));
            return 0;
        }

        if (!TeamsIntegration.isOnTeam(player)) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.not_on_team"));
            return 0;
        }

        PlayerBindingData.setBindingMode(player, BindingMode.TEAM, true);
        Optional<String> teamName = TeamsIntegration.getTeamName(player);
        context.getSource().sendSuccess(() -> 
                Component.translatable("commands.bloodmagicteams.set_team", teamName.orElse("Unknown")), false);
        return 1;
    }

    private static int setSpecificTeamBinding(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.player_only"));
            return 0;
        }

        String teamIdStr = StringArgumentType.getString(context, "teamId");
        UUID teamId;
        try {
            teamId = UUID.fromString(teamIdStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.invalid_team_id"));
            return 0;
        }

        // Verify team exists
        String teamName = TeamsIntegration.getTeamNameByUuid(teamId);
        if (teamName == null) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.team_not_found"));
            return 0;
        }

        PlayerBindingData.setBindingMode(player, BindingMode.TEAM, true, teamId);
        context.getSource().sendSuccess(() -> 
                Component.translatable("commands.bloodmagicteams.set_specific_team", teamName), false);
        return 1;
    }

    private static int resetPreferences(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.player_only"));
            return 0;
        }

        PlayerBindingData.resetPreferences(player);
        context.getSource().sendSuccess(() -> 
                Component.translatable("commands.bloodmagicteams.reset_success"), false);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("commands.bloodmagicteams.player_only"));
            return 0;
        }

        PlayerBindingData.BindingPreference pref = PlayerBindingData.getPreference(player);
        
        if (pref == null || !pref.dontAsk) {
            context.getSource().sendSuccess(() -> 
                    Component.translatable("commands.bloodmagicteams.status_ask"), false);
        } else if (pref.mode == BindingMode.PERSONAL) {
            context.getSource().sendSuccess(() -> 
                    Component.translatable("commands.bloodmagicteams.status_self"), false);
        } else if (pref.targetTeamId != null) {
            String teamName = TeamsIntegration.getTeamNameByUuid(pref.targetTeamId);
            String displayName = teamName != null ? teamName : pref.targetTeamId.toString();
            context.getSource().sendSuccess(() -> 
                    Component.translatable("commands.bloodmagicteams.status_specific_team", displayName), false);
        } else {
            context.getSource().sendSuccess(() -> 
                    Component.translatable("commands.bloodmagicteams.status_team"), false);
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
