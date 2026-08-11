package net.tfminecraft.RPCharacters.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.Dependency;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Creation.Stages.AttributesStage;
import net.tfminecraft.RPCharacters.Creation.Stages.ClueStage;
import net.tfminecraft.RPCharacters.Creation.Stages.InfoStage;
import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SummaryStage;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeModifier;
import net.tfminecraft.RPCharacters.Objects.Experience.ExperienceModifier;
import net.tfminecraft.RPCharacters.Objects.PermissionGroupDefinition;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.api.ProvinceSystemClient;
import net.tfminecraft.RPCharacters.mmocore.MmoCoreClassGuiHelper;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;

/**
 * Build RPCharacters creation catalog JSON and push to ProvinceSystem (fail-soft).
 */
public final class CreationCatalogSyncService {

	private CreationCatalogSyncService() {}

	public static String buildPayloadJson() {
		StringBuilder sb = new StringBuilder(4096);
		sb.append('{');

		appendStages(sb);
		sb.append(',');
		appendAttributePointBuy(sb);
		sb.append(',');
		appendRaces(sb);
		sb.append(',');
		appendTraits(sb);
		sb.append(',');
		appendClasses(sb);
		sb.append(',');
		appendValidation(sb);
		sb.append(',');
		appendSlotLimits(sb);

		sb.append('}');
		return sb.toString();
	}

	private static void appendStages(StringBuilder sb) {
		sb.append("\"stages\":[");
		boolean first = true;
		int order = 0;
		for (Stage stage : StageLoader.oList) {
			if (stage == null || stage.getId() == null) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('{');
			appendField(sb, "id", stage.getId(), true);
			appendField(sb, "type", stageTypeName(stage), false);
			sb.append(",\"order\":").append(order++);
			sb.append(",\"repeat\":").append(stage.shouldRepeat());
			sb.append(",\"auto_next\":").append(stage.autoNext());
			if (stage.getLockTimeMs() > 0) {
				sb.append(",\"lock_time_ms\":").append(stage.getLockTimeMs());
			}
			if (stage.getRequireAccountAgeHoursMin() != null) {
				sb.append(",\"require_account_age_hours_min\":")
					.append(stage.getRequireAccountAgeHoursMin());
			}
			if (stage.getRequireAccountAgeHoursMax() != null) {
				sb.append(",\"require_account_age_hours_max\":")
					.append(stage.getRequireAccountAgeHoursMax());
			}
			if (stage.hasDependency()) {
				sb.append(',');
				appendDependency(sb, stage.getDependency());
			}
			if (stage instanceof InfoStage info) {
				sb.append(",\"interval\":").append(info.getInterval());
				sb.append(',');
				appendStringList(sb, "messages", substituteHoursList(info.getMessages()));
				if (info.hasWebMessages()) {
					sb.append(',');
					appendStringList(sb, "web_messages",
						substituteHoursList(info.getWebMessages()));
				}
			} else if (stage instanceof SetterStage setter) {
				appendField(sb, "target", setter.getTarget(), false);
			} else if (stage instanceof SelectionStage selection) {
				appendField(sb, "target", selection.getTarget(), false);
				if (selection.getKey() != null) {
					appendField(sb, "key", selection.getKey(), false);
				}
				sb.append(",\"min_select\":").append(selection.getMinSelections());
				sb.append(",\"max_select\":").append(selection.getMaxSelections());
				if (selection.hasPoints()) {
					sb.append(",\"points\":").append(selection.getInitialPoints());
				}
			} else if (stage instanceof AttributesStage attributes) {
				appendField(sb, "key", attributes.getKey(), false);
				sb.append(",\"points\":").append(attributes.getPool());
				sb.append(",\"max_rank\":").append(attributes.getMaxRank());
			} else if (stage instanceof SummaryStage summary) {
				sb.append(",\"entries\":{");
				boolean firstEntry = true;
				for (Map.Entry<String, String> e : summary.getEntries().entrySet()) {
					if (!firstEntry) {
						sb.append(',');
					}
					firstEntry = false;
					sb.append('"').append(escape(e.getKey())).append("\":\"")
						.append(escape(e.getValue())).append('"');
				}
				sb.append('}');
			} else if (stage instanceof ClueStage || stage instanceof QuestionStage) {
				// type already set
			}
			sb.append('}');
		}
		sb.append(']');
	}

