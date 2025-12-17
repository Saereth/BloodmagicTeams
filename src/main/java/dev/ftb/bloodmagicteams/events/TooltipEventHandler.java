package dev.ftb.bloodmagicteams.events;

import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
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
 * Handles tooltip modifications for team-bound Blood Magic items.
 * Blood Magic handles showing the binding info itself - we just add team indicators
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
            return; // Blood Magic handles unbound items
        }

        UUID ownerUuid = binding.uuid();
        if (ownerUuid == null) {
            return;
        }

        // Only process team-bound items
        if (!TeamsIntegration.isTeamUuid(ownerUuid)) {
            return; // Personal binding - Blood Magic handles it
        }

        List<Component> tooltip = event.getToolTip();
        String storedName = binding.name();

        // Get the current team name (may have changed since binding)
        String currentTeamName = TeamsIntegration.getTeamNameByUuid(ownerUuid);
        if (currentTeamName == null) {
            currentTeamName = storedName; // Fall back to stored name
        }

        // Find and update the owner line if team name changed
        if (!currentTeamName.equals(storedName)) {
            for (int i = 0; i < tooltip.size(); i++) {
                String lineStr = tooltip.get(i).getString();
                if (lineStr.contains(storedName)) {
                    // Replace with current team name
                    tooltip.set(i, Component.translatable("tooltip.bloodmagic.current_owner", currentTeamName)
                            .withStyle(ChatFormatting.GRAY));
                    break;
                }
            }
        }

        // Add team indicator if not already present
        String teamIndicator = Component.translatable("bloodmagicteams.tooltip.team_bound").getString();
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
            tooltip.add(insertIndex, Component.translatable("bloodmagicteams.tooltip.team_bound")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
