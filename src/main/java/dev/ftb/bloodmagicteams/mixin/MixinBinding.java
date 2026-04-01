package dev.ftb.bloodmagicteams.mixin;

import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wayoftime.bloodmagic.core.data.Binding;

import java.util.UUID;

/**
 * Fixes NPE crash in Binding.serializeNBT() when the owner name is null.
 * This happens when a team-bound activation crystal is used on a ritual stone:
 * after server restart, TileMasterRitualStone tries to recreate the Binding using
 * PlayerHelper.getUsernameFromUUID(owner), which returns null for team UUIDs.
 */
@Mixin(value = Binding.class, remap = false)
public class MixinBinding {

    @Shadow
    private String name;

    @Shadow
    private UUID uuid;

    @Inject(method = "serializeNBT", at = @At("HEAD"))
    private void bloodmagicteams$fixNullName(CallbackInfoReturnable<CompoundTag> cir) {
        if (this.name == null) {
            // Try to resolve as a team name first
            if (this.uuid != null) {
                String teamName = TeamsIntegration.getTeamNameByUuid(this.uuid);
                if (teamName != null) {
                    this.name = teamName;
                    return;
                }
            }
            // Fallback to empty string to prevent NPE
            this.name = "";
        }
    }
}
