package dev.ftb.bloodmagicteams.integration;

import dev.ftb.bloodmagicteams.team.BMTeamsProperties;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.TeamRank;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation class that directly calls FTB Teams API.
 * This class should ONLY be loaded when FTB Teams is present.
 * All access should go through {@link TeamsIntegration}.
 */
class TeamsIntegrationImpl {

    static Optional<UUID> getTeamId(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager()
                .getTeamForPlayer(player)
                .filter(team -> !team.isPlayerTeam()) // Only return party teams, not solo player teams
                .map(Team::getId);
    }

    static Optional<String> getTeamName(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager()
                .getTeamForPlayer(player)
                .filter(team -> !team.isPlayerTeam())
                .map(team -> team.getName().getString());
    }

    static boolean isOnTeam(ServerPlayer player) {
        return FTBTeamsAPI.api().getManager()
                .getTeamForPlayer(player)
                .filter(team -> !team.isPlayerTeam())
                .isPresent();
    }

    static boolean areOnSameTeam(ServerPlayer player1, ServerPlayer player2) {
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> team1 = manager.getTeamForPlayer(player1).filter(t -> !t.isPlayerTeam());
        Optional<Team> team2 = manager.getTeamForPlayer(player2).filter(t -> !t.isPlayerTeam());

        if (team1.isEmpty() || team2.isEmpty()) {
            return false;
        }

        return team1.get().getId().equals(team2.get().getId());
    }

    static boolean isTeamUuid(UUID uuid) {
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> team = manager.getTeamByID(uuid);
        return team.isPresent() && !team.get().isPlayerTeam();
    }

    @Nullable
    static String getTeamNameByUuid(UUID teamUuid) {
        return FTBTeamsAPI.api().getManager()
                .getTeamByID(teamUuid)
                .map(team -> team.getName().getString())
                .orElse(null);
    }

    /**
     * Check if a player can bind to their team based on the team's restriction setting.
     */
    static boolean canBindToTeam(ServerPlayer player) {
        TeamManager manager = FTBTeamsAPI.api().getManager();
        Optional<Team> teamOpt = manager.getTeamForPlayer(player)
                .filter(team -> !team.isPlayerTeam());

        if (teamOpt.isEmpty()) {
            return false; // Not on a team
        }

        Team team = teamOpt.get();
        TeamRank playerRank = team.getRankForPlayer(player.getUUID());
        return BMTeamsProperties.canPlayerBind(team, playerRank);
    }

    /**
     * Register team properties.
     */
    static void registerProperties() {
        BMTeamsProperties.register();
    }
}
