package net.tfminecraft.RPCharacters.Creation.Stages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Objects.Question;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public class QuestionStage extends Stage{
	private List<Question> base = new ArrayList<>();
	private List<Question> questions = new ArrayList<>();
	
	private int amount;
	private int currentQuestion;
	
	public QuestionStage(Stage s, ConfigurationSection config) {
		copyBaseFields(s);
		Set<String> set = config.getConfigurationSection("questions").getKeys(false);

		List<String> list = new ArrayList<String>(set);
		amount = config.getInt("amount", 1);
		for(String key : list) {
			base.add(new Question(config.getConfigurationSection("questions."+key).getString("question"), config.getConfigurationSection("questions."+key).getStringList("answers")));
		}
		currentQuestion = 0;
	}
	public QuestionStage(QuestionStage another) {
		copyBaseFields(another);
		setStored(another.getQuestions());
		setCurrentQuestion(0);
	}
	public int getAmount() {
		return amount;
	}

	public void pick() {
		// Defensive check: if amount > base size, limit it
		int pickAmount = Math.min(amount, base.size());

		// Create a copy of base to shuffle
		List<Question> shuffled = new ArrayList<>(base);
		
		// Shuffle the copy randomly
		Collections.shuffle(shuffled, new Random());

		// Pick the first N elements after shuffle
		questions = new ArrayList<>(shuffled.subList(0, pickAmount));
	}

	public List<Question> getQuestions() {
		return questions;
	}
	public void setStored(List<Question> questions) {
		base = questions;
	} 
	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}
	
	public int getCurrentQuestion() {
		return currentQuestion;
	}
	public void setCurrentQuestion(int currentQuestion) {
		this.currentQuestion = currentQuestion;
	}
	public void execute(Player p, CharacterCreation cc) {
		if(cc.isCancelled()) return;
		if(currentQuestion >= questions.size()) {
			if(autoNext()) {
				cc.runStage();
			} else {
				cc.setCanNext(true);
			}
		}
		Question q = questions.get(currentQuestion);
		RPTexts.title(p, RPTexts.SUCCESS + "Question " + (currentQuestion + 1),
				RPTexts.formatGui(q.getQuestion()), 5, 60, 5);
	}
	
	public void checkAnswer(String m, Player p, CharacterCreation cc) {
		if(currentQuestion >= questions.size()) {
			return;
		}
		Question q = questions.get(currentQuestion);
		if(q.isCorrect(m)) {
			RPTexts.title(p, RPTexts.SUCCESS + "Correct!", " ", 2, 16, 2);
			currentQuestion++;
			new BukkitRunnable()
			{
				public void run()
				{
					execute(p, cc);
				}
			}.runTaskLater(RPCharacters.plugin, 20L);
		} else {
			RPTexts.send(p, RPTexts.ERROR + "Wrong answer.");
		}
	}
}
