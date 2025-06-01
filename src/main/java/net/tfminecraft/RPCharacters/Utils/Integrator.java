package net.tfminecraft.RPCharacters.Utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.api.player.attribute.PlayerAttributes.AttributeInstance;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeModifier;

public class Integrator {
	public void integrate(Player p, RPCharacter c) {
		PlayerData pd = PlayerData.get(p);
		for(AttributeModifier m : c.getAttributeData().getModifiers()) {
			AttributeInstance attribute = pd.getAttributes().getInstance(m.getType());
			if(attribute == null) continue;
			attribute.setBase(attribute.getBase()+m.getAmount());
		}
	}
	public void remove(Player p, RPCharacter c, boolean reset) {
		PlayerData pd = PlayerData.get(p);
		int count = 0;
		for(AttributeModifier m : c.getAttributeData().getModifiers()) {
			AttributeInstance attribute = pd.getAttributes().getInstance(m.getType());
			if(attribute == null) continue;
			attribute.setBase(attribute.getBase()-m.getAmount());
			if(reset) {
				count = count+attribute.getBase();
				attribute.setBase(0);
			}
		}
		pd.setAttributePoints(pd.getAttributePoints()+count);
	}
	public void remove(Player p, String s) {
		String type = s.split("\\.")[0];
		int amount = Integer.parseInt(s.split("\\.")[1]);
		PlayerData pd = PlayerData.get(p);
		AttributeInstance attribute = pd.getAttributes().getInstance(type);
		if(attribute == null) return;
		attribute.setBase(attribute.getBase()-amount);
		//p.sendMessage(type + " has "+attribute.getBase()+ " points");
	}
	public List<String> getRemoveList(Player p, RPCharacter c) {
		List<String> remove = new ArrayList<>();
		for(AttributeModifier m : c.getAttributeData().getModifiers()) {
			remove.add(m.getType()+"."+m.getAmount());
		}
		return remove;
	}
	public void dispatchCommand(Player player, String cmd) {
		if(player.isOp() == true) {
			player.performCommand(cmd);
		} else {
			try
			{
			    player.setOp(true);
			    player.performCommand(cmd);

			}
			catch(Exception e)
			{
			    e.printStackTrace();
			}
			finally
			{
			    player.setOp(false);
			}
		}	
	}
}
