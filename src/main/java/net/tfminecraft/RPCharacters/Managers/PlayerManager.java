package net.tfminecraft.RPCharacters.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import net.Indyuce.mmocore.api.event.PlayerChangeClassEvent;
import net.Indyuce.mmocore.api.event.PlayerExperienceGainEvent;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes.AttributeInstance;
import net.Indyuce.mmocore.api.player.profess.SavedClassInformation;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.Holder.RPCHolder;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Experience.ExperienceModifier;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.enums.ConfirmType;
import net.tfminecraft.RPCharacters.enums.Status;

public class PlayerManager implements Listener{
	
	private static List<PlayerData> data = new ArrayList<>();
	private HashMap<Player, Location> frozen = new HashMap<>();
	private HashMap<Player, ConfirmType> confirm = new HashMap<>();
	private HashMap<Player, RPCharacter> last = new HashMap<>();
	private HashMap<Player, Long> cooldown = new HashMap<>();
	private Database db = new Database();
	
	public static boolean exists(Player p) {
		for(PlayerData pd : data) {
			if(pd.getPlayer().equals(p)) return true;
		}
		return false;
	}
	public static PlayerData get(Player p) {
		for(PlayerData pd : data) {
			if(pd.getPlayer().equals(p)) return pd;
		}
		return null;
	}

	public boolean hasTrait(Player p, String trait) {
		PlayerData pd = get(p);
		if(pd == null) return false;
		RPCharacter active = pd.getActiveCharacter();
		if(active == null) return false;
		for(Trait t : active.getTraits()) {
			if(t.getId().equalsIgnoreCase(trait)) return true;
		}
		return false;
	}

	public boolean isAtFreezeLoc(Player p) {
		Location loc = frozen.get(p);
		if(p.getLocation().getX() != loc.getX()) return false;
		if(p.getLocation().getY() != loc.getY()) return false;
		if(p.getLocation().getZ() != loc.getZ()) return false;
		return true;
	}

	public void toFreezeLoc(Player p) {
		Location loc = frozen.get(p).clone();
		loc.setYaw(p.getLocation().getYaw());
		loc.setPitch(p.getLocation().getPitch());
		p.teleport(loc);
	}
	
