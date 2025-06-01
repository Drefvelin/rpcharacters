package net.tfminecraft.RPCharacters.Creation.Stages;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;

public class InfoStage extends Stage{
	
	private int interval;
	
	private List<String> messages = new ArrayList<>();
	
	private boolean summary;
	
	public InfoStage(Stage s, ConfigurationSection config) {
		setId(s.getId());
		setRepeat(s.shouldRepeat());
		setAutoNext(s.autoNext());
		setCancelled(s.isCancelled());
		if(s.hasDependency()) setDependency(s.getDependency());
		this.interval = config.getInt("interval");
		this.messages = config.getStringList("messages");
		this.summary = config.getBoolean("should-summarize");
	}
	public InfoStage(InfoStage another) {
		setId(another.getId());
		setRepeat(another.shouldRepeat());
		setAutoNext(another.autoNext());
		setCancelled(another.isCancelled());
		if(another.hasDependency()) setDependency(another.getDependency());
		this.interval = another.getInterval();
		this.messages = another.getMessages();
		this.summary = another.shouldSummarize();
	}
	public int getInterval() {
		return interval;
	}

	public List<String> getMessages() {
		return messages;
	}

	public boolean shouldSummarize() {
		return summary;
	}
	public void runMessage(Player p, String message, List<String> summarized) {
		String type = message.split("\\(")[0];
		String info = StringFormatter.formatHex(message.split("\\(")[1].replace(")", ""));
		if(type.equalsIgnoreCase("title")) {
			p.sendTitle(info, " ", 5, interval-10, 5);
			summarized.add(" ");
			summarized.add("§o"+info);
			summarized.add(" ");
		} else if(type.equalsIgnoreCase("subtitle")) {
			p.sendTitle(" ", info, 5, interval-10, 5);
			summarized.add(info);
		} else if(type.equalsIgnoreCase("chat")) {
			p.sendMessage(info);
			summarized.add(info);
		}
	}
	public void execute(Player p, CharacterCreation cc) {
		List<String> summarized = new ArrayList<String>();
		new BukkitRunnable()
		{
			int i = 0;
			public void run()
			{
				if(isCancelled()) {
					this.cancel();
				}
				if(i >= messages.size()) {
					this.cancel();
					if(summary) {
						p.sendMessage("§7=======================================");
						p.sendMessage(" ");
						for(String m : summarized) {
							p.sendMessage(m);
						}
						p.sendMessage(" ");
						p.sendMessage("§7=======================================");
					}
					if(autoNext()) {
						cc.runStage();
					} else {
						cc.setCanNext(true);
					}
				} else {
					runMessage(p, messages.get(i), summarized);
					i++;
				}
			}
		}.runTaskTimer(RPCharacters.plugin, 0L, interval*1L);
	}
}
