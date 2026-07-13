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
import net.tfminecraft.RPCharacters.mmocore.ClassService;
import net.tfminecraft.RPCharacters.Utils.ClueGiver;
import net.tfminecraft.RPCharacters.Utils.ClueProgressFormatter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.Utils.TraitChangeService;
import net.tfminecraft.RPCharacters.Loaders.PermadeathZoneLoader;
import net.tfminecraft.RPCharacters.clues.discovery.ClueAdminModeService;
import net.tfminecraft.RPCharacters.permadeath.PermadeathAdminCommands;
import net.tfminecraft.RPCharacters.clues.discovery.ClueDiscoveryVisualManager;
import net.tfminecraft.RPCharacters.command.CharCommand;
import net.tfminecraft.RPCharacters.enums.Status;
import net.tfminecraft.RPCharacters.identity.TempAliasService;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;

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
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			if (!Permissions.isAdmin(sender)) {
				RPTexts.send(sender, RPTexts.ERROR + "You do not have permission to use this command.");
				return true;
			}
			RPCharacters.plugin.reloadConfigs(sender);
			return true;
		}
		if (!(sender instanceof Player)) {
			RPTexts.send(sender, RPTexts.ERROR + "Only players can use this command.");
			return true;
		}

		Player p = (Player) sender;
		if(args.length == 0) return true;
			if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("create") && args.length == 1) {
				if(CreationManager.activeCreators.containsKey(p)) {
					RPTexts.send(p, RPTexts.ERROR + "You are already creating a character");
					return true;
				}
				PlayerData pd = PlayerManager.get(p);
				if(!CharacterSlotService.hasFreeSlot(p, pd)){
					RPTexts.send(p, RPTexts.ERROR + "You don't have a free character slot!");
					return true;
				}
				CreationManager.initiateCreation(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("next") && args.length == 1) {
				if(!CreationManager.activeCreators.containsKey(p)) {
					RPTexts.send(p, RPTexts.ERROR + "You dont have an active creator");
					return true;
				}
				if(!CreationManager.activeCreators.get(p).canNext()) {
					RPTexts.send(p, RPTexts.ERROR + "You cannot use /rpcharacter next on this stage");
					return true;
				}
				CreationManager.next(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("menu") && args.length >= 1) {
				Player target = p;
				if(args.length > 1) {
					Player argPlayer = Bukkit.getPlayerExact(args[1]);
					if(argPlayer != null && !Permissions.isAdmin(sender)) {
						RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to view other player's profiles");
						return true;
					} else if(argPlayer != null) {
						target = argPlayer;
					}
				}
				if(CreationManager.activeCreators.containsKey(p)) {
					RPTexts.send(p, RPTexts.ERROR + "You are busy creating a character");
					return true;
				}
				InventoryManager inv = new InventoryManager();
				inv.profileView(p, target);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("cancel") && args.length == 1) {
				if(!CreationManager.activeCreators.containsKey(p)) {
					RPTexts.send(p, RPTexts.ERROR + "You dont have an active creator");
					return true;
				}
				CreationManager.activeCreators.get(p).cancel();
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("edit") && args.length == 1) {
				if(CreationManager.activeCreators.containsKey(p) && !CreationManager.activeCreators.get(p).isEditing()) {
					RPTexts.send(p, RPTexts.ERROR + "You are busy creating a character");
					return true;
				}
				CreationManager.initiateEdit(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("edit") && args.length == 2) {
				if(CreationManager.activeCreators.containsKey(p) && !CreationManager.activeCreators.get(p).isEditing()) {
					RPTexts.send(p, RPTexts.ERROR + "You are busy creating a character");
					return true;
				}
				CreationManager.initiateEditEntry(p, args[1]);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setclass") && args.length == 3) {
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(argPlayer != null && !Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to view other player's profiles");
					return true;
				}
				String newClass = args[2].toUpperCase();
				PlayerClass mmoClass = MMOCore.plugin.classManager.get(newClass);
				if(mmoClass == null) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "No class by the id " + newClass);
					return true;
				}
				if(argPlayer != null && CreationManager.activeCreators.containsKey(argPlayer)) {
					CreationManager.activeCreators.get(argPlayer).getCharacter().setMMOClass(newClass);
					RPTexts.send(argPlayer, RPTexts.WARN + "Draft class set to " + mmoClass.getName()
							+ " (applies when creation finishes)");
					if(sender != argPlayer) {
						RPTexts.send(p, RPTexts.WARN + "Draft class for " + argPlayer.getName()
								+ " set to " + mmoClass.getName());
					}
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(!pd.hasActiveCharacter()) {
					RPTexts.send(p, RPTexts.ERROR + argPlayer.getName() + " has no character");
					return true;
				}
				pd.getActiveCharacter().setMMOClass(newClass);
				boolean alreadyOnClass = ClassService.isOnClass(argPlayer, newClass);
				ClassService.applyClass(argPlayer, newClass);
				if (!alreadyOnClass) {
					RPTexts.send(argPlayer, RPTexts.WARN + "Your class was changed to " + mmoClass.getName());
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("seteighteen") && args.length == 3) {
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				if(argPlayer == null) {
					RPTexts.send(p, RPTexts.ERROR + "No player found");
					return true;
				}
				Boolean value = Boolean.parseBoolean(args[2]);
				PlayerData pd = PlayerManager.get(argPlayer);
				pd.setEighteen(value);
				RPTexts.send(p, RPTexts.WARN + "18+ value for " + argPlayer.getName()
						+ " changed to " + RPTexts.INFO + value.toString());
				RPTexts.send(argPlayer, RPTexts.WARN + "18+ value changed to " + RPTexts.INFO + value.toString());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("skipcooldown") && args.length == 2) {
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				if(argPlayer == null) {
					RPTexts.send(p, RPTexts.ERROR + "No player found");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				pd.clearCharacterSwitchCooldown();
				RPTexts.send(p, RPTexts.WARN + "Removed cooldown for " + argPlayer);
				RPTexts.send(argPlayer, RPTexts.WARN + "Character Cooldown has been skipped");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("adminmode") && args.length == 2) {
				if(!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				boolean enable;
				if (args[1].equalsIgnoreCase("on")) {
					enable = true;
				} else if (args[1].equalsIgnoreCase("off")) {
					enable = false;
				} else {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Usage: /rpcharacter adminmode <on|off>");
					return true;
				}
				ClueAdminModeService.setEnabled(p, enable);
				if (enable) {
					RPTexts.sendPrefixed(p, RPTexts.WARN + "Clue admin mode " + RPTexts.SUCCESS + "enabled"
							+ RPTexts.WARN + ". You can see all nearby clues at full clarity.");
				} else {
					RPTexts.sendPrefixed(p, RPTexts.WARN + "Clue admin mode " + RPTexts.ERROR + "disabled"
							+ RPTexts.WARN + ".");
					ClueDiscoveryVisualManager.get().clearViewer(p.getUniqueId());
				}
				ClueDiscoveryVisualManager.get().refreshViewer(p);
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("dismisspdwarning")) {
				PlayerData pd = PlayerManager.get(p);
				if (pd == null) {
					return true;
				}
				pd.setPermadeathTutorialDismissed(true);
				RPCharacters.getPlayerManager().savePlayer(p);
				RPTexts.sendPrefixed(p, RPTexts.SUCCESS + "Got it! " + RPTexts.MUTED
						+ "You won't see the permadeath tutorial again.");
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setworldspawn")) {
				if (!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				PermadeathZoneLoader.saveWorldSpawn(p.getLocation());
				RPTexts.sendPrefixed(p, RPTexts.WARN + "Permadeath world spawn set to your current location.");
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("injure")) {
				if (!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				return PermadeathAdminCommands.handleInjure(sender, args);
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("permakill")) {
				if (!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				return PermadeathAdminCommands.handlePermakill(sender, args);
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("addtrait") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(argPlayer == null) {
					RPTexts.send(p, RPTexts.ERROR + "No player found");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(pd == null || !pd.hasActiveCharacter()) {
					RPTexts.send(p, RPTexts.ERROR + argPlayer.getName() + " has no active character");
					return true;
				}
				Trait trait = TraitLoader.getByString(args[2]);
				if(trait == null) {
					RPTexts.send(p, RPTexts.ERROR + "No trait found with the id " + args[2]);
					return true;
				}

				RPCharacter character = pd.getActiveCharacter();
				for(Trait current : character.getTraits()) {
					if(current.getId().equalsIgnoreCase(trait.getId())) {
						RPTexts.send(p, RPTexts.ERROR + argPlayer.getName() + " already has the trait " + trait.getId());
						return true;
					}
				}
				TraitChangeService.addTrait(argPlayer, character, trait);
				RPTexts.send(p, RPTexts.SUCCESS + "Added trait " + RPTexts.WARN + trait.getId()
						+ RPTexts.SUCCESS + " to " + RPTexts.WARN + argPlayer.getName());
				TraitChangeService.sendGainedMessage(argPlayer, trait);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("removetrait") && args.length == 3) {
				if(!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				Player argPlayer = Bukkit.getPlayerExact(args[1]);
				if(argPlayer == null) {
					RPTexts.send(p, RPTexts.ERROR + "No player found");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(pd == null || !pd.hasActiveCharacter()) {
					RPTexts.send(p, RPTexts.ERROR + argPlayer.getName() + " has no active character");
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
					RPTexts.send(p, RPTexts.ERROR + argPlayer.getName() + " does not have the trait " + args[2]);
					return true;
				}

				TraitChangeService.removeTrait(argPlayer, character, trait);
				RPTexts.send(p, RPTexts.SUCCESS + "Removed trait " + RPTexts.WARN + trait.getId()
						+ RPTexts.SUCCESS + " from " + RPTexts.WARN + argPlayer.getName());
				TraitChangeService.sendLostMessage(argPlayer, trait);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("clues")) {
				if (args.length == 1) {
					if (CreationManager.activeCreators.containsKey(p)) {
						RPTexts.send(p, RPTexts.ERROR + "You are busy creating a character");
						return true;
					}
					PlayerData pd = PlayerManager.get(p);
					if (!pd.hasActiveCharacter()) {
						RPTexts.send(p, RPTexts.ERROR + "You have no active character");
						return true;
					}
					InventoryManager inv = new InventoryManager();
					inv.cluesView(p, pd.getActiveCharacter());
					return true;
				}
				if (args.length == 2) {
					if (!Permissions.isAdmin(sender)) {
						RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
						return true;
					}
					Player target = Bukkit.getPlayerExact(args[1]);
					if (target == null) {
						RPTexts.send(p, RPTexts.ERROR + "No player found");
						return true;
					}
					PlayerData pd = PlayerManager.get(target);
					if (pd == null || !pd.hasActiveCharacter()) {
						RPTexts.send(p, RPTexts.ERROR + target.getName() + " has no active character");
						return true;
					}
					RPCharacter character = pd.getActiveCharacter();
					RPTexts.sendPrefixed(p, RPTexts.MUTED + "Clues for " + RPTexts.WARN + character.getName()
							+ RPTexts.MUTED + " (" + target.getName() + ")");
					RPTexts.send(p, RPTexts.MUTED + "Progress: " + ClueProgressFormatter.progressLine(character));
					int i = 1;
					for (String clue : character.getPlayerClues()) {
						RPTexts.send(p, RPTexts.MUTED + i + ". " + clue);
						i++;
					}
					if (!ClueGiver.getAutomaticClues(character).isEmpty()) {
						RPTexts.send(p, RPTexts.MUTED + "An automatic race clue is included in the leave-behind pool.");
					}
					return true;
				}
				RPTexts.sendPrefixed(p, RPTexts.ERROR + "Error with command format");
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("placeclue")) {
				if (!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				if (args.length < 2) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Usage: /rpcharacter placeclue <text...>");
					return true;
				}
				String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
				if (text.isEmpty()) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Clue text cannot be empty.");
					return true;
				}
				RPCharacters.getPlaceClueManager().startAwaiting(p, text);
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("clearclues")) {
				if (!Permissions.isAdmin(sender)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have access to this command");
					return true;
				}
				if (args.length < 2) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Usage: /rpcharacter clearclues <radius>");
					return true;
				}
				double radius;
				try {
					radius = Double.parseDouble(args[1]);
				} catch (NumberFormatException ex) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Radius must be a number");
					return true;
				}
				if (radius <= 0) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Radius must be greater than 0");
					return true;
				}
				int removed = SpawnedClueManager.get().clearInRadius(p.getLocation(), radius);
				RPTexts.sendPrefixed(p, RPTexts.MUTED + "Removed " + RPTexts.WARN + removed
						+ RPTexts.MUTED + " spawned clue(s) within " + RPTexts.WARN + radius
						+ RPTexts.MUTED + " blocks. Chest clue items were not affected.");
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("tempalias")) {
				if (!p.hasPermission(Cache.personaTempaliasPermission)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have permission to use this command.");
					return true;
				}
				if (args.length < 2) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Usage: /rpcharacter tempalias <name...>|clear");
					return true;
				}
				String input = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
				if (input.equalsIgnoreCase("clear")) {
					TempAliasService.clear(p);
					RPTexts.sendPrefixed(p, RPTexts.MUTED + "Cleared your session temp alias.");
					return true;
				}
				String error = TempAliasService.set(p, input);
				if (error != null) {
					RPTexts.send(p, error);
					return true;
				}
				RPTexts.sendPrefixed(p, RPTexts.MUTED + "Session temp alias set to " + RPTexts.WARN
						+ TempAliasService.getPlain(p) + RPTexts.MUTED + ".");
				return true;
			} else if (cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("sethidden")) {
				if (!p.hasPermission(Cache.personaCharacterHiddenPermission)) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "You do not have permission to use this command.");
					return true;
				}
				if (args.length < 2) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Usage: /rpcharacter sethidden <slug> [clear]");
					return true;
				}
				PlayerData pd = PlayerManager.get(p);
				if (pd == null) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "Player data not loaded.");
					return true;
				}
				String slug = args[1];
				RPCharacter character = pd.getCharacterBySlug(slug);
				if (character == null) {
					RPTexts.sendPrefixed(p, RPTexts.ERROR + "No character with id " + RPTexts.WARN + slug
							+ RPTexts.ERROR + ".");
					return true;
				}
				boolean forceClear = args.length >= 3 && args[2].equalsIgnoreCase("clear");
				if (forceClear) {
					character.setHidden(false);
					RPCharacters.getPlayerManager().savePlayer(p);
					RPTexts.sendPrefixed(p, RPTexts.MUTED + "Character " + RPTexts.WARN + character.getSlug()
							+ RPTexts.MUTED + " is no longer hidden.");
					return true;
				}
				character.setHidden(!character.isHidden());
				RPCharacters.getPlayerManager().savePlayer(p);
				if (character.isHidden()) {
					RPTexts.sendPrefixed(p, RPTexts.MUTED + "Character " + RPTexts.WARN + character.getSlug()
							+ RPTexts.MUTED + " is now hidden from TAB/list.");
				} else {
					RPTexts.sendPrefixed(p, RPTexts.MUTED + "Character " + RPTexts.WARN + character.getSlug()
							+ RPTexts.MUTED + " is no longer hidden.");
				}
				return true;
			}
		RPTexts.sendPrefixed(p, RPTexts.ERROR + "Error with command format");
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
        RPTexts.send(event.getPlayer(), RPTexts.ERROR + "You cannot use other commands when you have no character, only "
				+ RPTexts.COMMAND + "/rpcharacter");
    }
}
