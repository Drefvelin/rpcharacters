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
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public class SetterStage extends Stage{
	private String target;
	
	private String message;
	
	public SetterStage(Stage s, ConfigurationSection config) {
		copyBaseFields(s);
		this.target = config.getString("target");
		this.message = config.getString("message");
	}
	public SetterStage(SetterStage another) {
		copyBaseFields(another);
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
		String info = RPTexts.format(message.split("\\(")[1].replace(")", ""));
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
				RPTexts.title(p, " ", RPTexts.MUTED + "Real age set to " + RPTexts.WARN + "18+", 5, 50, 5);
			} else if(n.equalsIgnoreCase("no")) {
				PlayerManager.get(p).setEighteen(false);
				RPTexts.title(p, " ", RPTexts.MUTED + "Real age set to " + RPTexts.WARN + "below 18", 5, 50, 5);
			} else {
				RPTexts.send(p, RPTexts.ERROR + "Write " + RPTexts.WARN + "yes " + RPTexts.MUTED + "or " + RPTexts.WARN + "no.");
				return;
			}
			scheduleAdvance(cc);
			return;
		}

		if (!isAlphabetic(n)) {
			RPTexts.send(p, RPTexts.ERROR + "Letters only.");
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
		RPTexts.title(p, " ", RPTexts.MUTED + WordUtils.capitalize(target) + " set to " + RPTexts.WARN + n, 5, 50, 5);
		RPTexts.send(p, RPTexts.MUTED + WordUtils.capitalize(target) + " set to " + RPTexts.WARN + n);
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
		RPTexts.title(p, " ", RPTexts.MUTED + "Description saved", 5, 50, 5);
		RPTexts.send(p, RPTexts.MUTED + "Description saved (" + RPTexts.WARN + text.length() + RPTexts.MUTED + " characters).");
		scheduleAdvance(cc);
	}

	private void handleAge(String input, Player p, CharacterCreation cc) {
		Integer age = parseAge(input);
		if (age == null) {
			RPTexts.send(p, RPTexts.ERROR + "Whole number only. e.g. " + RPTexts.WARN + "30" + RPTexts.ERROR + ".");
			return;
		}
		if (age <= 0) {
			RPTexts.send(p, RPTexts.ERROR + "Age must be greater than 0.");
			return;
		}

		Race race = cc.getCharacter().getRace();
		if (race == null) {
			RPTexts.send(p, RPTexts.ERROR + "Pick a race first.");
			return;
		}

		int ageMin = Cache.calendarAgeMinimum;
		int ageMax = race.getAgeMax();
		if (age < ageMin || age > ageMax) {
			RPTexts.send(p, RPTexts.ERROR + "Age must be between " + RPTexts.WARN + ageMin + " " + RPTexts.ERROR + "and "
					+ RPTexts.WARN + ageMax + " " + RPTexts.ERROR + "for your race.");
			return;
		}

		String birthday = AgeCalculator.birthdayFromAge(age, FantasyCalendar.getCurrentDate());
		cc.getCharacter().setBirthday(birthday);
		String formattedBirthday = FantasyCalendar.formatBirthday(birthday);
		RPTexts.title(p, " ", RPTexts.MUTED + "Age set to " + RPTexts.WARN + age, 5, 50, 5);
		RPTexts.send(p, RPTexts.MUTED + "Age set to " + RPTexts.WARN + age + RPTexts.MUTED + " (born "
				+ RPTexts.WARN + formattedBirthday + RPTexts.MUTED + ").");
		scheduleAdvance(cc);
	}

	private static Integer parseAge(String input) {
		if (input == null || input.isBlank()) {
			return null;
		}
		String trimmed = input.trim().replace(',', '.');
		if (trimmed.contains(".")) {
			return null;
		}
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private void scheduleAdvance(CharacterCreation cc) {
		new BukkitRunnable()
		{
			public void run()
			{
				if (cc.isEditingFromSummary()) {
					cc.returnToSummary();
				} else if(autoNext()) {
					cc.runStage();
				} else {
					cc.setCanNext(true);
				}
			}
		}.runTaskLater(RPCharacters.plugin, 60L);
	}

}
