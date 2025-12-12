package dev.ftb.bloodmagicteams.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class BMTeamsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_TEAM_BINDING;
    public static final ForgeConfigSpec.BooleanValue SHOW_BINDING_UI;
    public static final ForgeConfigSpec.BooleanValue ALLOW_BINDING_MODE_COMMAND;
    public static final ForgeConfigSpec.EnumValue<DefaultBindingMode> DEFAULT_BINDING_MODE;

    static {
        BUILDER.comment("BloodMagic Teams Configuration");
        BUILDER.push("general");

        ENABLE_TEAM_BINDING = BUILDER
                .comment("Enable team-level soul network binding")
                .define("enableTeamBinding", true);

        SHOW_BINDING_UI = BUILDER
                .comment("Show UI when binding items to choose between personal and team binding")
                .define("showBindingUI", true);

        ALLOW_BINDING_MODE_COMMAND = BUILDER
                .comment("Allow players to use /bmteams bindingmode command to set their default binding mode")
                .define("allowBindingModeCommand", true);

        DEFAULT_BINDING_MODE = BUILDER
                .comment("Default binding mode when UI is disabled or player hasn't set a preference",
                        "PERSONAL = bind to player's personal soul network",
                        "TEAM = bind to team's shared soul network",
                        "ASK = always show the binding UI")
                .defineEnum("defaultBindingMode", DefaultBindingMode.ASK);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public enum DefaultBindingMode {
        PERSONAL,
        TEAM,
        ASK
    }
}
