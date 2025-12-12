package dev.ftb.bloodmagicteams.events;

import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.common.datacomponent.Binding;

import java.util.List;
import java.util.UUID;

/**
 * Handles tooltip modifications to show current team names for team-bound items.
 */
public class TooltipEventHandler {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty() || !(stack.getItem() instanceof IBindable bindable)) {
            return;
        }

        Binding binding = bindable.getBinding(stack);
        if (binding == null) {
            return;
        }

        UUID ownerUuid = binding.uuid();
        if (ownerUuid == null) {
            return;
        }

        // Check if this is a team-bound item
        if (!TeamsIntegration.isTeamUuid(ownerUuid)) {
            return;
        }

        // Get the current team name
        String currentTeamName = TeamsIntegration.getTeamNameByUuid(ownerUuid);
        if (currentTeamName == null) {
            currentTeamName = Component.translatable("bloodmagicteams.tooltip.unknown_team").getString();
        }

        // Find and replace the owner line in the tooltip
        List<Component> tooltip = event.getToolTip();
        String storedName = binding.name();

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            String lineString = line.getString();

            // BloodMagic shows "Current owner: <name>" or similar
            // We need to find the line with the stored name and replace it with the current team name
            if (storedName != null && lineString.contains(storedName)) {
                // Replace with dynamic team name and add team indicator
                String newLineString = lineString.replace(storedName, currentTeamName);
                MutableComponent newLine = Component.literal(newLineString)
                        .withStyle(line.getStyle());
                tooltip.set(i, newLine);

                // Add a team indicator line if not already present
                boolean hasTeamIndicator = false;
                for (Component c : tooltip) {
                    if (c.getString().contains("[") && c.getString().contains("]")) {
                        // Check for our team indicator
                        String indicatorText = Component.translatable("bloodmagicteams.tooltip.team_bound").getString();
                        if (c.getString().contains(indicatorText) || c.getString().contains("Team")) {
                            hasTeamIndicator = true;
                            break;
                        }
                    }
                }

                if (!hasTeamIndicator) {
                    tooltip.add(i + 1, Component.translatable("bloodmagicteams.tooltip.team_bound")
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
                break;
            }
        }
    }
}
