package net.tfminecraft.RPCharacters.Creation;



import java.time.Instant;

import java.util.ArrayList;

import java.util.List;



import org.bukkit.entity.Player;



import net.Indyuce.mmocore.api.player.profess.PlayerClass;

import net.tfminecraft.RPCharacters.mmocore.ClassService;

import net.tfminecraft.RPCharacters.Creation.Stages.ClueStage;

import net.tfminecraft.RPCharacters.Creation.Stages.InfoStage;

import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;

import net.tfminecraft.RPCharacters.Creation.Stages.AttributesStage;

import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;

import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;

import net.tfminecraft.RPCharacters.Creation.Stages.SummaryStage;

import net.tfminecraft.RPCharacters.RPCharacters;

import net.tfminecraft.RPCharacters.Loaders.StageLoader;

import net.tfminecraft.RPCharacters.Managers.CreationManager;

import net.tfminecraft.RPCharacters.Managers.InventoryManager;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;

import net.tfminecraft.RPCharacters.Objects.PlayerData;

import net.tfminecraft.RPCharacters.Objects.RPCharacter;

import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;

import net.tfminecraft.RPCharacters.enums.CharacterSessionMode;

import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;



public class CharacterCreation {

	private RPCharacter character;

	private PlayerClass oldclass;

	private Player p;

	

	private boolean canNext;

	

	private List<Stage> stages = new ArrayList<>();

	

	private int currentStage;

	

	private AttributeData tempData;



	private boolean cancelled = false;



	private boolean editingFromSummary = false;

	private Stage editStage = null;

	private final String summaryStageId = "creation_summary_stage";

	private CharacterSessionMode sessionMode = CharacterSessionMode.CREATING;

	

	public void setCanNext(boolean b) {

		this.canNext = b;

	}

	

	public boolean canNext() {

		return canNext;

	}



	public CharacterSessionMode getSessionMode() {

		return sessionMode;

	}



	public boolean isEditing() {

		return sessionMode == CharacterSessionMode.EDITING;

	}



	public boolean isPreview() {

		return sessionMode == CharacterSessionMode.PREVIEW;

	}

	

	public Stage getCurrentStage() {

		int i = currentStage - 1;

		if (i < 0) {

			i = 0;

		}

		if (i >= stages.size()) {

			i = stages.size() - 1;

		}

		return stages.get(i);

	}



	public Stage getActiveStage() {

		if (editingFromSummary && editStage != null) {

			return editStage;

		}

		return getCurrentStage();

	}



	public Stage getEditStage() {

		return editStage;

	}



	public boolean isEditingFromSummary() {

		return editingFromSummary;

	}



	public String getSummaryStageId() {

		return summaryStageId;

	}

	

	public CharacterCreation(Player p) {

		this.p = p;

		this.sessionMode = CharacterSessionMode.CREATING;

		oldclass = net.Indyuce.mmocore.api.player.PlayerData.get(p).getProfess();

		stages = StageLoader.getNew();

		character = new RPCharacter(p);

		currentStage = 0;

		tempData = new AttributeData();

		runStage();

	}



	public static CharacterCreation forEdit(Player p, RPCharacter character) {

		CharacterCreation cc = new CharacterCreation();

		cc.p = p;

		cc.character = character;

		cc.sessionMode = CharacterSessionMode.EDITING;

		cc.stages = StageLoader.getNew();

		cc.tempData = new AttributeData();

		return cc;

	}



