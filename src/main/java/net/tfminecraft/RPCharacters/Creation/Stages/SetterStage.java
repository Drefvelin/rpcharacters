package net.tfminecraft.RPCharacters.Creation.Stages;

import org.apache.commons.lang.WordUtils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;

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
		runMessage(p, message);
	}
	
	public void finish(String n, Player p, CharacterCreation cc) {
		p.sendTitle(" ", "§7"+WordUtils.capitalize(target)+" set to §e"+n, 5, 50, 5);
		p.sendMessage("§7"+WordUtils.capitalize(target)+" set to §e"+n);
		cc.getCharacter().modify(target, n);
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
