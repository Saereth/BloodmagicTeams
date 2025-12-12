package dev.ftb.bloodmagicteams;

import dev.ftb.bloodmagicteams.commands.BMTeamsCommands;
import dev.ftb.bloodmagicteams.config.BMTeamsConfig;
import dev.ftb.bloodmagicteams.events.TeamEventHandler;
import dev.ftb.bloodmagicteams.events.TooltipEventHandler;
import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import dev.ftb.bloodmagicteams.network.BMTeamsNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BloodMagicTeams.MOD_ID)
public class BloodMagicTeams {
    public static final String MOD_ID = "bloodmagicteams";
    public static final Logger LOGGER = LogManager.getLogger();

    public BloodMagicTeams() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BMTeamsConfig.SPEC);

        // Register setup event
        modEventBus.addListener(this::commonSetup);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new TeamEventHandler());
        MinecraftForge.EVENT_BUS.register(new TooltipEventHandler());
        
        // Register command event
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        LOGGER.info("BloodMagic Teams loaded - FTB Teams integration for Soul Networks");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize network packets
            BMTeamsNetwork.init();

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
