package dev.ftb.bloodmagicteams.network;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.data.PlayerBindingData;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingMode;
import dev.ftb.bloodmagicteams.events.TeamEventHandler;
import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import wayoftime.bloodmagic.common.item.IBindable;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.util.helper.BindableHelper;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet sent from client to server when player selects a binding mode.
 */
public class BindingModePacket {
    private final BindingMode mode;
    private final boolean dontAskAgain;

    public BindingModePacket(BindingMode mode, boolean dontAskAgain) {
        this.mode = mode;
        this.dontAskAgain = dontAskAgain;
    }

    public static void encode(BindingModePacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.mode);
        buf.writeBoolean(packet.dontAskAgain);
    }

    public static BindingModePacket decode(FriendlyByteBuf buf) {
        return new BindingModePacket(
                buf.readEnum(BindingMode.class),
                buf.readBoolean()
        );
    }

    public static void handle(BindingModePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            // Store the player's binding mode preference
            PlayerBindingData.setBindingMode(player, packet.mode, packet.dontAskAgain);
            BloodMagicTeams.LOGGER.debug("Player {} set binding mode to {} (dontAsk: {})", 
                    player.getName().getString(), packet.mode, packet.dontAskAgain);

            // Check if player was waiting to bind an item
            if (TeamEventHandler.isPending(player.getUUID())) {
                TeamEventHandler.clearPending(player.getUUID());
                
                // Find the bindable item in player's hands
                ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
                
                ItemStack toBind = null;
                if (mainHand.getItem() instanceof IBindable bindable && bindable.getBinding(mainHand) == null) {
                    toBind = mainHand;
                } else if (offHand.getItem() instanceof IBindable bindable && bindable.getBinding(offHand) == null) {
                    toBind = offHand;
                }

                if (toBind != null) {
                    applyBinding(player, toBind, packet.mode);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void applyBinding(ServerPlayer player, ItemStack stack, BindingMode mode) {
        UUID ownerId;
        String ownerName;

        if (mode == BindingMode.TEAM) {
            // Get team info
            Optional<UUID> teamId = TeamsIntegration.getTeamId(player);
            if (teamId.isEmpty()) {
                // Player left team? Fall back to personal
                ownerId = player.getUUID();
                ownerName = player.getName().getString();
            } else {
                ownerId = teamId.get();
                ownerName = TeamsIntegration.getTeamNameByUuid(teamId.get());
                if (ownerName == null) {
                    ownerName = "Team";
                }
            }
        } else {
            // Personal binding
            ownerId = player.getUUID();
            ownerName = player.getName().getString();
        }

        // Apply the binding
        Binding binding = new Binding(ownerId, ownerName);
        BindableHelper.applyBinding(stack, binding);

        BloodMagicTeams.LOGGER.debug("Applied {} binding for {} (owner: {})", 
                mode, player.getName().getString(), ownerName);
    }
}
