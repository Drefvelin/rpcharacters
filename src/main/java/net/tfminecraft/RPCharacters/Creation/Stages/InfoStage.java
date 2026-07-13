package net.tfminecraft.RPCharacters.Creation.Stages;



import java.util.ArrayList;

import java.util.List;



import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;



import net.tfminecraft.RPCharacters.RPCharacters;

import net.tfminecraft.RPCharacters.Creation.CharacterCreation;

import net.tfminecraft.RPCharacters.Creation.Stage;

import net.tfminecraft.RPCharacters.Utils.RPTexts;



public class InfoStage extends Stage{

	

	private int interval;

	

	private List<String> messages = new ArrayList<>();

	

	public InfoStage(Stage s, ConfigurationSection config) {

		copyBaseFields(s);

		this.interval = config.getInt("interval");

		this.messages = config.getStringList("messages");

	}

	public InfoStage(InfoStage another) {

		copyBaseFields(another);

		this.interval = another.getInterval();

		this.messages = another.getMessages();

	}

	public int getInterval() {

		return interval;

	}



	public List<String> getMessages() {

		return messages;

	}



	public void runMessage(Player p, String message) {

		String type = message.split("\\(")[0];

		String info = RPTexts.format(message.split("\\(")[1].replace(")", ""));

		if(type.equalsIgnoreCase("title")) {

			p.sendTitle(info, " ", 5, interval-10, 5);

		} else if(type.equalsIgnoreCase("subtitle")) {

			p.sendTitle(" ", info, 5, interval-10, 5);

		} else if(type.equalsIgnoreCase("chat")) {

			p.sendMessage(info);

		}

	}

	public void execute(Player p, CharacterCreation cc) {

		new BukkitRunnable()

		{

			int i = 0;

			public void run()

			{

				if(isCancelled() || cc.isCancelled()) {

					this.cancel();

				}

				if(i >= messages.size()) {

					this.cancel();

					if(autoNext()) {

						cc.runStage();

					} else {

						cc.setCanNext(true);

					}

				} else {

					runMessage(p, messages.get(i));

					i++;

				}

			}

		}.runTaskTimer(RPCharacters.plugin, 0L, interval*1L);

	}

}