	private static String stageTypeName(Stage stage) {
		if (stage instanceof InfoStage) {
			return "info";
		}
		if (stage instanceof SetterStage) {
			return "setter";
		}
		if (stage instanceof SelectionStage) {
			return "selection";
		}
		if (stage instanceof AttributesStage) {
			return "attributes";
		}
		if (stage instanceof ClueStage) {
			return "clue";
		}
		if (stage instanceof QuestionStage) {
			return "questions";
		}
		if (stage instanceof SummaryStage) {
			return "summary";
		}
		return "unknown";
	}

	private static void appendDependency(StringBuilder sb, Dependency dep) {
		sb.append("\"dependency\":{");
		appendField(sb, "type", dep.getType(), true);
		appendField(sb, "mode", dep.getMode(), false);
		sb.append(',');
		appendStringList(sb, "depends_on", dep.getDependencies());
		sb.append('}');
	}

	private static void appendAttributePointBuy(StringBuilder sb) {
		int pool = 12;
		int maxRank = 4;
		List<String> attrs = new ArrayList<>(Cache.attributes);
		for (Stage stage : StageLoader.oList) {
			if (stage instanceof AttributesStage attributes) {
				pool = attributes.getPool();
				maxRank = attributes.getMaxRank();
				if (!attributes.getAttributes().isEmpty()) {
					attrs = new ArrayList<>(attributes.getAttributes());
				}
				break;
			}
		}
		sb.append("\"attribute_point_buy\":{");
		sb.append("\"pool\":").append(pool);
		sb.append(",\"max_rank\":").append(maxRank);
		sb.append(",\"cost_for_rank\":[");
		for (int n = 1; n <= maxRank; n++) {
			if (n > 1) {
				sb.append(',');
			}
			sb.append(AttributesStage.costForRank(n));
		}
		sb.append(']');
		sb.append(',');
		appendStringList(sb, "attributes", attrs);
		sb.append(",\"abbreviations\":{");
		boolean first = true;
		for (String attr : attrs) {
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('"').append(escape(attr.toLowerCase(Locale.ROOT))).append("\":\"")
				.append(escape(AttributesStage.abbrevFor(attr))).append('"');
		}
		sb.append('}');
		sb.append(",\"trait_id_pattern\":\"{abbr}{rank}\"");
		sb.append('}');
	}

	private static void appendRaces(StringBuilder sb) {
		sb.append("\"races\":[");
		boolean first = true;
		for (Race race : RaceLoader.get()) {
			if (race == null || race.getId() == null) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('{');
			appendField(sb, "id", race.getId(), true);
			appendField(sb, "name", strip(race.getName()), false);
			String key = race.getRaceData() != null ? race.getRaceData().getKey() : "race";
			appendField(sb, "key", key, false);
			sb.append(",\"shown\":").append(race.isShown());
			sb.append(",\"age_max\":").append(race.getAgeMax());
			sb.append(',');
			appendStringList(sb, "description", stripList(race.getDesc()));
			if (race.getRaceData() != null) {
				appendAttributeData(sb, race.getRaceData().getAttributeData());
			}
			sb.append('}');
		}
		sb.append(']');
	}

	private static void appendTraits(StringBuilder sb) {
		sb.append("\"traits\":[");
		boolean first = true;
		for (Trait trait : TraitLoader.get()) {
			if (trait == null || trait.getId() == null || trait.getTraitData() == null) {
				continue;
			}
			String key = trait.getTraitData().getKey();
			if (key != null && key.equalsIgnoreCase("injury")) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('{');
			appendField(sb, "id", trait.getId(), true);
			appendField(sb, "name", strip(trait.getName()), false);
			if (key != null) {
				appendField(sb, "key", key, false);
			}
			sb.append(",\"cost\":").append(trait.getTraitData().getCost());
			sb.append(',');
			appendStringList(sb, "mutually_exclusive", trait.getTraitData().getExclusive());
			sb.append(',');
			appendStringList(sb, "description", stripList(trait.getDesc()));
			if (trait.getTraitData().hasDependency()) {
				sb.append(',');
				appendDependency(sb, trait.getTraitData().getDependency());
			}
			appendAttributeData(sb, trait.getTraitData().getAttributeData());
			int playtimeSeconds = trait.getTraitData().getRequiredAccountPlaytimeSeconds();
			if (playtimeSeconds > 0) {
				double hours = playtimeSeconds / 3600.0;
				sb.append(",\"required_account_playtime_hours\":");
				if (Math.abs(hours - Math.rint(hours)) < 1e-9) {
					sb.append((long) Math.rint(hours));
				} else {
					sb.append(hours);
				}
			}
			sb.append('}');
		}
		sb.append(']');
	}

