package dev.ftb.bloodmagicteams.events;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.config.BMTeamsConfig;
import dev.ftb.bloodmagicteams.data.PlayerBindingData;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingMode;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingPreference;
import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import dev.ftb.bloodmagicteams.network.OpenBindingScreenPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.breakinblocks.neovitae.common.datacomponent.BMDataComponents;
import com.breakinblocks.neovitae.common.datacomponent.Binding;
import com.breakinblocks.neovitae.common.event.ItemBindEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles events related to team binding functionality.
 * Intercepts BloodMagic's ItemBindEvent to provide team binding options.
 */
public class TeamEventHandler {

    // Track players that we're waiting for UI response on
    private static final Set<UUID> pendingBindingPlayers = new HashSet<>();

    /**
     * Intercepts BloodMagic's ItemBindEvent at HIGHEST priority.
     * This fires BEFORE BloodMagic sets the binding, so if we cancel it,
     * BloodMagic won't apply its default player binding.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemBind(ItemBindEvent event) {
        if (!BMTeamsConfig.ENABLE_TEAM_BINDING.get()) {
            return;
        }

        Player player = event.getNewOwner();
        if (player.level().isClientSide()) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack stack = event.getBindingStack();

        // Check if player is on a team
        if (!TeamsIntegration.isOnTeam(serverPlayer)) {
            BloodMagicTeams.LOGGER.debug("Player {} not on team, allowing normal binding",
                    player.getName().getString());
            return; // Not on team, let normal binding happen
        }

        // Check if player can bind to their team (team property restriction)
        if (!TeamsIntegration.canBindToTeam(serverPlayer)) {
            BloodMagicTeams.LOGGER.debug("Player {} restricted from team binding, allowing personal binding",
                    player.getName().getString());
            return;
        }

        // Check player's binding preference
        BindingPreference pref = PlayerBindingData.getPreference(player);
        BloodMagicTeams.LOGGER.debug("Player {} preference: {}", player.getName().getString(), pref);

        if (pref != null && pref.dontAsk) {
            if (pref.mode == BindingMode.TEAM) {
                // Cancel BloodMagic's binding and apply team binding
                event.setCanceled(true);
                applyTeamBinding(serverPlayer, stack, pref);
                BloodMagicTeams.LOGGER.info("Applied team binding for {} (saved preference)",
                        player.getName().getString());
            } else {
                // PERSONAL mode - let BloodMagic handle it normally
                BloodMagicTeams.LOGGER.debug("Player {} has PERSONAL preference, allowing normal binding",
                        player.getName().getString());
            }
            return;
        }

        // No preference set - need to show UI
        if (BMTeamsConfig.SHOW_BINDING_UI.get()) {
            // Cancel BloodMagic's binding - we'll handle it when player makes choice
            event.setCanceled(true);

            // Mark as pending
            pendingBindingPlayers.add(player.getUUID());

            // Send packet to open UI
            Optional<String> teamName = TeamsIntegration.getTeamName(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new OpenBindingScreenPayload(teamName.orElse("Unknown Team")));

            BloodMagicTeams.LOGGER.info("Showing binding UI to {} for team {}",
                    player.getName().getString(), teamName.orElse("Unknown"));
        }
    }

    /**
     * Applies team binding to an item.
     */
    private void applyTeamBinding(ServerPlayer player, ItemStack stack, BindingPreference pref) {
        Optional<UUID> teamId = pref.targetTeamId != null
                ? Optional.of(pref.targetTeamId)
                : TeamsIntegration.getTeamId(player);

        if (teamId.isPresent()) {
            String teamName = TeamsIntegration.getTeamNameByUuid(teamId.get());
            if (teamName == null) {
                teamName = "Unknown Team";
            }

            Binding teamBinding = new Binding(teamId.get(), teamName);
            stack.set(BMDataComponents.BINDING.get(), teamBinding);

            BloodMagicTeams.LOGGER.debug("Set team binding on stack: owner={}, name={}",
                    teamId.get(), teamName);
        } else {
            BloodMagicTeams.LOGGER.warn("Could not find team for player {} to apply team binding",
                    player.getName().getString());
        }
    }

    /**
     * Applies team binding for a player who just chose TEAM in the UI.
     */
    public static void applyTeamBindingFromUI(ServerPlayer player, ItemStack stack) {
        Optional<UUID> teamId = TeamsIntegration.getTeamId(player);

        if (teamId.isPresent()) {
            String teamName = TeamsIntegration.getTeamNameByUuid(teamId.get());
            if (teamName == null) {
                teamName = "Unknown Team";
            }

            Binding teamBinding = new Binding(teamId.get(), teamName);
            stack.set(BMDataComponents.BINDING.get(), teamBinding);

            BloodMagicTeams.LOGGER.info("Applied team binding from UI for {} to team {}",
                    player.getName().getString(), teamName);
        } else {
            BloodMagicTeams.LOGGER.warn("Could not find team for player {} when applying from UI",
                    player.getName().getString());
        }
    }

    /**
     * Applies personal binding for a player who just chose PERSONAL in the UI.
     */
    public static void applyPersonalBindingFromUI(ServerPlayer player, ItemStack stack) {
        Binding personalBinding = new Binding(player.getUUID(), player.getName().getString());
        stack.set(BMDataComponents.BINDING.get(), personalBinding);

        BloodMagicTeams.LOGGER.info("Applied personal binding from UI for {}",
                player.getName().getString());
    }

    /**
     * Clears pending state for a player (called when they make a choice or disconnect).
     */
    public static void clearPending(UUID playerId) {
        pendingBindingPlayers.remove(playerId);
    }

    /**
     * Checks if a player has a pending binding choice.
     */
    public static boolean isPending(UUID playerId) {
        return pendingBindingPlayers.contains(playerId);
    }
}
