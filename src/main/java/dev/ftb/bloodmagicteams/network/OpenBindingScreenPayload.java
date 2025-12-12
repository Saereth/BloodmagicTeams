package dev.ftb.bloodmagicteams.network;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.ui.BindingModeScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Payload sent from server to client to open the binding mode selection screen.
 */
public record OpenBindingScreenPayload(String teamName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenBindingScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BloodMagicTeams.MOD_ID, "open_binding_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenBindingScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenBindingScreenPayload::teamName,
            OpenBindingScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenBindingScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClient(payload));
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OpenBindingScreenPayload payload) {
        new BindingModeScreen(payload.teamName).openGui();
    }
}