	public static CharacterCreation forStagePreview(Player p, String stageId) {

		if (p == null || stageId == null || stageId.isBlank()) {

			return null;

		}

		Stage template = StageLoader.getById(stageId.trim());

		if (template == null) {

			RPTexts.send(p, RPTexts.ERROR + "Unknown stage id.");

			return null;

		}

		Stage fresh = Stage.another(template);

		if (fresh == null) {

			RPTexts.send(p, RPTexts.ERROR + "Could not preview that stage.");

			return null;

		}

		CharacterCreation cc = new CharacterCreation();

		cc.p = p;

		cc.sessionMode = CharacterSessionMode.PREVIEW;

		cc.character = new RPCharacter(p);

		cc.stages = new ArrayList<>();

		cc.stages.add(fresh);

		cc.currentStage = 0;

		cc.tempData = new AttributeData();

		cc.editingFromSummary = true;

		cc.editStage = fresh;

		CreationManager.activeCreators.put(p, cc);

		if (fresh instanceof SelectionStage selection) {

			selection.hydrateFromCharacter(cc.character);

			selection.execute(p, cc);

		} else if (fresh instanceof AttributesStage attributes) {

			attributes.hydrateFromCharacter(cc.character);

			attributes.execute(p, cc);

		} else if (fresh instanceof SetterStage setter) {

			setter.execute(p, cc);

		} else if (fresh instanceof InfoStage info) {

			info.execute(p, cc);

			RPTexts.send(p, RPTexts.COMMAND + "/rpcharacter next");

		} else if (fresh instanceof ClueStage clue) {

			clue.execute(p, cc);

		} else if (fresh instanceof QuestionStage question) {

			question.execute(p, cc);

		} else if (fresh instanceof SummaryStage summary) {

			summary.execute(p, cc);

		} else {

			CreationManager.activeCreators.remove(p);

			RPTexts.send(p, RPTexts.ERROR + "That stage type cannot be previewed.");

			return null;

		}

		RPTexts.send(p, RPTexts.MUTED + "Stage preview: " + RPTexts.WARN + stageId.trim()

				+ RPTexts.MUTED + ". Confirm or " + RPTexts.COMMAND + "/rpcharacter cancel"

				+ RPTexts.MUTED + " to exit.");

		return cc;

	}



	private CharacterCreation() {}

	

	public AttributeData getTempData() {

		return tempData;

	}

	

	public RPCharacter getCharacter() {

		return character;

	}



	public void openSummary() {

		if (isPreview()) {

			endPreview();

			return;

		}

		SummaryStage summary = findSummaryStage();

		if (summary == null) {

			if (isEditing()) {

				closeEditSession();

			} else {

				finish();

			}

			return;

		}

		InventoryManager inv = new InventoryManager();

		inv.creationSummaryView(p, this, summary);

	}



	private SummaryStage findSummaryStage() {

		for (Stage stage : stages) {

			if (stage instanceof SummaryStage summary) {

				return summary;

			}

		}

		Stage template = StageLoader.getById(summaryStageId);

		if (template instanceof SummaryStage summary) {

			return summary;

		}

		return SummaryEditSupport.getSummaryStage();

	}



	public void jumpToStageForEdit(String stageId) {

		if (stageId == null || stageId.isBlank()) {

			return;

		}

		Stage template = StageLoader.getById(stageId);

		if (template == null) {

			RPTexts.send(p, RPTexts.ERROR + "Could not open editor for that choice.");

			return;

		}

		if (!StageEditLock.canEdit(p, template, character)) {

			RPTexts.send(p, RPTexts.ERROR + "That choice is locked and can no longer be edited.");

			return;

		}

		Stage fresh = Stage.another(template);

		if (fresh == null) {

			RPTexts.send(p, RPTexts.ERROR + "Could not open editor for that choice.");

			return;

		}

		editingFromSummary = true;

		editStage = fresh;

		if (fresh instanceof SelectionStage selection) {

			selection.hydrateFromCharacter(character);

			selection.execute(p, this);

		} else if (fresh instanceof AttributesStage attributes) {

			attributes.hydrateFromCharacter(character);

			attributes.execute(p, this);

		} else if (fresh instanceof SetterStage setter) {

			setter.execute(p, this);

		} else if (fresh instanceof InfoStage info) {

			info.execute(p, this);

			RPTexts.send(p, RPTexts.COMMAND + "/rpcharacter next");

		} else {

			editingFromSummary = false;

			editStage = null;

			RPTexts.send(p, RPTexts.ERROR + "That choice cannot be edited from the summary.");

		}

	}



	public void returnToSummary() {

		if (isPreview()) {

			endPreview();

			return;

		}

		editingFromSummary = false;

		editStage = null;

		if (isEditing()) {

			persistEdits();

		}

		openSummary();

	}



	public void persistEdits() {

		character.update();

		RPCharacters.getPlayerManager().savePlayer(p);

		RPCharacters.getPlayerManager().reevaluateFreeze(p);

	}

	

