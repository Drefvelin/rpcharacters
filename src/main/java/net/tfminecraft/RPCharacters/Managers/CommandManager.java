package net.tfminecraft.RPCharacters.Managers;

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
import net.tfminecraft.RPCharacters.Permissions;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.enums.Status;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "rpcharacter";
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().equalsIgnoreCase(cmd1) && args.length == 0) return true;
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
				if(CreationManager.activeCreators.containsKey(argPlayer)) {
					p.sendMessage("§c"+argPlayer.getName()+" is busy creating a character");
					return true;
				}
				PlayerData pd = PlayerManager.get(argPlayer);
				if(!pd.hasActiveCharacter()) {
					p.sendMessage("§c"+argPlayer.getName()+" has no character");
					return true;
				}
				pd.getActiveCharacter().setMMOClass(newClass);
				net.Indyuce.mmocore.api.player.PlayerData.get(argPlayer).setClass(mmoClass);
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
			}
			p.sendMessage("§a[RPCharacters] §cError with command format");
			return true;
		}
		return true;
	}

	@EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
		Player p = event.getPlayer();
		if(Permissions.isAdmin(p)) return;
		if(PlayerManager.get(p).hasActiveCharacter() || !Cache.requireCharacter || !p.getGameMode().equals(GameMode.SURVIVAL)) return;
        String message = event.getMessage().toLowerCase();

        if (!(message.startsWith("/rpcharacter") || message.startsWith("/class"))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot use other commands when you have no character, only §e/rpcharacter §cor §e/class");
        }
    }
}
