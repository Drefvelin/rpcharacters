package net.tfminecraft.RPCharacters.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Database.SpawnedClueDatabase;
import net.tfminecraft.RPCharacters.Objects.SpawnedClue;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.ClueHologram;

public class SpawnedClueManager implements Listener {

	private static SpawnedClueManager instance;

	private final Map<UUID, SpawnedClue> byId = new HashMap<>();
	private final Map<ChunkKey, List<UUID>> byChunk = new HashMap<>();
	private boolean dirty = false;

	public static SpawnedClueManager get() {
		if (instance == null) {
			instance = new SpawnedClueManager();
		}
		return instance;
	}

	public void loadAllFromDisk() {
		byId.clear();
		byChunk.clear();
		for (SpawnedClue clue : SpawnedClueDatabase.loadAll()) {
			registerInternal(clue);
			if (clue.isChunkLoaded()) {
				spawnVisuals(clue);
			}
		}
		startAutosave();
	}

	public void register(SpawnedClue clue) {
		registerInternal(clue);
		markDirty();
		if (clue.isChunkLoaded()) {
			spawnVisuals(clue);
		}
	}

	private void registerInternal(SpawnedClue clue) {
		byId.put(clue.getId(), clue);
		ChunkKey key = chunkKey(clue);
		byChunk.computeIfAbsent(key, k -> new ArrayList<>());
		List<UUID> ids = byChunk.get(key);
		if (!ids.contains(clue.getId())) {
			ids.add(clue.getId());
		}
	}

	public SpawnedClue get(UUID id) {
		return byId.get(id);
	}

	public boolean hasClueAt(Location location) {
		if (location == null || location.getWorld() == null) return false;
		String worldName = location.getWorld().getName();
		int blockX = location.getBlockX();
		int blockY = location.getBlockY();
		int blockZ = location.getBlockZ();
		for (SpawnedClue clue : byId.values()) {
			if (clue.isExpired() || !worldName.equals(clue.getWorldName())) continue;
			if (blockX == (int) Math.floor(clue.getX())
					&& blockY == (int) Math.floor(clue.getY())
					&& blockZ == (int) Math.floor(clue.getZ())) {
				return true;
			}
		}
		return false;
	}

	public void remove(SpawnedClue clue) {
		if (clue == null) return;
		removeVisuals(clue);
		byId.remove(clue.getId());
		ChunkKey key = chunkKey(clue);
		List<UUID> ids = byChunk.get(key);
		if (ids != null) {
			ids.remove(clue.getId());
			if (ids.isEmpty()) byChunk.remove(key);
		}
		markDirty();
	}

	/**
	 * Removes spawned world clue markers within {@code radius} blocks of center.
	 * Does not affect clue paper items in inventories or chests.
	 */
	public int clearInRadius(Location center, double radius) {
		if (center == null || center.getWorld() == null || radius <= 0) return 0;

		double radiusSq = radius * radius;
		String worldName = center.getWorld().getName();
		double centerX = center.getX();
		double centerY = center.getY();
		double centerZ = center.getZ();

		List<SpawnedClue> toRemove = new ArrayList<>();
		for (SpawnedClue clue : byId.values()) {
			if (!worldName.equals(clue.getWorldName())) continue;
			double dx = clue.getX() - centerX;
			double dy = clue.getY() - centerY;
			double dz = clue.getZ() - centerZ;
			if ((dx * dx) + (dy * dy) + (dz * dz) <= radiusSq) {
				toRemove.add(clue);
			}
		}
		for (SpawnedClue clue : toRemove) {
			remove(clue);
		}
		return toRemove.size();
	}

	/**
	 * Removes spawned world clue markers linked to the given block via target block coords.
	 * Does not affect clue paper items in inventories or chests.
	 */
	public int clearLinkedToBlock(Location block) {
		if (block == null || block.getWorld() == null) return 0;

		String worldName = block.getWorld().getName();
		int blockX = block.getBlockX();
		int blockY = block.getBlockY();
		int blockZ = block.getBlockZ();

		List<SpawnedClue> toRemove = new ArrayList<>();
		for (SpawnedClue clue : byId.values()) {
			if (!clue.hasTargetBlock()) continue;
			if (!worldName.equals(clue.getWorldName())) continue;
			if (blockX == clue.getTargetBlockX()
					&& blockY == clue.getTargetBlockY()
					&& blockZ == clue.getTargetBlockZ()) {
				toRemove.add(clue);
			}
		}
		for (SpawnedClue clue : toRemove) {
			remove(clue);
		}
		return toRemove.size();
	}

