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
 *
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.expressions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;
import ch.njol.skript.aliases.ItemType;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

@Name("Book Pages")
@Description("The pages of a book.")
@Examples({"on book sign:",
			"\tmessage \"Book Pages: %pages of event-item%\"",
			"\tmessage \"Book Page 1: %page 1 of event-item%\""})
@Since("2.2-dev31")
public class ExprBookPages extends SimpleExpression<String> {
	
	static {
		Skript.registerExpression(ExprBookPages.class, String.class, ExpressionType.PROPERTY,
				"[all] [the] [book] (pages|content) of %itemtypes%",
				"%itemtypes%'s [book] (pages|content)",
				"[book] page %number% of %itemtypes%",
				"%itemtypes%'[s] [book] page %number%");
	}
	
	@SuppressWarnings("null")
	private Expression<ItemType> books;
	@Nullable
	private Expression<Number> page = null;
	
	@SuppressWarnings({"unchecked", "null"})
	@Override
	public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
		books = (Expression<ItemType>) exprs[matchedPattern == 3 ? 1 : 0];
		
		if (matchedPattern > 1)
			page = (Expression<Number>) exprs[matchedPattern ^ 2];
		return true;
	}
	
	@SuppressWarnings("null")
	@Nullable
	@Override
	protected String[] get(Event e) {
		ItemType[] items = books.getArray(e);
		Number n = page != null ? this.page.getSingle(e) : null;
		List<String> pages = new ArrayList<>();
		
		for (ItemType item : items) {
			ItemMeta meta = item.getItemMeta();
			
			if (!(meta instanceof BookMeta))
				continue;
			
			BookMeta book = (BookMeta) meta;
			
			if (!book.hasPages())
				continue;
			
			if (n != null) {
				int p = n.intValue();
				if (p <= book.getPageCount())
					pages.add(book.getPage(p));
			} else {
				for (int p = 1; p <= book.getPageCount(); p++)
					pages.add(book.getPage(p));
			}
		}
		
		if (pages.isEmpty())
			return null;
		
		return pages.toArray(new String[0]);
	}
	
	@Override
	public boolean isSingle() {
		return page != null;
	}
	
	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
	
	@Override
	public String toString(@Nullable Event e, boolean debug) {
		return (page != null ? "book page " + page.toString(e, debug) + " "
			: "all the pages of ") + books.toString(e, debug);
	}
}
