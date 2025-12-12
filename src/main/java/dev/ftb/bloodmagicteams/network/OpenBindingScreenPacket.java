package dev.ftb.bloodmagicteams.network;

import dev.ftb.bloodmagicteams.ui.BindingModeScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from server to client to open the binding mode selection screen.
 */
public class OpenBindingScreenPacket {
    private final String teamName;

    public OpenBindingScreenPacket(String teamName) {
        this.teamName = teamName;
    }

    public static void encode(OpenBindingScreenPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.teamName);
    }

    public static OpenBindingScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenBindingScreenPacket(buf.readUtf());
    }

    public static void handle(OpenBindingScreenPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            handleClient(packet);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OpenBindingScreenPacket packet) {
        new BindingModeScreen(packet.teamName).openGui();
    }
}