	private static void appendClasses(StringBuilder sb) {
		sb.append("\"classes\":[");
		boolean first = true;
		try {
			List<PlayerClass> classes = new ArrayList<>();
			for (PlayerClass playerClass : MMOCore.plugin.classManager.getAll()) {
				if (MmoCoreClassGuiHelper.isClassDisplayed(playerClass)) {
					classes.add(playerClass);
				}
			}
			classes.sort((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()));
			for (PlayerClass playerClass : classes) {
				if (!first) {
					sb.append(',');
				}
				first = false;
				sb.append('{');
				appendField(sb, "id", playerClass.getId(), true);
				appendField(sb, "name", strip(playerClass.getName()), false);
				sb.append(",\"display_order\":").append(playerClass.getDisplayOrder());
				sb.append(',');
				appendStringList(sb, "description", stripList(playerClass.getDescription()));
				sb.append(',');
				appendStringList(
					sb,
					"attribute_description",
					stripList(playerClass.getAttributeDescription())
				);
				sb.append('}');
			}
		} catch (Throwable t) {
			RPCharacters.plugin.getLogger().warning(
				"[creation-catalog] could not read MMOCore classes: " + t.getMessage()
			);
		}
		sb.append(']');
	}

	/** Serialize race/trait AttributeData (omit zero amounts). */
	private static void appendAttributeData(StringBuilder sb, AttributeData data) {
		if (data == null) {
			return;
		}
		sb.append(",\"attribute_modifiers\":[");
		boolean first = true;
		for (AttributeModifier mod : data.getModifiers()) {
			if (mod == null || mod.getAmount() == 0) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('{');
			appendField(sb, "type", mod.getType(), true);
			sb.append(",\"amount\":").append(mod.getAmount());
			sb.append('}');
		}
		sb.append(']');
		sb.append(",\"experience_modifiers\":[");
		first = true;
		for (ExperienceModifier mod : data.getExperienceModifiers()) {
			if (mod == null || mod.getModifier() == 0) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('{');
			appendField(sb, "profession", mod.getProfession(), true);
			appendField(sb, "alias", strip(mod.getAlias()), false);
			sb.append(",\"amount\":").append(mod.getModifier());
			sb.append('}');
		}
		sb.append(']');
	}

	private static void appendValidation(StringBuilder sb) {
		sb.append("\"validation\":{");
		sb.append("\"name\":{");
		sb.append("\"min_length\":").append(Cache.personaDisplayNameMinLength);
		sb.append(",\"max_length\":").append(Cache.personaDisplayNameMaxLength);
		sb.append('}');
		sb.append(",\"age\":{");
		sb.append("\"minimum\":").append(Cache.calendarAgeMinimum);
		sb.append('}');
		sb.append(",\"calendar\":{");
		sb.append("\"year_offset\":").append(Cache.calendarYearOffset);
		sb.append(',');
		appendField(sb, "era_suffix", Cache.calendarEraSuffix, true);
		sb.append('}');
		sb.append(",\"description\":{");
		sb.append("\"min_length\":").append(Cache.characterDescriptionMinLength);
		sb.append(",\"max_length\":").append(Cache.characterDescriptionMaxLength);
		sb.append('}');
		sb.append(",\"clues\":{");
		sb.append("\"default_required\":").append(Cache.defaultCluesRequired);
		sb.append(",\"evil_required\":").append(Cache.evilCluesRequired);
		sb.append(",\"evil_min_account_age_hours\":").append(Cache.evilMinAccountAgeHours);
		sb.append(",\"min_length\":").append(Cache.clueMinLength);
		sb.append(",\"max_length\":").append(Cache.clueMaxLength);
		sb.append(",\"max_clues\":").append(Cache.maxClues);
		sb.append('}');
		sb.append('}');
	}

