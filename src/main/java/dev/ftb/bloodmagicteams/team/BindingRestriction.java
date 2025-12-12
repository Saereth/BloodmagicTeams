package dev.ftb.bloodmagicteams.team;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Defines who can bind items to a team's soul network.
 */
public enum BindingRestriction implements StringRepresentable {
    MEMBER("member"),   // Any team member can bind (default)
    OFFICER("officer"); // Only officers and above can bind

    private final String name;

    BindingRestriction(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component getDisplayName() {
        return Component.translatable("bloodmagicteams.binding_restriction." + name);
    }

    public static BindingRestriction fromString(String name) {
        for (BindingRestriction restriction : values()) {
            if (restriction.name.equals(name)) {
                return restriction;
            }
        }
        return MEMBER;
    }
}