	public void spawnVisuals(SpawnedClue clue) {
		if (clue == null || clue.isExpired()) return;
		runSync(() -> ClueHologram.refresh(clue));
	}

	public void removeVisuals(SpawnedClue clue) {
		if (clue == null) return;
		runSync(() -> {
			ClueHologram.remove(clue);
			markDirty();
		});
	}

	public void removeAllVisuals() {
		runSync(() -> {
			for (SpawnedClue clue : new ArrayList<>(byId.values())) {
				ClueHologram.remove(clue);
			}
		});
	}

	public void startTicks() {
		int particleInterval = Math.max(1, Cache.spawnedClueParticleInterval);
		new BukkitRunnable() {
			@Override
			public void run() {
				tickParticles();
			}
		}.runTaskTimer(RPCharacters.plugin, 0L, particleInterval);

		new BukkitRunnable() {
			@Override
			public void run() {
				purgeExpired();
			}
		}.runTaskTimer(RPCharacters.plugin, 1200L, 1200L);
	}

	private void tickParticles() {
		for (SpawnedClue clue : byId.values()) {
			if (clue.isExpired() || !clue.isVisualsSpawned()) continue;
			World world = clue.resolveWorld();
			if (world == null || world.getPlayers().isEmpty()) continue;
			if (!clue.isChunkLoaded()) continue;
			Location visualBase = clue.getVisualBase(world);
			if (visualBase != null) {
				ClueHologram.spawnClueParticles(clue);
			}
		}
	}

	private void purgeExpired() {
		boolean removedAny = false;
		Iterator<SpawnedClue> iterator = byId.values().iterator();
		List<SpawnedClue> toRemove = new ArrayList<>();
		while (iterator.hasNext()) {
			SpawnedClue clue = iterator.next();
			if (clue.isExpired()) {
				toRemove.add(clue);
			}
		}
		for (SpawnedClue clue : toRemove) {
			remove(clue);
			removedAny = true;
		}
		if (removedAny) markDirty();
	}

	private void startAutosave() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!dirty) return;
				dirty = false;
				saveAllNow();
			}
		}.runTaskTimer(RPCharacters.plugin, 200L, 200L);
	}

	public void saveAllNow() {
		SpawnedClueDatabase.saveAll(byId.values());
	}

	public void shutdown() {
		removeAllVisuals();
		for (SpawnedClue clue : byId.values()) {
			clue.clearDisplayEntityIds();
			clue.setVisualsSpawned(false);
		}
		saveAllNow();
	}

	public void markDisplayDirty() {
		markDirty();
	}

	private void markDirty() {
		dirty = true;
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		Chunk chunk = event.getChunk();
		ChunkKey key = new ChunkKey(event.getWorld().getName(), chunk.getX(), chunk.getZ());
		List<UUID> ids = byChunk.get(key);
		if (ids == null) return;
		for (UUID id : new ArrayList<>(ids)) {
			SpawnedClue clue = byId.get(id);
			if (clue != null && !clue.isExpired()) {
				spawnVisuals(clue);
			}
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		Chunk chunk = event.getChunk();
		ChunkKey key = new ChunkKey(event.getWorld().getName(), chunk.getX(), chunk.getZ());
		List<UUID> ids = byChunk.get(key);
		if (ids == null) return;
		for (UUID id : new ArrayList<>(ids)) {
			SpawnedClue clue = byId.get(id);
			if (clue != null) {
				removeVisuals(clue);
			}
		}
	}

	private ChunkKey chunkKey(SpawnedClue clue) {
		return new ChunkKey(clue.getWorldName(), clue.getChunkX(), clue.getChunkZ());
	}

	private void runSync(Runnable runnable) {
		if (Bukkit.isPrimaryThread()) {
			runnable.run();
		} else {
			Bukkit.getScheduler().runTask(RPCharacters.plugin, runnable);
		}
	}

	private static final class ChunkKey {
		private final String world;
		private final int chunkX;
		private final int chunkZ;

		private ChunkKey(String world, int chunkX, int chunkZ) {
			this.world = world;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof ChunkKey other)) return false;
			return chunkX == other.chunkX && chunkZ == other.chunkZ && Objects.equals(world, other.world);
		}

		@Override
		public int hashCode() {
			return Objects.hash(world, chunkX, chunkZ);
		}
	}
}
