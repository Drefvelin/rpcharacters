package net.tfminecraft.RPCharacters.permadeath;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Loaders.RegionLoader;
import me.Plugins.SimpleFactions.Map.MapRegion;
import me.Plugins.SimpleFactions.Map.ProvinceGrid;
import me.Plugins.SimpleFactions.Map.presence.ProvincePresenceService;
import me.Plugins.SimpleFactions.SimpleFactions;
import net.tfminecraft.RPCharacters.Loaders.PermadeathZoneLoader;
import net.tfminecraft.RPCharacters.Objects.PermadeathZoneDefinition;
import net.tfminecraft.RPCharacters.RPCharacters;

/**
 * Compile-time SimpleFactions types live only in this class so the rest of
 * permadeath still loads when SF is absent.
 */
public final class SimpleFactionsRegionBridge {

	private static boolean available;

	private SimpleFactionsRegionBridge() {
	}

	public static void init() {
		available = Bukkit.getPluginManager().isPluginEnabled("SimpleFactions");
		if (available) {
			RPCharacters.plugin.getLogger().info(
					"SimpleFactions bridge enabled for permadeath map regions.");
		}
	}

	public static boolean isAvailable() {
		return available;
	}

	public static PermadeathZoneDefinition getPermadeathZoneAt(Player player, Location location) {
		if (!available) {
			return null;
		}
		try {
			int provinceId = resolveProvinceId(player, location);
			if (provinceId <= 0 || provinceId == ProvincePresenceService.UNKNOWN_PROVINCE) {
				return null;
			}
			MapRegion region = RegionLoader.getByProvince(provinceId);
			if (region == null) {
				return null;
			}
			return PermadeathZoneLoader.getZone(region.getId());
		} catch (NoClassDefFoundError | Exception ex) {
			RPCharacters.plugin.getLogger().warning(
					"SimpleFactions region lookup failed: " + ex.getMessage());
			return null;
		}
	}

	private static int resolveProvinceId(Player player, Location location) {
		if (player != null) {
			int current = ProvincePresenceService.getInstance().getCurrentProvince(player);
			if (current > 0) {
				return current;
			}
		}
		if (location == null || location.getWorld() == null) {
			return ProvincePresenceService.UNKNOWN_PROVINCE;
		}
		SimpleFactions plugin = SimpleFactions.getInstance();
		if (plugin == null) {
			return ProvincePresenceService.UNKNOWN_PROVINCE;
		}
		ProvinceGrid grid = plugin.getProvinceGrid();
		if (grid == null) {
			return ProvincePresenceService.UNKNOWN_PROVINCE;
		}
		int fromGrid = grid.getAt(location.getBlockX(), location.getBlockZ());
		return fromGrid > 0 ? fromGrid : ProvincePresenceService.UNKNOWN_PROVINCE;
	}
}
