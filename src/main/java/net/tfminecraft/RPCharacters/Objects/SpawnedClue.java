package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import net.tfminecraft.RPCharacters.Cache;

public class SpawnedClue {

	private final UUID id;
	private final String world;
	private final double x;
	private final double y;
	private final double z;
	private final String clueText;
	private final long expiresAtMs;
	private final UUID ownerUuid;
	private final Integer targetBlockX;
	private final Integer targetBlockY;
	private final Integer targetBlockZ;

	private final List<UUID> displayEntityIds = new ArrayList<>();
	private boolean visualsSpawned;

	public SpawnedClue(UUID id, String world, double x, double y, double z, String clueText,
			long expiresAtMs, UUID ownerUuid) {
		this(id, world, x, y, z, clueText, expiresAtMs, ownerUuid, null, null, null);
	}

	public SpawnedClue(UUID id, String world, double x, double y, double z, String clueText,
			long expiresAtMs, UUID ownerUuid, Integer targetBlockX, Integer targetBlockY, Integer targetBlockZ) {
		this(id, world, x, y, z, clueText, expiresAtMs, ownerUuid, targetBlockX, targetBlockY, targetBlockZ, null);
	}

	public SpawnedClue(UUID id, String world, double x, double y, double z, String clueText,
			long expiresAtMs, UUID ownerUuid, Integer targetBlockX, Integer targetBlockY, Integer targetBlockZ,
			List<UUID> displayEntityIds) {
		this.id = id;
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.clueText = clueText;
		this.expiresAtMs = expiresAtMs;
		this.ownerUuid = ownerUuid;
		this.targetBlockX = targetBlockX;
		this.targetBlockY = targetBlockY;
		this.targetBlockZ = targetBlockZ;
		if (displayEntityIds != null) {
			this.displayEntityIds.addAll(displayEntityIds);
		}
	}

	public UUID getId() {
		return id;
	}

	public String getWorldName() {
		return world;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public String getClueText() {
		return clueText;
	}

	public long getExpiresAtMs() {
		return expiresAtMs;
	}

	public UUID getOwnerUuid() {
		return ownerUuid;
	}

	public boolean hasTargetBlock() {
		return targetBlockX != null && targetBlockY != null && targetBlockZ != null;
	}

	public int getTargetBlockX() {
		return targetBlockX;
	}

	public int getTargetBlockY() {
		return targetBlockY;
	}

	public int getTargetBlockZ() {
		return targetBlockZ;
	}

	public Location getTargetCenter(World world) {
		if (!hasTargetBlock() || world == null) return null;
		return new Location(world, targetBlockX + 0.5, targetBlockY + 0.5, targetBlockZ + 0.5);
	}

	public Location getTargetCenter() {
		return getTargetCenter(resolveWorld());
	}

	public List<UUID> getDisplayEntityIds() {
		return displayEntityIds;
	}

	public boolean isVisualsSpawned() {
		return visualsSpawned;
	}

	public void setVisualsSpawned(boolean visualsSpawned) {
		this.visualsSpawned = visualsSpawned;
	}

	public void clearDisplayEntityIds() {
		displayEntityIds.clear();
	}

	public boolean isExpired() {
		return System.currentTimeMillis() >= expiresAtMs;
	}

	public int getChunkX() {
		return ((int) Math.floor(x)) >> 4;
	}

	public int getChunkZ() {
		return ((int) Math.floor(z)) >> 4;
	}

	public World resolveWorld() {
		return Bukkit.getWorld(world);
	}

	public Location getAnchor(World world) {
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	public Location getAnchor() {
		return getAnchor(resolveWorld());
	}

	public Location getVisualBase(World world) {
		Location anchor = getAnchor(world);
		if (anchor == null) return null;
		return anchor.clone().add(0, Cache.spawnedClueVisualYOffset, 0);
	}

	public Location getVisualBase() {
		return getVisualBase(resolveWorld());
	}

	public boolean isChunkLoaded() {
		World w = resolveWorld();
		if (w == null) return false;
		return w.isChunkLoaded(getChunkX(), getChunkZ());
	}
}
