package net.tfminecraft.RPCharacters.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.chat.ChatChannel;
import net.tfminecraft.RPCharacters.chat.ChatRecipientFilters;
import net.tfminecraft.RPCharacters.chat.ChatRecipientResolver;

public final class PartyChatRecipientResolver implements ChatRecipientResolver {

	@Override
	public Set<Player> resolve(Player sender, ChatChannel channel) {
		if (sender == null || channel == null) {
			return Collections.emptySet();
		}

		Party party = PartyManager.get().getParty(sender.getUniqueId());
		if (party == null) {
			return Collections.emptySet();
		}

		List<Player> candidates = new ArrayList<>();
		for (UUID memberId : party.getMemberIds()) {
			Player online = Bukkit.getPlayer(memberId);
			if (online != null && online.isOnline()) {
				candidates.add(online);
			}
		}
		return ChatRecipientFilters.filterCandidates(sender, channel, candidates);
	}
}
