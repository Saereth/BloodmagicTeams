package dev.ftb.bloodmagicteams.network;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Network handler for BloodMagic Teams packets.
 */
public class BMTeamsNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BloodMagicTeams.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void init() {
        // Client -> Server: Player's binding mode selection
        CHANNEL.registerMessage(
                packetId++,
                BindingModePacket.class,
                BindingModePacket::encode,
                BindingModePacket::decode,
                BindingModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        // Server -> Client: Open binding mode selection screen
        CHANNEL.registerMessage(
                packetId++,
                OpenBindingScreenPacket.class,
                OpenBindingScreenPacket::encode,
                OpenBindingScreenPacket::decode,
                OpenBindingScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        BloodMagicTeams.LOGGER.debug("Network packets registered");
    }
}