	private static void appendSlotLimits(StringBuilder sb) {
		sb.append("\"slot_limits\":{");
		sb.append("\"hard_cap\":").append(CharacterSlotService.getHardSlotCap());
		sb.append(",\"defaults\":{");
		int defaultMax = Cache.permissionGroupDefaults.getOrDefault(
			PermissionGroupDefinition.KEY_MAX_ALIVE_CHARACTERS, 3
		);
		int defaultColourStops = Cache.permissionGroupDefaults.getOrDefault(
			PermissionGroupDefinition.KEY_NAME_COLOUR_STOPS, 0
		);
		sb.append("\"max_alive_characters\":").append(defaultMax);
		sb.append(",\"name_colour_stops\":").append(defaultColourStops);
		sb.append('}');
		sb.append(",\"groups\":[");
		boolean first = true;
		for (PermissionGroupDefinition group : Cache.permissionGroups) {
			if (group == null) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('{');
			appendField(sb, "id", group.getId(), true);
			appendField(sb, "permission", group.getPermission(), false);
			appendField(sb, "display_name", strip(group.getDisplayName()), false);
			sb.append(",\"tier\":").append(group.getTier());
			sb.append(",\"visible\":").append(group.isVisible());
			sb.append(",\"max_alive_characters\":").append(
				group.getPerk(PermissionGroupDefinition.KEY_MAX_ALIVE_CHARACTERS, defaultMax)
			);
			sb.append(",\"name_colour_stops\":").append(
				group.getPerk(PermissionGroupDefinition.KEY_NAME_COLOUR_STOPS, defaultColourStops)
			);
			sb.append('}');
		}
		sb.append("]}");
	}

	public static void pushAsync(JavaPlugin plugin) {
		if (plugin == null) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			ProvinceSystemClient.CatalogPushResult result = pushNow();
			Logger log = plugin.getLogger();
			if (result.ok) {
				log.info("[creation-catalog] synced to ProvinceSystem: stages="
					+ result.stages
					+ " races=" + result.races
					+ " traits=" + result.traits
					+ " classes=" + result.classes);
			} else {
				log.warning("[creation-catalog] sync failed: " + result.error);
			}
		});
	}

	public static ProvinceSystemClient.CatalogPushResult pushNow() {
		return ProvinceSystemClient.pushCreationCatalog(buildPayloadJson());
	}

	public static void pushAsyncFromPlugin() {
		if (RPCharacters.plugin != null) {
			pushAsync(RPCharacters.plugin);
		}
	}

	private static void appendField(StringBuilder sb, String key, String value, boolean first) {
		if (!first) {
			sb.append(',');
		}
		sb.append('"').append(escape(key)).append("\":\"")
			.append(escape(value == null ? "" : value)).append('"');
	}

	private static List<String> substituteHoursList(List<String> values) {
		List<String> out = new ArrayList<>();
		if (values == null) {
			return out;
		}
		for (String v : values) {
			out.add(InfoStage.substitutePlaceholders(v));
		}
		return out;
	}

	private static void appendStringList(StringBuilder sb, String key, List<String> values) {
		sb.append('"').append(escape(key)).append("\":[");
		if (values != null) {
			boolean first = true;
			for (String v : values) {
				if (v == null) {
					continue;
				}
				if (!first) {
					sb.append(',');
				}
				first = false;
				sb.append('"').append(escape(v)).append('"');
			}
		}
		sb.append(']');
	}

	private static String strip(String raw) {
		if (raw == null) {
			return "";
		}
		return ChatColor.stripColor(raw);
	}

	private static List<String> stripList(List<String> raw) {
		List<String> out = new ArrayList<>();
		if (raw == null) {
			return out;
		}
		for (String s : raw) {
			out.add(strip(s));
		}
		return out;
	}

	private static String escape(String raw) {
		if (raw == null) {
			return "";
		}
		StringBuilder out = new StringBuilder(raw.length() + 8);
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			switch (c) {
				case '\\' -> out.append("\\\\");
				case '"' -> out.append("\\\"");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> {
					if (c < 0x20) {
						out.append(String.format("\\u%04x", (int) c));
					} else {
						out.append(c);
					}
				}
			}
		}
		return out.toString();
	}
}
