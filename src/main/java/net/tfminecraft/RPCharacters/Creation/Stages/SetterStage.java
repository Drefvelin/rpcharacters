package net.tfminecraft.RPCharacters.Creation.Stages;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.plugins.tlibs.shaded.lang3.text.WordUtils;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.calendar.AgeCalculator;
import net.tfminecraft.RPCharacters.calendar.FantasyCalendar;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.persona.AliasValidator;
import net.tfminecraft.RPCharacters.persona.DescriptionValidator;

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

	@SuppressWarnings("deprecation")
	public void finish(String n, Player p, CharacterCreation cc) {
		if (target != null && target.equalsIgnoreCase("age")) {
			handleAge(n, p, cc);
			return;
		}

		if (target != null && target.equalsIgnoreCase("description")) {
			handleDescription(n, p, cc);
			return;
		}

		if (target != null && target.equalsIgnoreCase("real_age")) {
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
			scheduleAdvance(cc);
			return;
		}

		if (!isAlphabetic(n)) {
			p.sendMessage("§cInvalid input! Only letters are allowed.");
			return;
		}

		n = capitalizeWords(n);
		if (target != null && target.equalsIgnoreCase("name")) {
			String nameError = AliasValidator.validateCharacterName(n);
			if (nameError != null) {
				p.sendMessage(nameError);
				return;
			}
		}
		p.sendTitle(" ", "§7"+WordUtils.capitalize(target)+" set to §e"+n, 5, 50, 5);
		p.sendMessage("§7"+WordUtils.capitalize(target)+" set to §e"+n);
		cc.getCharacter().modify(target, n);
		scheduleAdvance(cc);
	}

	private void handleDescription(String input, Player p, CharacterCreation cc) {
		String text = ClueFormatter.stripColor(input != null ? input.trim() : "");
		String error = DescriptionValidator.validate(text);
		if (error != null) {
			p.sendMessage(error);
			return;
		}
		cc.getCharacter().setPersonaDescription(text);
		p.sendTitle(" ", "§7Description saved", 5, 50, 5);
		p.sendMessage("§7Description saved (§e" + text.length() + "§7 characters).");
		scheduleAdvance(cc);
	}

	private void handleAge(String input, Player p, CharacterCreation cc) {
		Double age = parseAge(input);
		if (age == null) {
			p.sendMessage("§cInvalid age. Enter a number such as §e30 §cor §e33.7§c.");
			return;
		}
		if (age <= 0) {
			p.sendMessage("§cAge must be greater than 0.");
			return;
		}

		Race race = cc.getCharacter().getRace();
		if (race == null) {
			p.sendMessage("§cSelect a race before setting age.");
			return;
		}

		int ageMin = Cache.calendarAgeMinimum;
		int ageMax = race.getAgeMax();
		if (age < ageMin || age > ageMax) {
			p.sendMessage("§cAge must be between §e" + ageMin + " §cand §e" + ageMax
					+ " §cfor your race.");
			return;
		}

		String birthday = AgeCalculator.birthdayFromAge(age, FantasyCalendar.getCurrentDate());
		cc.getCharacter().setBirthday(birthday);
		String formattedAge = AgeCalculator.formatAge(birthday);
		p.sendTitle(" ", "§7Age set to §e" + formattedAge, 5, 50, 5);
		p.sendMessage("§7Age set to §e" + formattedAge + "§7.");
		scheduleAdvance(cc);
	}

	private static Double parseAge(String input) {
		if (input == null || input.isBlank()) {
			return null;
		}
		try {
			return Double.parseDouble(input.trim().replace(',', '.'));
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private void scheduleAdvance(CharacterCreation cc) {
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
