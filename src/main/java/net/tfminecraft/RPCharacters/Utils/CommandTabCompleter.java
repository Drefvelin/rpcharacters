package net.tfminecraft.RPCharacters.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Permissions;

public class CommandTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!cmd.getName().equalsIgnoreCase("rpcharacter")) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("create");
            completions.add("next");
            completions.add("menu");
            completions.add("cancel");
            completions.add("edit");
            if(Permissions.isAdmin(sender)) {
                completions.add("setclass");
                completions.add("skipcooldown");
                completions.add("addtrait");
                completions.add("removetrait");
            }
            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("menu")) {
                // Suggest online players
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            } else if (args[0].equalsIgnoreCase("edit")) {
                // Suggest editable traits
                completions.addAll(Cache.editableTraits);
            } else if (args[0].equalsIgnoreCase("setclass") && args.length < 3) {
                // Suggest editable traits
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            } else if (args[0].equalsIgnoreCase("skipcooldown") && args.length < 2) {
                // Suggest editable traits
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            } else if (args[0].equalsIgnoreCase("addtrait") && args.length < 3) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            } else if (args[0].equalsIgnoreCase("removetrait") && args.length < 3) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setclass")) {
                completions.add("className");
            } else if (args[0].equalsIgnoreCase("addtrait") || args[0].equalsIgnoreCase("removetrait")) {
                for (Trait trait : TraitLoader.get()) {
                    completions.add(trait.getId());
                }
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}

