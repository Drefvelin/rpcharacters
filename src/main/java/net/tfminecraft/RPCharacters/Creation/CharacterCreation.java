package net.tfminecraft.RPCharacters.Creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import net.Indyuce.mmocore.api.MMOCoreAPI;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.Creation.Stages.InfoStage;
import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;

public class CharacterCreation {
	private RPCharacter character;
	private PlayerClass oldclass;
	private Player p;
	
	private boolean canNext;
	
	private List<Stage> stages = new ArrayList<>();
	
	private int currentStage;
	
	private AttributeData tempData;

	private boolean cancelled = false;
	
	public void setCanNext(boolean b) {
		this.canNext = b;
	}
	
	public boolean canNext() {
		return canNext;
	}
	
	public Stage getCurrentStage() {
		int i = currentStage-1;
		if(i < 0) i = 0;
		return stages.get(i);
	}
	
	public CharacterCreation(Player p) {
		this.p = p;
		oldclass = net.Indyuce.mmocore.api.player.PlayerData.get(p).getProfess();
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String command = "mmocore admin class-points set " + p.getName() + " " + 1;
		Bukkit.dispatchCommand(console, command);
		stages = StageLoader.getNew();
		character = new RPCharacter(p);
		currentStage = 0;
		tempData = new AttributeData();
		runStage();
	}
	
	public AttributeData getTempData() {
		return tempData;
	}
	
	public RPCharacter getCharacter() {
		return character;
	}
	
	public void runStage() {
		canNext = false;
		if(cancelled) return;
		if(currentStage >= stages.size()) {
			CreationManager.activeCreators.remove(p);
			finish();
			return;
		}
		Stage s = stages.get(currentStage);
		if(!s.shouldRepeat()) {
			PlayerData pd = PlayerManager.get(p);
			if(pd.hasCompletedStage(s)) {
				currentStage++;
				runStage();
				return;
			} else {
				pd.addCompletedStage(s);
			}
		}
		if(s.hasDependency()) {
			if(!s.getDependency().check(character)) {
				currentStage++;
				runStage();
				return;
			}
		}
		if(s instanceof InfoStage) {
			InfoStage info = (InfoStage) s;
			info.execute(p, this);
		} else if(s instanceof QuestionStage) {
			QuestionStage q = (QuestionStage) s;
			q.execute(p, this);
		} else if(s instanceof SetterStage) {
			SetterStage ss = (SetterStage) s;
			ss.execute(p, this);
		} else if(s instanceof SelectionStage) {
			SelectionStage ss = (SelectionStage) s;
			ss.execute(p, this);
		}
		currentStage++;
	}
	
	public void answerQuestion(String a) {
		Stage s = getCurrentStage();
		if(s instanceof QuestionStage) {
			QuestionStage q = (QuestionStage) s;
			q.checkAnswer(a, p, this);
		}
	}
	public void finish() {
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String command = "mmocore admin class-points set " + p.getName() + " " + 0;
		Bukkit.dispatchCommand(console, command);
		PlayerData pd = PlayerManager.get(p);
		character.update();
		pd.addCharacter(character);
		pd.setActiveCharacter(character);
		p.sendTitle("§aFinished!", "§eCharacter §7"+character.getName()+"§e created!", 5, 50, 5);
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		String command = "mmocore admin class-points set " + p.getName() + " " + 0;
		Bukkit.dispatchCommand(console, command);
		CreationManager.activeCreators.remove(p);
		Stage s = stages.get(currentStage);
		cancelled = true;
		if(oldclass != null) {
			net.Indyuce.mmocore.api.player.PlayerData.get(p).setClass(oldclass);
			p.sendMessage("§cYour class was set back to "+oldclass.getName());
		}
		p.sendTitle("§cCancelled!", "§eCharacter creation cancelled", 5, 50, 5);
		s.cancel();
	}
}
