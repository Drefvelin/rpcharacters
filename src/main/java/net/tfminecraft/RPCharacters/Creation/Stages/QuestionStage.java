package net.tfminecraft.RPCharacters.Creation.Stages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Objects.Question;

public class QuestionStage extends Stage{
	private List<Question> questions = new ArrayList<Question>();
	
	private int currentQuestion;
	
	public QuestionStage(Stage s, ConfigurationSection config) {
		setId(s.getId());
		setRepeat(s.shouldRepeat());
		setAutoNext(s.autoNext());
		setCancelled(s.isCancelled());
		if(s.hasDependency()) setDependency(s.getDependency());
		Set<String> set = config.getConfigurationSection("questions").getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			questions.add(new Question(config.getConfigurationSection("questions."+key).getString("question"), config.getConfigurationSection("questions."+key).getStringList("answers")));
		}
		currentQuestion = 0;
	}
	public QuestionStage(QuestionStage another) {
		setId(another.getId());
		setRepeat(another.shouldRepeat());
		setAutoNext(another.autoNext());
		setCancelled(another.isCancelled());
		if(another.hasDependency()) setDependency(another.getDependency());
		setQuestions(another.getQuestions());
		setCurrentQuestion(0);
	}
	public List<Question> getQuestions() {
		return questions;
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
		if(currentQuestion >= questions.size()) {
			if(autoNext()) {
				cc.runStage();
			} else {
				cc.setCanNext(true);
			}
		}
		Question q = questions.get(currentQuestion);
		p.sendTitle("§aQuestion "+(currentQuestion+1), q.getQuestion(), 5, 60, 5);
		p.sendMessage("§7=======================================");
		p.sendMessage(" ");
		p.sendMessage("§aQuestion "+(currentQuestion+1));
		p.sendMessage(q.getQuestion());
		p.sendMessage(" ");
		p.sendMessage("§7=======================================");
	}
	
	public void checkAnswer(String m, Player p, CharacterCreation cc) {
		if(currentQuestion >= questions.size()) {
			return;
		}
		Question q = questions.get(currentQuestion);
		if(q.isCorrect(m)) {
			p.sendTitle("§aCorrect!", " ", 2, 16, 2);
			p.sendMessage("§aCorrect!");
			currentQuestion++;
			new BukkitRunnable()
			{
				public void run()
				{
					execute(p, cc);
				}
			}.runTaskLater(RPCharacters.plugin, 20L);
		} else {
			p.sendMessage("§cIncorrect answer, please try again");
		}
	}
}
