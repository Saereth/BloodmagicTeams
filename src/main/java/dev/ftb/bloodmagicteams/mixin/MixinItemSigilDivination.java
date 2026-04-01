package dev.ftb.bloodmagicteams.mixin;

import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wayoftime.bloodmagic.common.item.sigil.ItemSigilDivination;
import wayoftime.bloodmagic.core.data.Binding;

import java.util.Optional;
import java.util.UUID;

/**
 * Suppresses the "other network" message on the Divination/Seer sigil when
 * the binding owner is a team UUID and the player is a member of that team.
 */
@Mixin(ItemSigilDivination.class)
public class MixinItemSigilDivination {

    /**
     * Redirects Binding.getOwnerId() calls in use() so that team members
     * see the team's soul network as their own (no "other network" message).
     */
    @Redirect(
            method = "use",
            at = @At(value = "INVOKE", target = "Lwayoftime/bloodmagic/core/data/Binding;getOwnerId()Ljava/util/UUID;", remap = false)
    )
    private UUID bloodmagicteams$getTeamAwareOwnerId(Binding binding,
                                                     Level world, Player player, InteractionHand hand) {
        UUID ownerId = binding.getOwnerId();
        if (player instanceof ServerPlayer serverPlayer) {
            Optional<UUID> playerTeam = TeamsIntegration.getTeamId(serverPlayer);
            if (playerTeam.isPresent() && playerTeam.get().equals(ownerId)) {
                // Player is on the team that owns this binding - return player's UUID
                // so the ownership check passes and the "otherNetwork" message is suppressed
                return player.getGameProfile().getId();
            }
        }
        return ownerId;
    }
}
