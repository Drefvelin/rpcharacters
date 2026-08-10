package net.tfminecraft.RPCharacters.Creation;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.RPCharacters.Creation.Stages.AttributesStage;
import net.tfminecraft.RPCharacters.Creation.Stages.ClueStage;
import net.tfminecraft.RPCharacters.Creation.Stages.InfoStage;
import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SummaryStage;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Utils.DurationParser;
import net.tfminecraft.RPCharacters.enums.StageType;

public class Stage {
	private String id;
	
	private boolean repeat;
	
	private boolean autoNext;
	
	private boolean cancelled;
	
	private Dependency dependency;

	private long lockTimeMs = -1L;
	
	public long getLockTimeMs() {
		return lockTimeMs;
	}

	public void setLockTimeMs(long lockTimeMs) {
		this.lockTimeMs = lockTimeMs;
	}

	protected void copyBaseFields(Stage source) {
		setId(source.getId());
		setRepeat(source.shouldRepeat());
		setAutoNext(source.autoNext());
		setCancelled(source.isCancelled());
		if (source.hasDependency()) {
			setDependency(source.getDependency());
		} else {
			setDependency(null);
		}
		setLockTimeMs(source.getLockTimeMs());
	}

	public boolean autoNext() {
		return autoNext;
	}
	
	public boolean shouldRepeat() {
		return repeat;
	}
	
	public void setRepeat(boolean b) {
		this.repeat = b;
	}
	
	public void setAutoNext(boolean b) {
		this.autoNext = b;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public boolean hasDependency() {
		if(dependency != null) return true;
		return false;
	}
	
	public Dependency getDependency() {
		return dependency;
	}

	public void setDependency(Dependency dependency) {
		this.dependency = dependency;
	}

	public static Stage create(String id, ConfigurationSection config) {
		Stage s = new Stage();
		s.setId(id);
		if(config.contains("repeat")) {
			s.setRepeat(config.getBoolean("repeat"));
		} else {
			s.setRepeat(true);
		}
		if(config.contains("auto-next")) {
			s.setAutoNext(config.getBoolean("auto-next"));
		} else {
			s.setAutoNext(true);
		}
		if(config.contains("dependency")) {
			s.setDependency(new Dependency(config.getConfigurationSection("dependency")));
		}
		s.setLockTimeMs(DurationParser.parseLockTimeMs(config.getString("lock-time", "-1")));
		StageType type = StageType.valueOf(config.getString("type").toUpperCase());
		s.setCancelled(false);
		if(type.equals(StageType.INFO)) {
			return new InfoStage(s, config);
		} else if(type.equals(StageType.QUESTIONS)) {
			return new QuestionStage(s, config);
		} else if(type.equals(StageType.SETTER)) {
			return new SetterStage(s, config);
		} else if(type.equals(StageType.SELECTION)) {
			return new SelectionStage(s, config);
		} else if(type.equals(StageType.ATTRIBUTES)) {
			return new AttributesStage(s, config);
		} else if(type.equals(StageType.CLUE)) {
			return new ClueStage(s, config);
		} else if(type.equals(StageType.SUMMARY)) {
			return new SummaryStage(s, config);
		}
		return s;
	}
	
	public static Stage another(Stage another) {
		if(another instanceof InfoStage) {
			InfoStage s = (InfoStage) another;
			return new InfoStage(s);
		} else if(another instanceof QuestionStage) {
			QuestionStage s = (QuestionStage) another;
			return new QuestionStage(s);
		} else if(another instanceof SetterStage) {
			SetterStage s = (SetterStage) another;
			return new SetterStage(s);
		} else if(another instanceof SelectionStage) {
			SelectionStage s = (SelectionStage) another;
			return new SelectionStage(s);
		} else if(another instanceof AttributesStage) {
			AttributesStage s = (AttributesStage) another;
			return new AttributesStage(s);
		} else if(another instanceof ClueStage) {
			ClueStage s = (ClueStage) another;
			return new ClueStage(s);
		} else if(another instanceof SummaryStage) {
			SummaryStage s = (SummaryStage) another;
			return new SummaryStage(s);
		}
		return null;
	}

	public void cancel() {
		cancelled = true;
	}
	public void update(PlayerData pd) {

	}
	public void setCancelled(boolean b) {
		this.cancelled = b;
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
}
