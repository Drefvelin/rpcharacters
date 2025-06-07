package net.tfminecraft.RPCharacters.Creation.Stages;

import org.apache.commons.lang.WordUtils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;

public class SetterStage extends Stage{
	private String target;
	
	private String message;
	
	public SetterStage(Stage s, ConfigurationSection config) {
		setId(s.getId());
		setRepeat(s.shouldRepeat());
		setAutoNext(s.autoNext());
		setCancelled(s.isCancelled());
		if(s.hasDependency()) setDependency(s.getDependency());
		this.target = config.getString("target");
		this.message = config.getString("message");
	}
	public SetterStage(SetterStage another) {
		setId(another.getId());
		setRepeat(another.shouldRepeat());
		setAutoNext(another.autoNext());
		setCancelled(another.isCancelled());
		if(another.hasDependency()) setDependency(another.getDependency());
		this.target = another.getTarget();
		this.message = another.getMessage();
	}
	public String getTarget() {
		return target;
	}
	
	public String getMessage() {
		return message;
	}

	public void runMessage(Player p, String message) {
		String type = message.split("\\(")[0];
		String info = message.split("\\(")[1].replace(")", "");
		if(type.equalsIgnoreCase("title")) {
			p.sendTitle(info, " ", 5, 50, 5);
		} else if(type.equalsIgnoreCase("subtitle")) {
			p.sendTitle(" ", info, 5, 50, 5);
		} else if(type.equalsIgnoreCase("chat")) {
			p.sendMessage(info);
		}
	}
	public void execute(Player p, CharacterCreation cc) {
		if(cc.isCancelled()) return;
		runMessage(p, message);
	}

	public boolean isAlphabetic(String input) {
		return input != null && input.matches("^[a-zA-Z ]+$");
	}

	public String capitalizeWords(String input) {
		if (input == null || input.isEmpty()) return input;
		
		String[] words = input.toLowerCase().split("\\s+");
		StringBuilder sb = new StringBuilder();
		
		for (String word : words) {
			if (word.length() > 0) {
				sb.append(Character.toUpperCase(word.charAt(0)));
				sb.append(word.substring(1));
				sb.append(" ");
			}
		}
		
		return sb.toString().trim();
	}



	
	public void finish(String n, Player p, CharacterCreation cc) {
		if (!isAlphabetic(n)) {
			p.sendMessage("§cInvalid input! Only letters are allowed.");
			return;
		}

		if(target.equalsIgnoreCase("real_age")) {
			if(n.equalsIgnoreCase("yes")) {
				PlayerManager.get(p).setEighteen(true);
				p.sendTitle(" ", "§7Real age set to §e18+", 5, 50, 5);
			} else if(n.equalsIgnoreCase("no")) {
				PlayerManager.get(p).setEighteen(false);
				p.sendTitle(" ", "§7Real age set to §ebelow 18", 5, 50, 5);
			} else {
				p.sendMessage("§cInvalid input! write either §eyes §cor §eno");
				return;
			}
		} else {
			n = capitalizeWords(n);
			p.sendTitle(" ", "§7"+WordUtils.capitalize(target)+" set to §e"+n, 5, 50, 5);
			p.sendMessage("§7"+WordUtils.capitalize(target)+" set to §e"+n);
			cc.getCharacter().modify(target, n);
		}
		new BukkitRunnable()
		{
			public void run()
			{
				if(autoNext()) {
					cc.runStage();
				} else {
					cc.setCanNext(true);
				}
			}
		}.runTaskLater(RPCharacters.plugin, 60L);
	}

}
