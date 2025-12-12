package dev.ftb.bloodmagicteams.network;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.data.PlayerBindingData;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingMode;
import dev.ftb.bloodmagicteams.events.TeamEventHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import wayoftime.bloodmagic.common.item.IBindable;

/**
 * Payload sent from client to server when player selects a binding mode.
 */
public record BindingModePayload(BindingMode mode, boolean dontAskAgain) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BindingModePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodMagicTeams.MOD_ID, "binding_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BindingModePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(i -> BindingMode.values()[i], BindingMode::ordinal),
            BindingModePayload::mode,
            ByteBufCodecs.BOOL,
            BindingModePayload::dontAskAgain,
            BindingModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BindingModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Store the player's binding mode preference
            PlayerBindingData.setBindingMode(player, payload.mode, payload.dontAskAgain);
            BloodMagicTeams.LOGGER.info("Player {} set binding mode to {} (dontAsk: {})",
                    player.getName().getString(), payload.mode, payload.dontAskAgain);

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
                    if (payload.mode == BindingMode.TEAM) {
                        TeamEventHandler.applyTeamBindingFromUI(player, toBind);
                    } else {
                        TeamEventHandler.applyPersonalBindingFromUI(player, toBind);
                    }
                } else {
                    BloodMagicTeams.LOGGER.warn("Player {} made binding choice but no bindable item found in hands",
                            player.getName().getString());
                }
            }
        });
    }
}
