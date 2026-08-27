package net.tfminecraft.RPCharacters.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Permissions;
import net.tfminecraft.RPCharacters.command.CharCommand;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;
import net.tfminecraft.RPCharacters.wardrobe.WardrobeCommand;

public class CommandTabCompleter implements TabCompleter {

	private static final List<String> PERSONA_SUBCOMMANDS = List.of(
			"alias", "namecolour", "gender", "description", "profile", "override", "birthday");
	private static final List<String> CLEAR = List.of("clear");
	private static final List<String> OVERRIDE_FIELDS = List.of(
			"alias", "gender", "description", "namecolour", "birthday", "playtime");

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("rpcharacter")) {
			return Collections.emptyList();
		}

		if (args.length == 1) {
			List<String> completions = new ArrayList<>();
			completions.add("create");
			completions.add("kit");
			completions.add("next");
			completions.add("back");
			completions.add("menu");
			completions.add("cancel");
			completions.add("edit");
			completions.add("clues");
			completions.add("wardrobe");
			completions.addAll(PERSONA_SUBCOMMANDS);
			if (Permissions.isAdmin(sender)) {
				completions.add("reload");
				completions.add("catalog");
				completions.add("pending");
				completions.add("reclaimkit");
				completions.add("resetkit");
				completions.add("stage");
				completions.add("setclass");
				completions.add("seteighteen");
				completions.add("skipcooldown");
				completions.add("addtrait");
				completions.add("removetrait");
				completions.add("clearclues");
				completions.add("placeclue");
				completions.add("adminmode");
				completions.add("discordgate");
				completions.add("setworldspawn");
				completions.add("injure");
				completions.add("permakill");
			}
			completions.add("dismisspdwarning");
			if (sender.hasPermission(Cache.personaTempaliasPermission)) {
				completions.add("tempalias");
			}
			if (sender.hasPermission(Cache.personaCharacterHiddenPermission) && sender instanceof Player) {
				completions.add("sethidden");
			}
			return filter(completions, args[0]);
		}

		String sub = args[0].toLowerCase(Locale.ROOT);
		if (CharCommand.isPersonaSubcommand(sub)) {
			return completePersona(sender, sub, args);
		}

		List<String> completions = new ArrayList<>();

		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("kit")) {
				return filter(new ArrayList<>(
						net.tfminecraft.RPCharacters.Loaders.KitLoader.kitIds()
				), args[1]);
			}
			if (args[0].equalsIgnoreCase("catalog") && Permissions.isAdmin(sender)) {
				return filter(List.of("sync"), args[1]);
			}
			if (args[0].equalsIgnoreCase("pending") && Permissions.isAdmin(sender)) {
				return filter(List.of("sync"), args[1]);
			}
			if (args[0].equalsIgnoreCase("stage") && Permissions.isAdmin(sender)) {
				return filter(List.of("preview"), args[1]);
			}
			if (args[0].equalsIgnoreCase("menu")) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("wardrobe") && sender instanceof Player) {
				return WardrobeCommand.tabComplete((Player) sender, args);
			} else if (args[0].equalsIgnoreCase("clues") && Permissions.isAdmin(sender)) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("edit")) {
				completions.addAll(net.tfminecraft.RPCharacters.Creation.SummaryEditSupport.getEditEntryKeys());
			} else if (args[0].equalsIgnoreCase("setclass")) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("seteighteen") && Permissions.isAdmin(sender)) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if ((args[0].equalsIgnoreCase("resetkit")
					|| args[0].equalsIgnoreCase("reclaimkit"))
					&& Permissions.isAdmin(sender)) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("skipcooldown")) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("addtrait") || args[0].equalsIgnoreCase("removetrait")
					|| args[0].equalsIgnoreCase("injure") || args[0].equalsIgnoreCase("permakill")) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("clearclues") && Permissions.isAdmin(sender)) {
				completions.add("5");
				completions.add("10");
				completions.add("25");
				completions.add("50");
			} else if (args[0].equalsIgnoreCase("placeclue") && Permissions.isAdmin(sender)) {
				// text is free-form; no tab completions
			} else if (args[0].equalsIgnoreCase("adminmode") && Permissions.isAdmin(sender)) {
				completions.add("on");
				completions.add("off");
			} else if (args[0].equalsIgnoreCase("discordgate") && Permissions.isAdmin(sender)) {
				for (Player online : Bukkit.getOnlinePlayers()) {
					completions.add(online.getName());
				}
			} else if (args[0].equalsIgnoreCase("tempalias") && sender.hasPermission(Cache.personaTempaliasPermission)) {
				completions.addAll(CLEAR);
			} else if (args[0].equalsIgnoreCase("sethidden") && sender.hasPermission(Cache.personaCharacterHiddenPermission)
					&& sender instanceof Player player) {
				for (RPCharacter character : PlayerManager.get(player).getCharacters()) {
					if (character.getSlug() != null) {
						completions.add(character.getSlug());
					}
				}
			}

			return filter(completions, args[1]);
		}

		if (args.length == 3) {
			if (args[0].equalsIgnoreCase("stage") && args[1].equalsIgnoreCase("preview")
					&& Permissions.isAdmin(sender)) {
				for (net.tfminecraft.RPCharacters.Creation.Stage stage
						: net.tfminecraft.RPCharacters.Loaders.StageLoader.oList) {
					if (stage != null && stage.getId() != null) {
						completions.add(stage.getId());
					}
				}
			} else if (args[0].equalsIgnoreCase("discordgate") && Permissions.isAdmin(sender)) {
				completions.add("on");
				completions.add("off");
			} else if (args[0].equalsIgnoreCase("sethidden") && sender.hasPermission(Cache.personaCharacterHiddenPermission)) {
				completions.addAll(CLEAR);
			} else if (args[0].equalsIgnoreCase("setclass")) {
				completions.add("className");
			} else if (args[0].equalsIgnoreCase("seteighteen") && Permissions.isAdmin(sender)) {
				completions.add("true");
				completions.add("false");
			} else if ((args[0].equalsIgnoreCase("resetkit")
					|| args[0].equalsIgnoreCase("reclaimkit"))
					&& Permissions.isAdmin(sender)) {
				Player target = Bukkit.getPlayerExact(args[1]);
				if (target != null) {
					PlayerData pd = PlayerManager.get(target);
					if (pd != null) {
						for (RPCharacter character : pd.getCharacters()) {
							if (character != null
									&& character.getSlug() != null
									&& !character.getSlug().isBlank()) {
								completions.add(character.getSlug());
							}
						}
					}
				}
			} else if (args[0].equalsIgnoreCase("addtrait") || args[0].equalsIgnoreCase("removetrait")) {
				for (Trait trait : TraitLoader.get()) {
					completions.add(trait.getId());
				}
			} else if ((args[0].equalsIgnoreCase("injure") || args[0].equalsIgnoreCase("permakill"))
					&& Permissions.isAdmin(sender)) {
				Player target = Bukkit.getPlayerExact(args[1]);
				if (target != null) {
					PlayerData pd = PlayerManager.get(target);
					if (pd != null) {
						for (RPCharacter character : pd.getCharacters()) {
							if (character.getSlug() != null) {
								completions.add(character.getSlug());
							}
							if (character.getName() != null) {
								completions.add(character.getName());
							}
						}
					}
				}
				if (args[0].equalsIgnoreCase("injure")) {
					completions.add("permanent");
				}
			}

			return filter(completions, args[2]);
		}

		if (args.length == 4
				&& args[0].equalsIgnoreCase("injure")
				&& Permissions.isAdmin(sender)) {
			return filter(List.of("permanent"), args[3]);
		}

		if (args.length == 4
				&& (args[0].equalsIgnoreCase("resetkit")
						|| args[0].equalsIgnoreCase("reclaimkit"))
				&& Permissions.isAdmin(sender)) {
			return filter(new ArrayList<>(
					net.tfminecraft.RPCharacters.Loaders.KitLoader.kitIds()
			), args[3]);
		}

		return Collections.emptyList();
	}

	private List<String> completePersona(CommandSender sender, String sub, String[] args) {
		if (sub.equals("override")) {
			return completeOverride(sender, args);
		}
		if (sub.equals("profile")) {
			if (args.length == 2) {
				return Bukkit.getOnlinePlayers().stream()
						.map(Player::getName)
						.filter(name -> name.toLowerCase(Locale.ROOT)
								.startsWith(args[1].toLowerCase(Locale.ROOT)))
						.collect(Collectors.toList());
			}
			return Collections.emptyList();
		}
		if (!(sender instanceof Player)) {
			return Collections.emptyList();
		}

		switch (sub) {
			case "alias":
			case "description":
				if (args.length == 2) {
					return filter(CLEAR, args[1]);
				}
				return Collections.emptyList();
			case "namecolour":
				if (args.length == 2) {
					List<String> options = new ArrayList<>(CLEAR);
					options.add("#ff5555");
					return filter(options, args[1]);
				}
				if (args.length >= 3 && !args[1].equalsIgnoreCase("clear") && sender instanceof Player player) {
					int maxStops = PermissionGroupService.getNameColourStops(player);
					if (args.length - 1 < maxStops) {
						return filter(List.of("#0000ff"), args[args.length - 1]);
					}
				}
				return Collections.emptyList();
			case "gender":
				if (args.length == 2) {
					return filter(Cache.personaGenders, args[1]);
				}
				return Collections.emptyList();
			case "birthday":
				if (args.length == 2) {
					return filter(CLEAR, args[1]);
				}
				return Collections.emptyList();
			default:
				return Collections.emptyList();
		}
	}

	private List<String> completeOverride(CommandSender sender, String[] args) {
		if (!sender.hasPermission(Cache.personaOverridePermission)) {
			return Collections.emptyList();
		}
		if (args.length == 2) {
			return Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.filter(name -> name.toLowerCase(Locale.ROOT)
							.startsWith(args[1].toLowerCase(Locale.ROOT)))
					.collect(Collectors.toList());
		}
		if (args.length == 3) {
			return filter(OVERRIDE_FIELDS, args[2]);
		}
		if (args.length == 4) {
			String field = args[2].toLowerCase(Locale.ROOT);
			if (field.equals("alias") || field.equals("description") || field.equals("namecolour")
					|| field.equals("birthday") || field.equals("playtime")) {
				return filter(CLEAR, args[3]);
			}
			if (field.equals("gender")) {
				return filter(Cache.personaGenders, args[3]);
			}
		}
		if (args.length == 5 && args[2].equalsIgnoreCase("namecolour") && !args[3].equalsIgnoreCase("clear")) {
			return filter(List.of("#0000ff"), args[4]);
		}
		return Collections.emptyList();
	}

	private List<String> filter(List<String> options, String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return new ArrayList<>(options);
		}
		String lower = prefix.toLowerCase(Locale.ROOT);
		return options.stream()
				.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
				.collect(Collectors.toList());
	}
}
