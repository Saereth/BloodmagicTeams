package dev.ftb.bloodmagicteams.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player binding mode preferences.
 * Stores whether players prefer personal or team binding,
 * and whether to show the binding prompt.
 */
public class PlayerBindingData {
    private static final String NBT_KEY = "BMTeamsData";
    private static final String BINDING_MODE_KEY = "BindingMode";
    private static final String DONT_ASK_KEY = "DontAsk";
    private static final String TARGET_TEAM_KEY = "TargetTeam";
    
    // Cache for player data
    private static final Map<UUID, BindingPreference> playerPreferences = new HashMap<>();

    /**
     * Binding mode options.
     */
    public enum BindingMode {
        PERSONAL,  // Always bind to personal soul network
        TEAM       // Bind to team's soul network
    }

    /**
     * Stores a player's binding preferences.
     */
    public static class BindingPreference {
        public BindingMode mode;
        public boolean dontAsk;
        @Nullable
        public UUID targetTeamId; // Specific team to bind to (null = current team)

        public BindingPreference() {
            this.mode = BindingMode.PERSONAL;
            this.dontAsk = false;
            this.targetTeamId = null;
        }

        public BindingPreference(BindingMode mode, boolean dontAsk, @Nullable UUID targetTeamId) {
            this.mode = mode;
            this.dontAsk = dontAsk;
            this.targetTeamId = targetTeamId;
        }
    }

    /**
     * Sets the binding mode preference for a player.
     */
    public static void setBindingMode(ServerPlayer player, BindingMode mode, boolean dontAsk) {
        setBindingMode(player, mode, dontAsk, null);
    }

    /**
     * Sets the binding mode preference for a player with a specific team target.
     */
    public static void setBindingMode(ServerPlayer player, BindingMode mode, boolean dontAsk, @Nullable UUID targetTeamId) {
        BindingPreference pref = new BindingPreference(mode, dontAsk, targetTeamId);
        playerPreferences.put(player.getUUID(), pref);
        saveToNBT(player, pref);
    }

    /**
     * Gets the binding preference for a player.
     * Returns null if the player hasn't set a preference (should show prompt).
     */
    @Nullable
    public static BindingPreference getPreference(Player player) {
        UUID playerId = player.getUUID();

        // Check cache first
        if (playerPreferences.containsKey(playerId)) {
            return playerPreferences.get(playerId);
        }

        // Load from persistent data
        BindingPreference pref = loadFromNBT(player);
        if (pref != null) {
            playerPreferences.put(playerId, pref);
        }
        return pref;
    }

    /**
     * Checks if the player should see the binding prompt.
     * Returns true if they should see the prompt (no preference set or dontAsk is false).
     */
    public static boolean shouldShowPrompt(Player player) {
        BindingPreference pref = getPreference(player);
        return pref == null || !pref.dontAsk;
    }

    /**
     * Gets the binding mode for a player.
     * Returns null if no preference is set.
     */
    @Nullable
    public static BindingMode getBindingMode(Player player) {
        BindingPreference pref = getPreference(player);
        return pref != null ? pref.mode : null;
    }

    /**
     * Resets the player's binding preferences (clears "don't ask again").
     */
    public static void resetPreferences(ServerPlayer player) {
        playerPreferences.remove(player.getUUID());
        
        // Clear from NBT
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.contains(NBT_KEY)) {
            persistentData.remove(NBT_KEY);
        }
    }

    /**
     * Clears the cached preferences for a player (e.g., when they log out).
     */
    public static void clearCache(UUID playerId) {
        playerPreferences.remove(playerId);
    }

    private static void saveToNBT(ServerPlayer player, BindingPreference pref) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag bmData = new CompoundTag();
        
        bmData.putString(BINDING_MODE_KEY, pref.mode.name());
        bmData.putBoolean(DONT_ASK_KEY, pref.dontAsk);
        if (pref.targetTeamId != null) {
            bmData.putUUID(TARGET_TEAM_KEY, pref.targetTeamId);
        }
        
        persistentData.put(NBT_KEY, bmData);
    }

    @Nullable
    private static BindingPreference loadFromNBT(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        
        if (!persistentData.contains(NBT_KEY)) {
            return null;
        }
        
        CompoundTag bmData = persistentData.getCompound(NBT_KEY);
        
        try {
            BindingMode mode = BindingMode.valueOf(bmData.getString(BINDING_MODE_KEY));
            boolean dontAsk = bmData.getBoolean(DONT_ASK_KEY);
            UUID targetTeam = bmData.contains(TARGET_TEAM_KEY) ? bmData.getUUID(TARGET_TEAM_KEY) : null;
            
            return new BindingPreference(mode, dontAsk, targetTeam);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
