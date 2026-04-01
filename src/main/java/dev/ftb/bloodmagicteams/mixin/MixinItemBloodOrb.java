package dev.ftb.bloodmagicteams.mixin;

import dev.ftb.bloodmagicteams.integration.TeamsIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wayoftime.bloodmagic.common.item.ItemBloodOrb;
import wayoftime.bloodmagic.core.data.Binding;

import java.util.Optional;
import java.util.UUID;

/**
 * Allows team members to set the orb tier on a team-bound Blood Orb.
 * Without this, only the binding owner (which is the team UUID) would match,
 * meaning no player could ever set the tier on a team-bound orb.
 */
@Mixin(ItemBloodOrb.class)
public class MixinItemBloodOrb {

    /**
     * Redirects Binding.getOwnerId() in use() so that team members
     * can set the orb tier on the team's soul network.
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
                return player.getGameProfile().getId();
            }
        }
        return ownerId;
    }
}
