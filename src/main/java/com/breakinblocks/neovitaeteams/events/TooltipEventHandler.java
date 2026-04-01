package com.breakinblocks.neovitaeteams.events;

import com.breakinblocks.neovitaeteams.integration.TeamsIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import com.breakinblocks.neovitae.common.item.IBindable;
import com.breakinblocks.neovitae.common.datacomponent.Binding;

import java.util.List;
import java.util.UUID;

/**
 * Handles tooltip modifications for team-bound Neo Vitae items.
 * Neo Vitae handles showing the binding info itself - we just add team indicators
 * and update team names if they've changed.
 */
public class TooltipEventHandler {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty() || !(stack.getItem() instanceof IBindable bindable)) {
            return;
        }

        Binding binding = bindable.getBinding(stack);
        if (binding == null || binding.isEmpty()) {
            return; // Neo Vitae handles unbound items
        }

        UUID ownerUuid = binding.uuid();
        if (ownerUuid == null) {
            return;
        }

        // Only process team-bound items
        if (!TeamsIntegration.isTeamUuid(ownerUuid)) {
            return; // Personal binding - Neo Vitae handles it
        }

        List<Component> tooltip = event.getToolTip();
        String storedName = binding.name();

        // Get the current team name (may have changed since binding)
        String currentTeamName = TeamsIntegration.getTeamNameByUuid(ownerUuid);
        if (currentTeamName == null) {
            currentTeamName = storedName; // Fall back to stored name
        }

        // Find and update the owner line if team name changed
        boolean replaced = false;
        if (!currentTeamName.equals(storedName)) {
            for (int i = 0; i < tooltip.size(); i++) {
                String lineStr = tooltip.get(i).getString();
                if (lineStr.contains(storedName)) {
                    // Replace with current team name
                    tooltip.set(i, Component.translatable("tooltip.neovitae.current_owner", currentTeamName)
                            .withStyle(ChatFormatting.GRAY));
                    replaced = true;
                    break;
                }
            }
        }

        // Fallback: other mods may resolve the owner name client-side via player UUID lookup.
        // When team-bound, the UUID is a team UUID which won't match any player, so they show "Unknown".
        if (!replaced) {
            for (int i = 0; i < tooltip.size(); i++) {
                String lineStr = tooltip.get(i).getString();
                if (lineStr.contains("Unknown")) {
                    String newLineStr = lineStr.replace("Unknown", currentTeamName);
                    tooltip.set(i, Component.literal(newLineStr)
                            .withStyle(tooltip.get(i).getStyle()));
                    break;
                }
            }
        }

        // Add team indicator if not already present
        String teamIndicator = Component.translatable("neovitaeteams.tooltip.team_bound").getString();
        boolean hasIndicator = tooltip.stream()
                .anyMatch(c -> c.getString().contains(teamIndicator));

        if (!hasIndicator) {
            // Find where to insert (after the owner line)
            int insertIndex = 1;
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().contains(currentTeamName) ||
                    tooltip.get(i).getString().contains(storedName)) {
                    insertIndex = i + 1;
                    break;
                }
            }
            tooltip.add(insertIndex, Component.translatable("neovitaeteams.tooltip.team_bound")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
