package net.tfminecraft.RPCharacters.Managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Permissions;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.enums.Status;

public class CommandManager implements Listener, CommandExecutor{
	public String cmd1 = "rpcharacter";
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
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
			}  else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("cancel") && args.length == 1) {
				if(!CreationManager.activeCreators.containsKey(p)) {
					p.sendMessage("§cYou dont have an active creator");
					return true;
				}
				CreationManager.activeCreators.get(p).cancel();
			}
		}
		return false;
	}

}
