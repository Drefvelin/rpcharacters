package net.tfminecraft.RPCharacters.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.calendar.FantasyCalendar;
import net.tfminecraft.RPCharacters.identity.NameColour;
import net.tfminecraft.RPCharacters.persona.AliasValidator;
import net.tfminecraft.RPCharacters.persona.DescriptionValidator;
import net.tfminecraft.RPCharacters.persona.NameColourParser;
import net.tfminecraft.RPCharacters.profile.ProfileManager;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;
import net.tfminecraft.RPCharacters.persona.PersonaCooldownManager;

public final class CharCommand {

	private static final Set<String> SUBCOMMANDS = Set.of(
			"alias", "namecolour", "gender", "description", "profile", "override");

	private static final String FIELD_ALIAS = "alias";
	private static final String FIELD_GENDER = "gender";
	private static final String FIELD_DESCRIPTION = "description";

	private CharCommand() {}

	public static boolean isPersonaSubcommand(String subcommand) {
		return subcommand != null && SUBCOMMANDS.contains(subcommand.toLowerCase(Locale.ROOT));
	}

	public static boolean handle(CommandSender sender, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage("§eUsage: /" + label + " <alias|namecolour|gender|description|profile|override>");
			return true;
		}

		String sub = args[0].toLowerCase(Locale.ROOT);
		if (sub.equals("override")) {
			return handleOverride(sender, label, args);
		}
		if (sub.equals("profile")) {
			if (!(sender instanceof Player player)) {
				sender.sendMessage("§cOnly players can use this command.");
				return true;
			}
			return handleProfile(player, args);
		}

		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cOnly players can use this command.");
			return true;
		}

		switch (sub) {
			case "alias":
				return handleAlias(player, label, args);
			case "namecolour":
				return handleNamecolour(player, label, args);
			case "gender":
				return handleGender(player, label, args);
			case "description":
				return handleDescription(player, label, args);
			default:
				player.sendMessage("§cUnknown subcommand. Use alias, namecolour, gender, description, or profile.");
				return true;
		}
	}

	private static boolean handleAlias(Player player, String label, String[] args) {
		if (!player.hasPermission(Cache.personaSetPermission)) {
			player.sendMessage("§cYou do not have permission to change your alias.");
			return true;
		}
		RPCharacter character = requireActiveCharacter(player);
		if (character == null) {
			return true;
		}
		if (args.length < 2) {
			player.sendMessage("§eUsage: /" + label + " alias <name...>|clear");
			return true;
		}
		String aliasInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
		if (aliasInput.equalsIgnoreCase("clear")) {
			character.clearAlias();
			RPCharacters.getPlayerManager().savePlayer(player);
			player.sendMessage("§aAlias cleared.");
			return true;
		}
		if (isOnCooldown(player, FIELD_ALIAS, Cache.personaAliasCooldownSeconds)) {
			return true;
		}
		String error = AliasValidator.validate(aliasInput);
		if (error != null) {
			player.sendMessage(error);
			return true;
		}
		character.setAlias(aliasInput);
		applyCooldown(player, FIELD_ALIAS, Cache.personaAliasCooldownSeconds);
		RPCharacters.getPlayerManager().savePlayer(player);
		player.sendMessage("§aAlias set to §e" + character.getAlias() + "§a.");
		return true;
	}

	private static boolean handleNamecolour(Player player, String label, String[] args) {
		if (!PermissionGroupService.canUseNameColour(player)) {
			player.sendMessage("§cYou do not have permission to change your name colour.");
			return true;
		}
		RPCharacter character = requireActiveCharacter(player);
		if (character == null) {
			return true;
		}
		if (args.length < 2) {
			player.sendMessage("§eUsage: /" + label + " namecolour <#hex> [<#hex>...]|clear");
			return true;
		}
		if (args[1].equalsIgnoreCase("clear")) {
			character.setNameColour(null);
			character.setNameColourStaffOverride(false);
			RPCharacters.getPlayerManager().savePlayer(player);
			player.sendMessage("§aName colour cleared.");
			return true;
		}
		List<String> hexArgs = Arrays.asList(Arrays.copyOfRange(args, 1, args.length));
		Optional<String> validationError = PermissionGroupService.validateNameColourHexes(player, hexArgs, false);
		if (validationError.isPresent()) {
			player.sendMessage(validationError.get());
			return true;
		}
		List<String> parsed = parseHexColours(player, hexArgs);
		if (parsed == null) {
			return true;
		}
		character.setNameColour(NameColour.of(parsed));
		character.setNameColourStaffOverride(false);
		RPCharacters.getPlayerManager().savePlayer(player);
		player.sendMessage("§aName colour updated.");
		return true;
	}

	private static boolean handleGender(Player player, String label, String[] args) {
		if (!player.hasPermission(Cache.personaSetPermission)) {
			player.sendMessage("§cYou do not have permission to change your gender.");
			return true;
		}
		RPCharacter character = requireActiveCharacter(player);
		if (character == null) {
			return true;
		}
		if (args.length < 2) {
			player.sendMessage("§eUsage: /" + label + " gender <" + String.join("|", Cache.personaGenders) + ">");
			return true;
		}
		if (isOnCooldown(player, FIELD_GENDER, Cache.personaGenderCooldownSeconds)) {
			return true;
		}
		String value = resolveGenderValue(args[1]);
		if (value == null) {
			player.sendMessage("§cInvalid gender. Allowed: §e" + String.join(", ", Cache.personaGenders));
			return true;
		}
		character.setGender(value);
		applyCooldown(player, FIELD_GENDER, Cache.personaGenderCooldownSeconds);
		RPCharacters.getPlayerManager().savePlayer(player);
		player.sendMessage("§aGender set to §e" + value + "§a.");
		return true;
	}

	private static boolean handleDescription(Player player, String label, String[] args) {
		if (!player.hasPermission(Cache.personaSetPermission)) {
			player.sendMessage("§cYou do not have permission to change your description.");
			return true;
		}
		RPCharacter character = requireActiveCharacter(player);
		if (character == null) {
			return true;
		}
		if (args.length < 2) {
			player.sendMessage("§eUsage: /" + label + " description <text...>|clear");
			return true;
		}
		if (args[1].equalsIgnoreCase("clear")) {
			character.setPersonaDescription(null);
			RPCharacters.getPlayerManager().savePlayer(player);
			player.sendMessage("§aDescription cleared.");
			return true;
		}
		if (isOnCooldown(player, FIELD_DESCRIPTION, Cache.personaDescriptionCooldownSeconds)) {
			return true;
		}
		String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		String text = player.hasPermission(Cache.personaDescriptionColorsPermission)
				? raw.trim()
				: ClueFormatter.stripColor(raw);
		String error = DescriptionValidator.validate(text);
		if (error != null) {
			player.sendMessage(error);
			return true;
		}
		character.setPersonaDescription(text);
		applyCooldown(player, FIELD_DESCRIPTION, Cache.personaDescriptionCooldownSeconds);
		RPCharacters.getPlayerManager().savePlayer(player);
		player.sendMessage("§aDescription updated.");
		return true;
	}

	private static boolean handleProfile(Player viewer, String[] args) {
		if (!viewer.hasPermission(Cache.profilePermission)) {
			viewer.sendMessage("§cYou do not have permission to view character profiles.");
			return true;
		}
		Player target = viewer;
		if (args.length >= 2) {
			Player named = Bukkit.getPlayerExact(args[1]);
			if (named == null) {
				viewer.sendMessage("§cPlayer not found.");
				return true;
			}
			target = named;
		}
		ProfileManager.showProfile(viewer, target, true);
		return true;
	}

	private static boolean handleOverride(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission(Cache.personaOverridePermission)) {
			sender.sendMessage("§cYou do not have permission to override persona fields.");
			return true;
		}
		if (args.length < 4) {
			sender.sendMessage("§eUsage: /" + label + " override <player> <alias|gender|description|namecolour|birthday> <value...>");
			return true;
		}
		Player target = Bukkit.getPlayerExact(args[1]);
		if (target == null) {
			sender.sendMessage("§cPlayer not found.");
			return true;
		}
		PlayerData data = PlayerManager.get(target);
		if (data == null || !data.hasActiveCharacter()) {
			sender.sendMessage("§cThat player has no active character.");
			return true;
		}
		RPCharacter character = data.getActiveCharacter();
		String field = args[2].toLowerCase(Locale.ROOT);
		switch (field) {
			case "alias":
				return overrideAlias(sender, target, character, args);
			case "gender":
				return overrideGender(sender, target, character, args);
			case "description":
				return overrideDescription(sender, target, character, args);
			case "namecolour":
				return overrideNamecolour(sender, target, character, args);
			case "birthday":
				return overrideBirthday(sender, target, character, args);
			default:
				sender.sendMessage("§cUnknown field. Use alias, gender, description, namecolour, or birthday.");
				return true;
		}
	}

	private static boolean overrideAlias(CommandSender sender, Player target, RPCharacter character, String[] args) {
		String aliasInput = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).trim();
		if (aliasInput.equalsIgnoreCase("clear")) {
			character.clearAlias();
			RPCharacters.getPlayerManager().savePlayer(target);
			sender.sendMessage("§aCleared alias for §e" + target.getName() + "§a.");
			return true;
		}
		String error = AliasValidator.validate(aliasInput);
		if (error != null) {
			sender.sendMessage(error);
			return true;
		}
		character.setAlias(aliasInput);
		RPCharacters.getPlayerManager().savePlayer(target);
		sender.sendMessage("§aSet alias for §e" + target.getName() + "§a to §e" + character.getAlias() + "§a.");
		return true;
	}

	private static boolean overrideGender(CommandSender sender, Player target, RPCharacter character, String[] args) {
		String value = resolveGenderValue(args[3]);
		if (value == null) {
			sender.sendMessage("§cInvalid gender. Allowed: §e" + String.join(", ", Cache.personaGenders));
			return true;
		}
		character.setGender(value);
		RPCharacters.getPlayerManager().savePlayer(target);
		sender.sendMessage("§aSet gender for §e" + target.getName() + "§a to §e" + value + "§a.");
		return true;
	}

	private static boolean overrideDescription(CommandSender sender, Player target, RPCharacter character, String[] args) {
		if (args[3].equalsIgnoreCase("clear")) {
			character.setPersonaDescription(null);
			RPCharacters.getPlayerManager().savePlayer(target);
			sender.sendMessage("§aCleared description for §e" + target.getName() + "§a.");
			return true;
		}
		String raw = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
		String text = sender.hasPermission(Cache.personaDescriptionColorsPermission)
				? raw.trim()
				: ClueFormatter.stripColor(raw);
		String error = DescriptionValidator.validate(text);
		if (error != null) {
			sender.sendMessage(error);
			return true;
		}
		character.setPersonaDescription(text);
		RPCharacters.getPlayerManager().savePlayer(target);
		sender.sendMessage("§aUpdated description for §e" + target.getName() + "§a.");
		return true;
	}

	private static boolean overrideNamecolour(CommandSender sender, Player target, RPCharacter character, String[] args) {
		if (args[3].equalsIgnoreCase("clear")) {
			character.setNameColour(null);
			character.setNameColourStaffOverride(false);
			RPCharacters.getPlayerManager().savePlayer(target);
			sender.sendMessage("§aCleared name colour for §e" + target.getName() + "§a.");
			return true;
		}
		List<String> hexArgs = Arrays.asList(Arrays.copyOfRange(args, 3, args.length));
		Optional<String> validationError = PermissionGroupService.validateNameColourHexes(target, hexArgs, true);
		if (validationError.isPresent()) {
			sender.sendMessage(validationError.get());
			return true;
		}
		List<String> parsed = parseHexColours(sender, hexArgs);
		if (parsed == null) {
			return true;
		}
		character.setNameColour(NameColour.of(parsed));
		character.setNameColourStaffOverride(true);
		RPCharacters.getPlayerManager().savePlayer(target);
		sender.sendMessage("§aUpdated name colour for §e" + target.getName() + "§a.");
		return true;
	}

	private static boolean overrideBirthday(CommandSender sender, Player target, RPCharacter character, String[] args) {
		if (args[3].equalsIgnoreCase("clear")) {
			character.setBirthday(null);
			RPCharacters.getPlayerManager().savePlayer(target);
			sender.sendMessage("§aCleared birthday for §e" + target.getName() + "§a.");
			return true;
		}
		if (FantasyCalendar.fromIso(args[3]) == null) {
			sender.sendMessage("§cInvalid birthday. Use ISO format §eYYYY-MM-DD§c.");
			return true;
		}
		character.setBirthday(args[3]);
		RPCharacters.getPlayerManager().savePlayer(target);
		sender.sendMessage("§aSet birthday for §e" + target.getName() + "§a to §e" + character.getBirthday() + "§a.");
		return true;
	}

	private static RPCharacter requireActiveCharacter(Player player) {
		PlayerData data = PlayerManager.get(player);
		if (data == null || !data.hasActiveCharacter()) {
			player.sendMessage("§cYou must have an active character.");
			return null;
		}
		return data.getActiveCharacter();
	}

	private static String resolveGenderValue(String input) {
		for (String allowed : Cache.personaGenders) {
			if (allowed.equalsIgnoreCase(input)) {
				return allowed;
			}
		}
		return null;
	}

	private static boolean isOnCooldown(Player player, String field, int cooldownSeconds) {
		if (player.hasPermission(Cache.personaBypassCooldownPermission)) {
			return false;
		}
		PersonaCooldownManager cooldowns = PersonaCooldownManager.get();
		if (!cooldowns.isOnCooldown(player, field, cooldownSeconds)) {
			return false;
		}
		int remaining = cooldowns.getRemainingSeconds(player, field);
		player.sendMessage("§cPlease wait §e" + remaining + "§c more second(s) before changing your " + field + ".");
		return true;
	}

	private static void applyCooldown(Player player, String field, int cooldownSeconds) {
		if (player.hasPermission(Cache.personaBypassCooldownPermission)) {
			return;
		}
		PersonaCooldownManager.get().applyCooldown(player, field, cooldownSeconds);
	}

	private static List<String> parseHexColours(CommandSender sender, List<String> hexArgs) {
		List<String> parsed = new ArrayList<>();
		for (String arg : hexArgs) {
			Optional<String> hex = NameColourParser.parse(arg);
			if (hex.isEmpty()) {
				sender.sendMessage("§cInvalid hex colour: §e" + arg);
				return null;
			}
			parsed.add(hex.get());
		}
		return parsed;
	}
}
