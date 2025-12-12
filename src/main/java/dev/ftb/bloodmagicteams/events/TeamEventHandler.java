package dev.ftb.bloodmagicteams.events;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.config.BMTeamsConfig;
import dev.ftb.bloodmagicteams.data.PlayerBindingData;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingMode;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingPreference;
import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import dev.ftb.bloodmagicteams.network.BMTeamsNetwork;
import dev.ftb.bloodmagicteams.network.OpenBindingScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.util.helper.BindableHelper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles events related to team binding functionality.
 * Intercepts BloodMagic binding to provide team binding options.
 */
public class TeamEventHandler {

    // Track items that we're waiting for UI response on
    private static final Set<UUID> pendingBindingPlayers = new HashSet<>();

    /**
     * Intercepts binding attempts BEFORE BloodMagic processes them.
     * This runs at HIGHEST priority to check if we need to show the binding UI.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerRightClickPre(PlayerInteractEvent.RightClickItem event) {
        if (!BMTeamsConfig.ENABLE_TEAM_BINDING.get()) {
            return;
        }

        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !(held.getItem() instanceof IBindable bindable)) {
            return;
        }

        // Check if item already has a binding
        Binding existingBinding = bindable.getBinding(held);
        if (existingBinding != null) {
            return; // Already bound, don't interfere
        }

        // Check if player is on a team
        if (!TeamsIntegration.isOnTeam(serverPlayer)) {
            return; // Not on team, let normal binding happen
        }

        // Check if player can bind to their team (team property restriction)
        if (!TeamsIntegration.canBindToTeam(serverPlayer)) {
            // Player is on team but restricted from team binding, let personal binding happen
            return;
        }

        // Check player's binding preference
        BindingPreference pref = PlayerBindingData.getPreference(player);

        if (pref != null && pref.dontAsk) {
            // Player has a saved preference, apply it automatically
            if (pref.mode == BindingMode.TEAM) {
                // Cancel the event and apply team binding directly
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                applyTeamBinding(serverPlayer, held, pref);
                BloodMagicTeams.LOGGER.debug("Auto-applied team binding for {} (saved preference)",
                        player.getName().getString());
            }
            // For PERSONAL mode, just let normal binding happen
            return;
        }

        // Player needs to see the UI - cancel this event and show UI
        if (BMTeamsConfig.SHOW_BINDING_UI.get()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);

            // Mark as pending
            pendingBindingPlayers.add(player.getUUID());

            // Send packet to open UI
            Optional<String> teamName = TeamsIntegration.getTeamName(serverPlayer);
            BMTeamsNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new OpenBindingScreenPacket(teamName.orElse("Unknown Team"))
            );
            
            BloodMagicTeams.LOGGER.debug("Showing binding UI to {}", player.getName().getString());
        }
    }

    /**
     * Called after BloodMagic's binding event to potentially modify the binding.
     * This handles the case where player has team binding preference set.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemBind(wayoftime.bloodmagic.event.ItemBindEvent event) {
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

        // Check if this player was pending team binding
        if (!pendingBindingPlayers.remove(player.getUUID())) {
            return;
        }

        // Get player's preference
        BindingPreference pref = PlayerBindingData.getPreference(player);
        if (pref == null || pref.mode != BindingMode.TEAM) {
            return;
        }

        // Apply team binding
        Optional<UUID> teamId = pref.targetTeamId != null 
                ? Optional.of(pref.targetTeamId) 
                : TeamsIntegration.getTeamId(serverPlayer);

        if (teamId.isPresent()) {
            String teamName = TeamsIntegration.getTeamNameByUuid(teamId.get());
            if (teamName == null) {
                teamName = "Unknown Team";
            }

            ItemStack stack = event.getBindingStack();
            Binding teamBinding = new Binding(teamId.get(), teamName);
            BindableHelper.applyBinding(stack, teamBinding);

            BloodMagicTeams.LOGGER.debug("Applied team binding for {} to team {}", 
                    player.getName().getString(), teamName);
        }
    }

    /**
     * Applies team binding directly to an item.
     * Used when player has saved TEAM preference with dontAsk=true.
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
            BindableHelper.applyBinding(stack, teamBinding);

            BloodMagicTeams.LOGGER.debug("Applied team binding for {} to team {}",
                    player.getName().getString(), teamName);
        } else {
            BloodMagicTeams.LOGGER.warn("Could not find team for player {} to apply team binding",
                    player.getName().getString());
        }
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
