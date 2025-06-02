package net.tfminecraft.RPCharacters.Utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.tfminecraft.RPCharacters.Cache;

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
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}

