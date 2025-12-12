package dev.ftb.bloodmagicteams;

import dev.ftb.bloodmagicteams.commands.BMTeamsCommands;
import dev.ftb.bloodmagicteams.config.BMTeamsConfig;
import dev.ftb.bloodmagicteams.events.TeamEventHandler;
import dev.ftb.bloodmagicteams.events.TooltipEventHandler;
import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import dev.ftb.bloodmagicteams.network.BMTeamsNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BloodMagicTeams.MOD_ID)
public class BloodMagicTeams {
    public static final String MOD_ID = "bloodmagicteams";
    public static final Logger LOGGER = LogManager.getLogger();

    public BloodMagicTeams(IEventBus modEventBus, ModContainer container) {
        // Register config
        container.registerConfig(ModConfig.Type.COMMON, BMTeamsConfig.SPEC);

        // Register setup event
        modEventBus.addListener(this::commonSetup);

        // Register event handlers
        NeoForge.EVENT_BUS.register(new TeamEventHandler());
        NeoForge.EVENT_BUS.register(new TooltipEventHandler());

        // Register command event
        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Register network payloads
        modEventBus.addListener(BMTeamsNetwork::registerPayloads);

        LOGGER.info("BloodMagic Teams loaded - FTB Teams integration for Soul Networks");
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
        BMTeamsCommands.register(event.getDispatcher());
    }
}
