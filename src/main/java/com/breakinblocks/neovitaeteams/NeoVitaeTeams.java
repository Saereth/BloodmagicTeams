package com.breakinblocks.neovitaeteams;

import com.breakinblocks.neovitaeteams.commands.NVTeamsCommands;
import com.breakinblocks.neovitaeteams.config.NVTeamsConfig;
import com.breakinblocks.neovitaeteams.events.TeamEventHandler;
import com.breakinblocks.neovitaeteams.events.TooltipEventHandler;
import com.breakinblocks.neovitaeteams.integration.TeamsIntegration;
import com.breakinblocks.neovitaeteams.network.NVTeamsNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NeoVitaeTeams.MOD_ID)
public class NeoVitaeTeams {
    public static final String MOD_ID = "neovitaeteams";
    public static final Logger LOGGER = LogManager.getLogger();

    public NeoVitaeTeams(IEventBus modEventBus, ModContainer container) {
        // Register config
        container.registerConfig(ModConfig.Type.COMMON, NVTeamsConfig.SPEC);

        // Register setup event
        modEventBus.addListener(this::commonSetup);

        // Register event handlers
        NeoForge.EVENT_BUS.register(new TeamEventHandler());
        NeoForge.EVENT_BUS.register(new TooltipEventHandler());

        // Register command event
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Register network payloads
        modEventBus.addListener(NVTeamsNetwork::registerPayloads);

        LOGGER.info("Neo Vitae Teams loaded - FTB Teams integration for Soul Networks");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Check for FTB Teams and register properties
            if (TeamsIntegration.isTeamsLoaded()) {
                TeamsIntegration.registerProperties();
                LOGGER.info("FTB Teams detected - team binding features enabled");
            } else {
                LOGGER.warn("FTB Teams not found - team binding features disabled");
            }
        });
    }

    private void registerCommands(RegisterCommandsEvent event) {
        NVTeamsCommands.register(event.getDispatcher());
    }
}
