package dev.ftb.bloodmagicteams.events;

import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.core.data.Binding;

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

        UUID ownerUuid = binding.getOwnerId();
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
        String storedName = binding.getOwnerName();

        boolean replaced = false;
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
                replaced = true;
                addTeamIndicator(tooltip, i);
                break;
            }
        }

        // Fallback: other mods (e.g. Animus) resolve the owner name client-side via player UUID lookup.
        // When team-bound, the UUID is a team UUID which won't match any player, so they show "Unknown".
        // Search for that and replace it with the team name.
        if (!replaced) {
            for (int i = 0; i < tooltip.size(); i++) {
                Component line = tooltip.get(i);
                String lineString = line.getString();

                if (lineString.contains("Unknown")) {
                    String newLineString = lineString.replace("Unknown", currentTeamName);
                    MutableComponent newLine = Component.literal(newLineString)
                            .withStyle(line.getStyle());
                    tooltip.set(i, newLine);
                    addTeamIndicator(tooltip, i);
                    break;
                }
            }
        }
    }

    private void addTeamIndicator(List<Component> tooltip, int afterIndex) {
        for (Component c : tooltip) {
            String text = c.getString();
            if (text.contains("[") && text.contains("]")) {
                String indicatorText = Component.translatable("bloodmagicteams.tooltip.team_bound").getString();
                if (text.contains(indicatorText) || text.contains("Team")) {
                    return;
                }
            }
        }
        tooltip.add(afterIndex + 1, Component.translatable("bloodmagicteams.tooltip.team_bound")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
