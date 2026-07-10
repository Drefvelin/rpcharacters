package net.tfminecraft.RPCharacters.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Managers.SpawnedClueManager;
import net.tfminecraft.RPCharacters.Objects.SpawnedClue;
import net.tfminecraft.RPCharacters.RPCharacters;

public final class ClueHologram {

	private static final DustOptions RAY_DUST = new DustOptions(Color.fromRGB(0xC8C8C8), 0.8f);
	private static final double ORPHAN_SCAN_RADIUS = 2.5;

	private ClueHologram() {}

	public static void spawn(SpawnedClue clue) {
		refresh(clue);
	}

	public static void refresh(SpawnedClue clue) {
		Location visualBase = clue.getVisualBase();
		if (visualBase == null || visualBase.getWorld() == null) return;

		World world = visualBase.getWorld();
		List<String> lines = ClueFormatter.wrapLore(clue.getClueText(), Cache.spawnedClueLineLength);
		float scale = Cache.spawnedClueScale;
		Transformation transformation = new Transformation(
				new Vector3f(0, 0, 0),
				new AxisAngle4f(0, 0, 0, 1),
				new Vector3f(scale, scale, scale),
				new AxisAngle4f(0, 0, 0, 1));

		removeOrphanDisplays(clue, visualBase);

		List<UUID> ids = clue.getDisplayEntityIds();
		while (ids.size() > lines.size()) {
			removeDisplay(ids.remove(ids.size() - 1));
		}

		boolean idsChanged = false;
		for (int i = 0; i < lines.size(); i++) {
			int stackIndex = lines.size() - 1 - i;
			double y = Cache.spawnedClueFirstLineOffset + (stackIndex * Cache.spawnedClueLineSpacing);
			Location lineLoc = visualBase.clone().add(0, y, 0);
			String text = lines.get(i);

			UUID existingId = i < ids.size() ? ids.get(i) : null;
			TextDisplay display = getOrCreateDisplay(clue, i, existingId, world, lineLoc, text, transformation);
			if (display == null) continue;

			UUID displayId = display.getUniqueId();
			if (i < ids.size()) {
				if (!displayId.equals(ids.get(i))) {
					ids.set(i, displayId);
					idsChanged = true;
				}
			} else {
				ids.add(displayId);
				idsChanged = true;
			}
		}

		clue.setVisualsSpawned(true);
		if (idsChanged) {
			SpawnedClueManager.get().markDisplayDirty();
		}
	}

	public static void remove(SpawnedClue clue) {
		for (UUID entityId : new ArrayList<>(clue.getDisplayEntityIds())) {
			removeDisplay(entityId);
		}
		clue.clearDisplayEntityIds();
		clue.setVisualsSpawned(false);
	}

	private static TextDisplay getOrCreateDisplay(SpawnedClue clue, int lineIndex, UUID entityId,
			World world, Location lineLoc, String text, Transformation transformation) {
		TextDisplay existing = findDisplay(entityId);
		if (existing != null && !existing.isDead()) {
			existing.teleport(lineLoc);
			applyDisplay(existing, clue, lineIndex, text, transformation);
			return existing;
		}

		return world.spawn(lineLoc, TextDisplay.class, td -> applyDisplay(td, clue, lineIndex, text, transformation));
	}

	private static TextDisplay findDisplay(UUID entityId) {
		if (entityId == null) return null;
		Entity entity = Bukkit.getEntity(entityId);
		return entity instanceof TextDisplay textDisplay ? textDisplay : null;
	}

	private static void applyDisplay(TextDisplay display, SpawnedClue clue, int lineIndex,
			String text, Transformation transformation) {
		display.setText(text);
		display.setBillboard(Display.Billboard.CENTER);
		display.setSeeThrough(false);
		display.setShadowed(true);
		display.setInvulnerable(true);
		display.setGravity(false);
		display.setPersistent(true);
		display.setAlignment(TextDisplay.TextAlignment.CENTER);
		display.setTransformation(transformation);
		tagDisplay(display, clue.getId(), lineIndex);
	}

	private static void tagDisplay(TextDisplay display, UUID clueId, int lineIndex) {
		display.getPersistentDataContainer().set(clueIdKey(), PersistentDataType.STRING, clueId.toString());
		display.getPersistentDataContainer().set(clueLineKey(), PersistentDataType.INTEGER, lineIndex);
	}

	private static NamespacedKey clueIdKey() {
		return new NamespacedKey(RPCharacters.plugin, "spawned_clue_id");
	}

	private static NamespacedKey clueLineKey() {
		return new NamespacedKey(RPCharacters.plugin, "spawned_clue_line");
	}

	private static void removeDisplay(UUID entityId) {
		if (entityId == null) return;
		Entity entity = Bukkit.getEntity(entityId);
		if (entity != null && !entity.isDead()) {
			entity.remove();
		}
	}

	private static void removeOrphanDisplays(SpawnedClue clue, Location visualBase) {
		World world = visualBase.getWorld();
		if (world == null) return;

		Set<UUID> tracked = new HashSet<>(clue.getDisplayEntityIds());
		String clueId = clue.getId().toString();

		for (Entity entity : world.getNearbyEntities(visualBase, ORPHAN_SCAN_RADIUS, ORPHAN_SCAN_RADIUS, ORPHAN_SCAN_RADIUS)) {
			if (!(entity instanceof TextDisplay display)) continue;
			if (display.isDead()) continue;
			if (!clueId.equals(display.getPersistentDataContainer().get(clueIdKey(), PersistentDataType.STRING))) {
				continue;
			}
			if (!tracked.contains(display.getUniqueId())) {
				display.remove();
			}
		}
	}

	public static void spawnParticle(Location visualBase) {
		if (visualBase == null || visualBase.getWorld() == null) return;
		visualBase.getWorld().spawnParticle(
				Particle.FIREWORK,
				visualBase,
				3,
				0.05, 0.05, 0.05,
				0.01);
	}

	public static void spawnParticleRay(Location from, Location to) {
		if (from == null || to == null || from.getWorld() == null) return;
		if (!from.getWorld().equals(to.getWorld())) return;

		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double dz = to.getZ() - from.getZ();
		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (distance < 0.05) {
			spawnParticle(from);
			return;
		}

		int steps = Math.max(2, (int) Math.ceil(distance * 4));
		World world = from.getWorld();
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			world.spawnParticle(
					Particle.DUST,
					from.getX() + dx * t,
					from.getY() + dy * t,
					from.getZ() + dz * t,
					1,
					0, 0, 0,
					0,
					RAY_DUST);
		}
	}

	public static void spawnClueParticles(SpawnedClue clue) {
		Location visualBase = clue.getVisualBase();
		if (visualBase == null) return;

		spawnParticle(visualBase);
		if (clue.hasTargetBlock()) {
			Location targetCenter = clue.getTargetCenter(visualBase.getWorld());
			if (targetCenter != null) {
				spawnParticleRay(visualBase, targetCenter);
			}
		}
	}
}
