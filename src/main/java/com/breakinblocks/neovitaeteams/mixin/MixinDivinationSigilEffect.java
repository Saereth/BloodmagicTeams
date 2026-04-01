package com.breakinblocks.neovitaeteams.mixin;

import com.breakinblocks.neovitae.api.sigil.effects.DivinationSigilEffect;
import com.breakinblocks.neovitae.common.datacomponent.Binding;
import com.breakinblocks.neovitaeteams.integration.TeamsIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.UUID;

/**
 * Suppresses the "other network" message on the Divination/Seer sigil when
 * the binding owner is a team UUID and the player is a member of that team.
 */
@Mixin(value = DivinationSigilEffect.class, remap = false)
public class MixinDivinationSigilEffect {

    @Redirect(
            method = "showNetworkInfo",
            at = @At(value = "INVOKE", target = "Lcom/breakinblocks/neovitae/common/datacomponent/Binding;uuid()Ljava/util/UUID;")
    )
    private UUID neovitaeteams$teamAwareUuid(Binding binding, Player player, ItemStack stack) {
        UUID ownerId = binding.uuid();
        if (player instanceof ServerPlayer serverPlayer) {
            Optional<UUID> playerTeam = TeamsIntegration.getTeamId(serverPlayer);
            if (playerTeam.isPresent() && playerTeam.get().equals(ownerId)) {
                return player.getGameProfile().getId();
            }
        }
        return ownerId;
    }
}
