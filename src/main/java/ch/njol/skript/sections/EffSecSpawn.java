/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityType;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Direction;
import ch.njol.skript.util.Getter;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Consumer;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO this won't show up in the docs, sections don't have a tab. We should create a tab for them,
//  and maybe add EffectSections to the effects page as well
@Name("Spawn")
@Description({"Spawn a creature. This can be used as an effect and as a section.",
	"If it is used as a section, the section is run before the entity is added to the world.",
	"You can modify the entity in this section, using for example 'event-entity' or 'cow'. ",
	"Do note that other event values, such as 'player', won't work in this section."})
@Examples({"spawn 3 creepers at the targeted block",
	"spawn a ghast 5 meters above the player",
	"spawn a zombie at the player:",
	"\tset name of the zombie to \"\""})
@Since("1.0, INSERT VERSION (with section)")
public class EffSecSpawn extends EffectSection {

	public static class SpawnEvent extends Event {
		private final Entity entity;

		public SpawnEvent(Entity entity) {
			this.entity = entity;
		}

		public Entity getEntity() {
			return entity;
		}

		@Override
		public @NotNull HandlerList getHandlers() {
			throw new IllegalStateException();
		}
	}

	static {
		Skript.registerSection(EffSecSpawn.class,
			"(spawn|summon) %entitytypes% [%directions% %locations%]",
			"(spawn|summon) %number% of %entitytypes% [%directions% %locations%]");
		EventValues.registerEventValue(SpawnEvent.class, Entity.class, new Getter<Entity, SpawnEvent>() {
			@Override
			public Entity get(SpawnEvent spawnEvent) {
				return spawnEvent.getEntity();
			}
		}, 0);
	}

	@Nullable
	public static Entity lastSpawned = null;

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<Location> locations;
	@SuppressWarnings("NotNullFieldNotInitialized")
	private Expression<EntityType> types;
	@Nullable
	private Expression<Number> amount;

	@Nullable
	private Trigger trigger;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Expression<?>[] exprs,
						int matchedPattern,
						Kleenean isDelayed,
						ParseResult parseResult,
						@Nullable SectionNode sectionNode,
						@Nullable List<TriggerItem> triggerItems) {
		amount = matchedPattern == 0 ? null : (Expression<Number>) (exprs[0]);
		types = (Expression<EntityType>) exprs[matchedPattern];
		locations = Direction.combine((Expression<? extends Direction>) exprs[1 + matchedPattern], (Expression<? extends Location>) exprs[2 + matchedPattern]);

		if (sectionNode != null)
			trigger = loadCode(sectionNode, "spawn", SpawnEvent.class);

		return true;
	}

	@Override
	@Nullable
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected TriggerItem walk(Event e) {
		lastSpawned = null;

		Object localVars = Variables.copyLocalVariables(e);

		Consumer<? extends Entity> consumer;
		if (trigger != null) {
			consumer = o -> {
				lastSpawned = o;
				SpawnEvent spawnEvent = new SpawnEvent(o);
				// Copy the local variables from the calling code to this section
				Variables.setLocalVariables(spawnEvent, localVars);
				trigger.execute(spawnEvent);
			};
		} else {
			consumer = null;
		}

		Number a = amount != null ? amount.getSingle(e) : 1;
		if (a != null) {
			EntityType[] ts = types.getArray(e);
			for (Location l : locations.getArray(e)) {
				for (EntityType type : ts) {
					for (int i = 0; i < a.doubleValue() * type.getAmount(); i++) {
						if (consumer != null)
							type.data.spawn(l, (Consumer) consumer); // lastSpawned set within Consumer
						else
							lastSpawned = type.data.spawn(l);
					}
				}
			}
		}

		return super.walk(e, false);
	}

	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return "spawn " + (amount != null ? amount.toString(e, debug) + " of " : "") + types.toString(e, debug) + " " + locations.toString(e, debug);
	}

}
