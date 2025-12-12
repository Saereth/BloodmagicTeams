package dev.ftb.bloodmagicteams.integration;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Soft dependency integration for FTB Teams.
 * All FTB Teams API calls should go through this class to avoid class loading issues.
 */
public class TeamsIntegration {
    private static Boolean teamsLoaded = null;

    public static boolean isTeamsLoaded() {
        if (teamsLoaded == null) {
            teamsLoaded = ModList.get().isLoaded("ftbteams");
        }
        return teamsLoaded;
    }

    /**
     * Gets the team UUID for a player, if they are on a team.
     * @param player The player to check
     * @return Optional containing the team UUID, or empty if player is not on a team
     */
    public static Optional<UUID> getTeamId(ServerPlayer player) {
        if (!isTeamsLoaded()) {
            return Optional.empty();
        }
        return TeamsIntegrationImpl.getTeamId(player);
    }

    /**
     * Gets the team name for a player, if they are on a team.
     * @param player The player to check
     * @return Optional containing the team name, or empty if player is not on a team
     */
    public static Optional<String> getTeamName(ServerPlayer player) {
        if (!isTeamsLoaded()) {
            return Optional.empty();
        }
        return TeamsIntegrationImpl.getTeamName(player);
    }

    /**
     * Checks if a player is on any team.
     * @param player The player to check
     * @return true if the player is on a team
     */
    public static boolean isOnTeam(ServerPlayer player) {
        if (!isTeamsLoaded()) {
            return false;
        }
        return TeamsIntegrationImpl.isOnTeam(player);
    }

    /**
     * Checks if two players are on the same team.
     * @param player1 First player
     * @param player2 Second player
     * @return true if both players are on the same team
     */
    public static boolean areOnSameTeam(ServerPlayer player1, ServerPlayer player2) {
        if (!isTeamsLoaded()) {
            return false;
        }
        return TeamsIntegrationImpl.areOnSameTeam(player1, player2);
    }

    /**
     * Gets the team UUID from a team-bound item's owner UUID.
     * This is used to resolve team ownership from stored binding data.
     * @param ownerUuid The UUID stored in the binding
     * @return true if this UUID corresponds to a team (not a player)
     */
    public static boolean isTeamUuid(UUID ownerUuid) {
        if (!isTeamsLoaded()) {
            return false;
        }
        return TeamsIntegrationImpl.isTeamUuid(ownerUuid);
    }

    /**
     * Gets the display name for a team by its UUID.
     * @param teamUuid The team's UUID
     * @return The team name, or null if not found
     */
    @Nullable
    public static String getTeamNameByUuid(UUID teamUuid) {
        if (!isTeamsLoaded()) {
            return null;
        }
        return TeamsIntegrationImpl.getTeamNameByUuid(teamUuid);
    }

    /**
     * Checks if a player can bind items to their team's soul network.
     * This respects the team's binding restriction property.
     * @param player The player to check
     * @return true if the player can bind to their team, false if restricted
     */
    public static boolean canBindToTeam(ServerPlayer player) {
        if (!isTeamsLoaded()) {
            return false;
        }
        return TeamsIntegrationImpl.canBindToTeam(player);
    }

    /**
     * Registers team properties. Call during mod setup when FTB Teams is loaded.
     */
    public static void registerProperties() {
        if (isTeamsLoaded()) {
            TeamsIntegrationImpl.registerProperties();
        }
    }
}
