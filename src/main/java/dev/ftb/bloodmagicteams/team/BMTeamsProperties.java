package dev.ftb.bloodmagicteams.team;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import dev.ftb.mods.ftbteams.api.property.EnumProperty;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Registers and manages FTB Teams properties for BloodMagic Teams.
 */
public class BMTeamsProperties {

    public static final ResourceLocation BINDING_RESTRICTION_ID =
            new ResourceLocation(BloodMagicTeams.MOD_ID, "binding_restriction");

    // Property for binding restriction - uses EnumProperty for dropdown UI
    public static final EnumProperty BINDING_RESTRICTION = new EnumProperty(
            BINDING_RESTRICTION_ID,
            () -> BindingRestriction.MEMBER.getSerializedName(),
            List.of(
                    BindingRestriction.MEMBER.getSerializedName(),
                    BindingRestriction.OFFICER.getSerializedName()
            ),
            Map.of(
                    BindingRestriction.MEMBER.getSerializedName(),
                    Component.translatable("bloodmagicteams.binding_restriction.member"),
                    BindingRestriction.OFFICER.getSerializedName(),
                    Component.translatable("bloodmagicteams.binding_restriction.officer")
            )
    );

    /**
     * Get the binding restriction for a team.
     */
    public static BindingRestriction getBindingRestriction(Team team) {
        TeamPropertyCollection properties = team.getProperties();
        String value = properties.get(BINDING_RESTRICTION);
        return BindingRestriction.fromString(value);
    }

    /**
     * Check if a player can bind to the team based on their rank.
     * @param team The team
     * @param playerRank The player's rank in the team (from Team.getRankForPlayer)
     * @return true if the player can bind to this team
     */
    public static boolean canPlayerBind(Team team, TeamRank playerRank) {
        BindingRestriction restriction = getBindingRestriction(team);

        if (restriction == BindingRestriction.MEMBER) {
            // Any member can bind
            return true;
        } else if (restriction == BindingRestriction.OFFICER) {
            // Only officers and above can bind
            return playerRank.isAtLeast(TeamRank.OFFICER);
        }

        return true; // Default to allowing
    }

    /**
     * Register the team property using Architectury event system.
     */
    public static void register() {
        TeamEvent.COLLECT_PROPERTIES.register(event -> {
            event.add(BINDING_RESTRICTION);
            BloodMagicTeams.LOGGER.debug("Added team property: {}", BINDING_RESTRICTION_ID);
        });
        BloodMagicTeams.LOGGER.debug("Registered team property listener for: {}", BINDING_RESTRICTION_ID);
    }
}