	public void runStage() {

		if (isPreview()) {

			endPreview();

			return;

		}

		canNext = false;

		if(cancelled) return;

		if(currentStage >= stages.size()) {

			openSummary();

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

		PlayerData agePd = PlayerManager.get(p);

		if (!s.passesAccountAgeGate(agePd)) {

			currentStage++;

			runStage();

			return;

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

		} else if(s instanceof AttributesStage) {

			AttributesStage attrStage = (AttributesStage) s;

			attrStage.execute(p, this);

		} else if(s instanceof ClueStage) {

			ClueStage cs = (ClueStage) s;

			cs.execute(p, this);

		} else if(s instanceof SummaryStage summary) {

			summary.execute(p, this);

		}

		currentStage++;

	}

	

	public void answerQuestion(String a) {

		Stage s = getActiveStage();

		if(s instanceof QuestionStage) {

			QuestionStage q = (QuestionStage) s;

			q.checkAnswer(a, p, this);

		}

	}

	public void finish() {

		if (isPreview()) {

			endPreview();

			return;

		}

		if (isEditing()) {

			closeEditSession();

			return;

		}

		if (!character.hasEnoughClues()) {

			RPTexts.send(p, RPTexts.ERROR + "Missing clues.");

			return;

		}

		if (character.getName() == null || character.getName().isBlank()) {

			RPTexts.send(p, RPTexts.ERROR + "Name not set.");

			return;

		}

		if (!character.hasMMOClass()) {

			RPTexts.send(p, RPTexts.ERROR + "Class not set.");

			return;

		}

		if (character.getRace() == null) {

			RPTexts.send(p, RPTexts.ERROR + "Race not set.");

			return;

		}

		if (character.getBirthday() == null || character.getBirthday().isBlank()) {

			RPTexts.send(p, RPTexts.ERROR + "Age not set.");

			return;

		}

		String description = character.getPersonaDescription();

		if (description == null || description.isBlank()) {

			RPTexts.send(p, RPTexts.ERROR + "Description not set.");

			return;

		}

		PlayerData pd = PlayerManager.get(p);

		if (!CharacterSlotService.hasFreeSlot(p, pd)) {

			RPTexts.send(p, RPTexts.ERROR + "No free character slot. A web create may have taken it.");

			cancel();

			p.closeInventory();

			net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushRosterForPlayer(p);

			return;

		}

		if (character.getCreatedAtEpochSeconds() <= 0) {

			character.setCreatedAtEpochSeconds((int) Instant.now().getEpochSecond());

		}

		character.update();

		pd.addCharacter(character);

		pd.setActiveCharacter(character);

		net.tfminecraft.RPCharacters.kit.KitService.onCharacterCreated(p, pd, character);

		RPCharacters.getPlayerManager().reevaluateFreeze(p);

		RPCharacters.getPlayerManager().savePlayer(p);

		CreationManager.activeCreators.remove(p);

		net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushRosterForPlayer(p);

		p.closeInventory();

		RPTexts.title(p, RPTexts.SUCCESS + "Finished!", RPTexts.WARN + "Character " + RPTexts.MUTED + character.getName() + RPTexts.WARN + " created!", 5, 50, 5);

		RPTexts.send(p, RPTexts.MUTED + "Edit later with " + RPTexts.COMMAND + "/rpcharacter edit" + RPTexts.MUTED + ".");

	}



	public void closeEditSession() {

		persistEdits();

		CreationManager.activeCreators.remove(p);

		p.closeInventory();

	}



	public boolean isCancelled() {

		return cancelled;

	}



	public void endPreview() {

		if (!isPreview()) {

			return;

		}

		if (CreationManager.activeCreators.get(p) != this) {

			return;

		}

		editingFromSummary = false;

		editStage = null;

		CreationManager.activeCreators.remove(p);

		p.closeInventory();

		RPTexts.send(p, RPTexts.SUCCESS + "Stage preview ended.");

	}



	public void cancel() {

		if (isPreview()) {

			endPreview();

			return;

		}

		if (isEditing()) {

			CreationManager.activeCreators.remove(p);

			p.closeInventory();

			return;

		}

		CreationManager.activeCreators.remove(p);

		Stage s = getCurrentStage();

		cancelled = true;

		if(oldclass != null) {

			ClassService.applyClass(p, oldclass.getId());

			RPTexts.send(p, RPTexts.ERROR + "Your class was set back to " + oldclass.getName());

		}

		RPTexts.title(p, RPTexts.ERROR + "Cancelled!", RPTexts.WARN + "Character creation cancelled", 5, 50, 5);

		s.cancel();

	}

}


