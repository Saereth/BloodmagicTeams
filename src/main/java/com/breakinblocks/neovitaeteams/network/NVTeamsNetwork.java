package com.breakinblocks.neovitaeteams.network;

import com.breakinblocks.neovitaeteams.NeoVitaeTeams;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network handler for Neo Vitae Teams packets.
 */
public class NVTeamsNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NeoVitaeTeams.MOD_ID)
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

        NeoVitaeTeams.LOGGER.debug("Network payloads registered");
    }
}
