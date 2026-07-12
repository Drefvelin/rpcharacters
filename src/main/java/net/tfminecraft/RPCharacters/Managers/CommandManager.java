package net.tfminecraft.RPCharacters.Managers;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Permissions;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.mmocore.ClassService;
import net.tfminecraft.RPCharacters.Utils.ClueGiver;
import net.tfminecraft.RPCharacters.command.CharCommand;
import net.tfminecraft.RPCharacters.enums.Status;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "rpcharacter";
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (!cmd.getName().equalsIgnoreCase(cmd1)) {
			return true;
		}
		if (args.length >= 1 && CharCommand.isPersonaSubcommand(args[0])) {
			return CharCommand.handle(sender, label, args);
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage("§cOnly players can use this command.");
			return true;
		}

		Player p = (Player) sender;
		if(args.length == 0) return true;
			if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("create") && args.length == 1) {
				if(CreationManager.activeCreators.containsKey(p)) {
					p.sendMessage("§cYou are already creating a character");
					return true;
				}
				PlayerData pd = PlayerManager.get(p);
				if(pd.getCharacters(Status.ALIVE).size() >= Cache.maxAlive){
					p.sendMessage("§cYou don't have a free character slot!");
					return true;
				}
				CreationManager.initiateCreation(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("next") && args.length == 1) {
				if(!CreationManager.activeCreators.containsKey(p)) {
					p.sendMessage("§cYou dont have an active creator");
					return true;
				}
				if(!CreationManager.activeCreators.get(p).canNext()) {
					p.sendMessage("§cYou cannot use /rpcharacter next on this stage");
					return true;
				}
				CreationManager.next(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("menu") && args.length >= 1) {
				Player target = p;
				if(args.length > 1) {
					Player argPlayer = Bukkit.getPlayerExact(args[1]);
					if(argPlayer != null && !Permissions.isAdmin(sender)) {
						p.sendMessage("§a[RPCharacters] §cYou do not have access to view other player's profiles");
						return true;
					} else if(argPlayer != null) {
						target = argPlayer;
					}
				}
				if(CreationManager.activeCreators.containsKey(p)) {
					p.sendMessage("§cYou are busy creating a character");
					return true;
				}
				InventoryManager inv = new InventoryManager();
				inv.profileView(p, target);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("cancel") && args.length == 1) {
				if(!CreationManager.activeCreators.containsKey(p)) {
					p.sendMessage("§cYou dont have an active creator");
					return true;
				}
				CreationManager.activeCreators.get(p).cancel();
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("edit") && args.length == 2) {
				if(CreationManager.activeCreators.containsKey(p)) {
					p.sendMessage("§cYou have an active creator");
					return true;
				}
				if(!PlayerManager.get(p).hasActiveCharacter()) {
					p.sendMessage("§cYou have no character");
					return true;
				}
				String key = args[1];
				if(!Cache.editableTraits.contains(key)) {
					return true;
				}
				RPCharacters.getPlayerManager().traitEdit(p, key);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setclass") && args.length == 3) {
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(argPlayer != null && !Permissions.isAdmin(sender)) {
					p.sendMessage("§a[RPCharacters] §cYou do not have access to view other player's profiles");
					return true;
				}
				String newClass = args[2].toUpperCase();
				PlayerClass mmoClass = MMOCore.plugin.classManager.get(newClass);
				if(mmoClass == null) {
					p.sendMessage("§a[RPCharacters] §cNo class by the id "+newClass);
					return true;
				}
				if(argPlayer != null && CreationManager.activeCreators.containsKey(argPlayer)) {
					CreationManager.activeCreators.get(argPlayer).getCharacter().setMMOClass(newClass);
					argPlayer.sendMessage("§eDraft class set to "+mmoClass.getName()+" (applies when creation finishes)");
					if(sender != argPlayer) {
						p.sendMessage("§eDraft class for "+argPlayer.getName()+" set to "+mmoClass.getName());
					}
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(!pd.hasActiveCharacter()) {
					p.sendMessage("§c"+argPlayer.getName()+" has no character");
					return true;
				}
				pd.getActiveCharacter().setMMOClass(newClass);
				ClassService.applyClass(argPlayer, newClass);
				argPlayer.sendMessage("§eYour class was changed to "+mmoClass.getName());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("seteighteen") && args.length == 3) {
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[RPCharacters] §cYou do not have access to this command");
					return true;
				}
				if(argPlayer == null) {
					p.sendMessage("§cNo player found");
					return true;
				}
				Boolean value = Boolean.parseBoolean(args[2]);
				PlayerData pd = PlayerManager.get(argPlayer);
				pd.setEighteen(value);
				p.sendMessage("§e18+ value for "+argPlayer.getName()+" changed to §9"+value.toString());
				argPlayer.sendMessage("§e18+ value changed to §9"+value.toString());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("skipcooldown") && args.length == 2) {
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[RPCharacters] §cYou do not have access to this command");
					return true;
				}
				if(argPlayer == null) {
					p.sendMessage("§cNo player found");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				pd.clearCharacterSwitchCooldown();
				p.sendMessage("§eRemoved cooldown for "+argPlayer);
				argPlayer.sendMessage("§eCharacter Cooldown has been skipped");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("addtrait") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[RPCharacters] §cYou do not have access to this command");
					return true;
				}
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(argPlayer == null) {
					p.sendMessage("§cNo player found");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(pd == null || !pd.hasActiveCharacter()) {
					p.sendMessage("§c"+argPlayer.getName()+" has no active character");
					return true;
				}
				Trait trait = TraitLoader.getByString(args[2]);
				if(trait == null) {
					p.sendMessage("§cNo trait found with the id " + args[2]);
					return true;
				}

				RPCharacter character = pd.getActiveCharacter();
				for(Trait current : character.getTraits()) {
					if(current.getId().equalsIgnoreCase(trait.getId())) {
						p.sendMessage("§c" + argPlayer.getName() + " already has the trait " + trait.getId());
						return true;
					}
				}
				if(character.isActive()) {
					Integrator integrator = new Integrator();
					integrator.remove(argPlayer, character, false);
					character.addTrait(trait);
					character.update();
					integrator.integrate(argPlayer, character);
				} else {
					character.addTrait(trait);
					character.update();
				}

				RPCharacters.getPlayerManager().savePlayer(argPlayer);
				RPCharacters.getPlayerManager().reevaluateFreeze(argPlayer);
				p.sendMessage("§aAdded trait §e" + trait.getId() + "§a to §e" + argPlayer.getName());
				argPlayer.sendMessage("§aAn admin added the trait §e" + trait.getName() + "§a to your active character.");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("removetrait") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[RPCharacters] §cYou do not have access to this command");
					return true;
				}
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(argPlayer == null) {
					p.sendMessage("§cNo player found");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(pd == null || !pd.hasActiveCharacter()) {
					p.sendMessage("§c"+argPlayer.getName()+" has no active character");
					return true;
				}

				RPCharacter character = pd.getActiveCharacter();
				Trait trait = null;
				for(Trait current : character.getTraits()) {
					if(current.getId().equalsIgnoreCase(args[2])) {
						trait = current;
						break;
					}
				}
				if(trait == null) {
					p.sendMessage("§c" + argPlayer.getName() + " does not have the trait " + args[2]);
					return true;
				}

				if(character.isActive()) {
					Integrator integrator = new Integrator();
					integrator.remove(argPlayer, character, false);
					character.removeTrait(trait);
					character.update();
					integrator.integrate(argPlayer, character);
				} else {
					character.removeTrait(trait);
					character.update();
				}

				RPCharacters.getPlayerManager().savePlayer(argPlayer);
				RPCharacters.getPlayerManager().reevaluateFreeze(argPlayer);
				p.sendMessage("§aRemoved trait §e" + trait.getId() + "§a from §e" + argPlayer.getName());
				argPlayer.sendMessage("§cAn admin removed the trait §e" + trait.getName() + "§c from your active character.");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("clues")) {
				if (args.length == 1) {
					if (CreationManager.activeCreators.containsKey(p)) {
						p.sendMessage("§cYou are busy creating a character");
						return true;
					}
					PlayerData pd = PlayerManager.get(p);
					if (!pd.hasActiveCharacter()) {
						p.sendMessage("§cYou have no active character");
						return true;
					}
					InventoryManager inv = new InventoryManager();
					inv.cluesView(p, pd.getActiveCharacter());
					return true;
				}
				if (args.length == 2) {
					if (!Permissions.isAdmin(sender)) {
						p.sendMessage("§a[RPCharacters] §cYou do not have access to this command");
						return true;
					}
					Player target = Bukkit.getPlayerExact(args[1]);
					if (target == null) {
						p.sendMessage("§cNo player found");
						return true;
					}
					PlayerData pd = PlayerManager.get(target);
					if (pd == null || !pd.hasActiveCharacter()) {
						p.sendMessage("§c" + target.getName() + " has no active character");
						return true;
					}
					RPCharacter character = pd.getActiveCharacter();
					p.sendMessage("§a[RPCharacters] §7Clues for §e" + character.getName() + " §7(" + target.getName() + ")");
					p.sendMessage("§7Progress: §e" + character.getPlayerClues().size() + "§7/§e" + character.getCluesNeeded() + " §7required");
					int i = 1;
					for (String clue : character.getPlayerClues()) {
						p.sendMessage("§7" + i + ". " + clue);
						i++;
					}
					if (!ClueGiver.getAutomaticClues(character).isEmpty()) {
						p.sendMessage("§7An automatic race clue is included in the leave-behind pool.");
					}
					return true;
				}
				p.sendMessage("§a[RPCharacters] §cError with command format");
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("clearclues")) {
				if (!Permissions.isAdmin(sender)) {
					p.sendMessage("§a[RPCharacters] §cYou do not have access to this command");
					return true;
				}
				if (args.length < 2) {
					p.sendMessage("§a[RPCharacters] §cUsage: /rpcharacter clearclues <radius>");
					return true;
				}
				double radius;
				try {
					radius = Double.parseDouble(args[1]);
				} catch (NumberFormatException ex) {
					p.sendMessage("§a[RPCharacters] §cRadius must be a number");
					return true;
				}
				if (radius <= 0) {
					p.sendMessage("§a[RPCharacters] §cRadius must be greater than 0");
					return true;
				}
				int removed = SpawnedClueManager.get().clearInRadius(p.getLocation(), radius);
				p.sendMessage("§a[RPCharacters] §7Removed §e" + removed + " §7spawned clue(s) within §e"
						+ radius + " §7blocks. Chest clue items were not affected.");
				return true;
			}
		p.sendMessage("§a[RPCharacters] §cError with command format");
		return true;
	}

	@EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
		Player p = event.getPlayer();
		if(Permissions.isAdmin(p)) return;
		if(PlayerManager.get(p).hasActiveCharacter() || !Cache.requireCharacter || !p.getGameMode().equals(GameMode.SURVIVAL)) return;
        String message = event.getMessage().toLowerCase();

        String raw = event.getMessage().stripLeading();
        if (raw.startsWith("/")) {
        	String withoutSlash = raw.substring(1);
        	int space = withoutSlash.indexOf(' ');
        	String label = (space < 0 ? withoutSlash : withoutSlash.substring(0, space)).toLowerCase(Locale.ROOT);
        	if (ChatLoader.getChannelCommands().contains(label)) {
        		return;
        	}
        }

        if (message.startsWith("/rpcharacter clues")
        		|| message.startsWith("/rpcharacter")
        		|| message.equals("/roll")
        		|| message.startsWith("/roll ")
        		|| message.startsWith("/tfmc roll")) {
        	return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYou cannot use other commands when you have no character, only §e/rpcharacter");
    }
}
