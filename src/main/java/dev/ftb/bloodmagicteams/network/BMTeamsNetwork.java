package dev.ftb.bloodmagicteams.network;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network handler for BloodMagic Teams packets.
 */
public class BMTeamsNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(BloodMagicTeams.MOD_ID)
                .versioned(PROTOCOL_VERSION);

        // Client -> Server: Player's binding mode selection
        registrar.playToServer(
                BindingModePayload.TYPE,
                BindingModePayload.STREAM_CODEC,
                BindingModePayload::handle
        );

        // Server -> Client: Open binding mode selection screen
        registrar.playToClient(
                OpenBindingScreenPayload.TYPE,
                OpenBindingScreenPayload.STREAM_CODEC,
                OpenBindingScreenPayload::handle
        );

        BloodMagicTeams.LOGGER.debug("Network payloads registered");
    }
}