	public void start() {
		Bukkit.getLogger().info("[RPCharacters] Starting Player Manager");
		pulse();
		new BukkitRunnable()
		{
			public void run()
			{
				for(Player p : Bukkit.getOnlinePlayers()) {
					if(frozen.containsKey(p)) {
						if(!isAtFreezeLoc(p)) {
							PlayerData pd = get(p);
							if(pd.hasActiveCharacter() || !Cache.requireCharacter || !p.getGameMode().equals(GameMode.SURVIVAL)) {
								frozen.remove(p);
							}
							toFreezeLoc(p);
							if(!CreationManager.activeCreators.containsKey(p)) {
								if(cooldown.containsKey(p)) {
									if(cooldown.get(p) > System.currentTimeMillis()) {
										return;
									}
								}
								cooldown.put(p, System.currentTimeMillis() + (5000));
								p.sendTitle(" ", "§cNo Character!", 5, 50, 5);
								p.sendMessage("§cYou do not have an active character!");
								p.sendMessage("§cCreate one with §e/rpcharacter create");
								p.sendMessage("§cOr do it through §e/rpcharacter menu");
								p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							}
						}
					} else {
						PlayerData pd = get(p);
						if(!pd.hasActiveCharacter() && Cache.requireCharacter) {
							frozen.put(p, p.getLocation());
						}
					}
				}
			}
		}.runTaskTimer(RPCharacters.plugin, 0L, 5L);
	}
	public void pulse() {
		Bukkit.getLogger().info("[RPCharacters] Starting Pulse");
		new BukkitRunnable()
		{
			public void run()
			{
				for(Player p : Bukkit.getOnlinePlayers()) {
					PlayerData pd = get(p);
					if(!pd.hasCooldown()) continue;
					pd.tick();
				}
			}
		}.runTaskTimer(RPCharacters.plugin, 0L, 20L);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		initiatePlayer(p);
	}
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		savePlayer(p);
		data.remove(get(p));
	}
	public void savePlayer(Player p) {
		if(exists(p)) {
			db.savePlayer(get(p));
		}
	}
	
	public void initiatePlayer(Player p) {
		if(!exists(p)) {
			PlayerData pd = db.loadPlayer(p);
			if(pd == null) {
				pd = new PlayerData(p);
			}
			data.add(pd);
		}
	}
	public void confirmClick(Player p, RPCharacter c, ConfirmType t) {
		if(t.equals(ConfirmType.KILL)) {
			if(c.isActive()) {
				c.deactivate();
			}
			c.setStatus(Status.DEAD);
			InventoryManager inv = new InventoryManager();
			inv.characterView(p, c);
		} else if(t.equals(ConfirmType.SWITCH)) {
			PlayerData pd = get(p);
			pd.setActiveCharacter(c);
			InventoryManager inv = new InventoryManager();
			inv.characterView(p, c);
		}
	}

	public void traitEdit(Player p, String key) {
		for(Stage s : StageLoader.getNew()) {
			if(!(s instanceof SelectionStage)) continue;
			SelectionStage stage = new SelectionStage((SelectionStage) s);
			if(!stage.getKey().equals(key)) continue;
			if(stage.hasDependency()) {
				if(!stage.getDependency().check(get(p).getActiveCharacter())) {
					p.sendMessage("§cYou do not fulfill the prerequisites to view those traits ("+key+")");
					return;
				}
			}
			InventoryManager inv = new InventoryManager();
			stage.update(get(p));
			inv.selectionView(p, stage, null);
		}
	}

	@EventHandler
	public void selectionClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(!(e.getView().getTopInventory().getHolder() instanceof RPCHolder)) return;
		if(e.getClickedInventory() == null) return;
		if(!e.getClickedInventory().equals(e.getView().getTopInventory())) return;
		RPCHolder h = (RPCHolder) e.getView().getTopInventory().getHolder();
		Player o = h.getOwner();
		if(e.getView().getTitle().equalsIgnoreCase("§7Character Menu")) {
			e.setCancelled(true);
			if(e.getSlot() == Cache.deadSlot) {
				PlayerData pd = get(o);
				if(pd.getCharacters(Status.DEAD).size() > 0) {
					InventoryManager inv = new InventoryManager();
					inv.deadView(p, o);
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				}
			} else if(Cache.characterSlots.contains(e.getSlot())) {
				ItemStack i = e.getCurrentItem();
				if(i.getType().equals(Material.YELLOW_CONCRETE)) {
					if(!p.equals(o)) return;
					p.closeInventory();
					CreationManager.initiateCreation(p);
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				} else if(i.getType().equals(Material.ENDER_PEARL)) {
					if(e.getSlot() == 0) return;
					NamespacedKey key = new NamespacedKey(RPCharacters.plugin, "character_id");
					String id = i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
					if(o == null) {
						p.sendMessage("§cCant find player, maybe they are offline?");
						return;
					}
					PlayerData pd = get(o);
					RPCharacter c = pd.getCharacterById(id);
					if(c == null) {
						p.sendMessage("§cCant find character");
						return;
					}
					InventoryManager inv = new InventoryManager();
					inv.characterView(p, c);
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				}
			}
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Character Info")) {
			e.setCancelled(true);
			if(e.getSlot() == 26) {
				if(o == null) {
					p.sendMessage("§cCant find player, maybe they are offline?");
					return;
				}
				InventoryManager inv = new InventoryManager();
				inv.profileView(p, o);
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(e.getSlot() == 8) {
				ItemStack i = e.getInventory().getItem(10);
				if(!e.getCurrentItem().getType().equals(Material.IRON_AXE)) return;
				if(o == null) {
					p.sendMessage("§cCant find player, maybe they are offline?");
					return;
				}
				NamespacedKey key = new NamespacedKey(RPCharacters.plugin, "character_id");
				String id = i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
				PlayerData pd = get(o);
				RPCharacter c = pd.getCharacterById(id);
				if(c == null) {
					p.sendMessage("§cCant find character");
					return;
				}
				confirm.put(p, ConfirmType.KILL);
				last.put(p, c);
				InventoryManager inv = new InventoryManager();
				inv.confirmView(p);
			} else if(e.getSlot() == 6) {
				ItemStack i = e.getInventory().getItem(10);
				if(!e.getCurrentItem().getType().equals(Material.EMERALD)) return;
				if(o == null) {
					p.sendMessage("§cCant find player, maybe they are offline?");
					return;
				}
				NamespacedKey key = new NamespacedKey(RPCharacters.plugin, "character_id");
				String id = i.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
				PlayerData pd = get(o);
				RPCharacter c = pd.getCharacterById(id);
				if(c == null) {
					p.sendMessage("§cCant find character");
					return;
				}
				confirm.put(p, ConfirmType.SWITCH);
				last.put(p, c);
				InventoryManager inv = new InventoryManager();
				inv.confirmView(p);
			}
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Confirm Action")) {
			e.setCancelled(true);
			if(!confirm.containsKey(p)) return;
			p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			if(e.getSlot() == 11) {
				confirmClick(p, last.get(p), confirm.get(p));
				confirm.remove(p);
				last.remove(p);
			} else if(e.getSlot() == 15) {
				InventoryManager inv = new InventoryManager();
				inv.characterView(p, last.get(p));
				confirm.remove(p);
				last.remove(p);
			}
		} if(e.getView().getTitle().equalsIgnoreCase("§7Dead Characters")) {
			e.setCancelled(true);
			if(e.getCurrentItem().getType().equals(Material.ENDER_PEARL)) {
				NamespacedKey key = new NamespacedKey(RPCharacters.plugin, "character_id");
				String id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
				if(o == null) {
					p.sendMessage("§cCant find player, maybe they are offline?");
					return;
				}
				PlayerData pd = get(o);
				RPCharacter c = pd.getCharacterById(id);
				if(c == null) {
					p.sendMessage("§cCant find character");
					return;
				}
				InventoryManager inv = new InventoryManager();
				inv.characterView(p, c);
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(e.getCurrentItem() != null) {
				if(o == null) {
					p.sendMessage("§cCant find player, maybe they are offline?");
					return;
				}
				InventoryManager inv = new InventoryManager();
				inv.profileView(p, o);
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		}
	}

	@EventHandler
	public void xpGain(PlayerExperienceGainEvent e) {
		Player p = e.getPlayer();
		PlayerData pd = get(p);
		if(!pd.hasActiveCharacter()) return;
		RPCharacter c = pd.getActiveCharacter();
		if(c.getAttributeData().getExperienceModifiers().size() == 0) return;
		String profession = e.getProfession().getId();
		for(ExperienceModifier m : c.getAttributeData().getExperienceModifiers()) {
			if(m.getProfession().equalsIgnoreCase(profession)) {
				double amount = e.getExperience();
				amount *= m.getFactor();
				e.setExperience((int) Math.round(amount));
			}
		}
	}

	@EventHandler
	public void classChange(PlayerChangeClassEvent e) {
		PlayerData pd = get(e.getPlayer());
		if(pd.hasActiveCharacter()) {
			RPCharacter c = pd.getActiveCharacter();
			final Map<String, Integer> map = (new Integrator()).get(e.getPlayer(), c);
			c.setMMOClass(e.getData().getProfess().getId());
			new BukkitRunnable() {
				@Override
				public void run() {
					net.Indyuce.mmocore.api.player.PlayerData mpd = net.Indyuce.mmocore.api.player.PlayerData.get(e.getPlayer());
					for(Map.Entry<String, Integer> entry : map.entrySet()) {
						for(AttributeInstance a : mpd.getAttributes().getInstances()) {
							if(a.getId().equalsIgnoreCase(entry.getKey())) a.setBase(entry.getValue());
						}
					}
				}
			}.runTaskLater(RPCharacters.plugin, 1L);
		}
	}
}
