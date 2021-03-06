/*
 * This file is part of PlaceholderAPI
 *
 * PlaceholderAPI
 * Copyright (c) 2015 - 2020 PlaceholderAPI Team
 *
 * PlaceholderAPI free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlaceholderAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.clip.placeholderapi.commands.impl.cloud;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.commands.PlaceholderCommand;
import me.clip.placeholderapi.commands.PlaceholderCommandRouter;
import me.clip.placeholderapi.libs.JSONMessage;
import me.clip.placeholderapi.util.ColorPalette;
import me.clip.placeholderapi.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class CommandECloud extends PlaceholderCommand
{

	@Unmodifiable
	private static final List<PlaceholderCommand> COMMANDS = ImmutableList.of(new CommandECloudClear(),
																			  new CommandECloudToggle(),
																			  new CommandECloudStatus(),
																			  new CommandECloudUpdate(),
																			  new CommandECloudRefresh(),
																			  new CommandECloudDownload(),
																			  new CommandECloudExpansionInfo(),
																			  new CommandECloudExpansionList(),
																			  new CommandECloudExpansionPlaceholders());

	static
	{
		COMMANDS.forEach(command -> command.setPermission("placeholderapi.ecloud." + command.getLabel()));
	}

	@NotNull
	@Unmodifiable
	private final Map<String, PlaceholderCommand> commands;


	public CommandECloud()
	{
		super("ecloud", "View all eCloud related commands.");

		final ImmutableMap.Builder<String, PlaceholderCommand> commands = ImmutableMap.builder();

		for (final PlaceholderCommand command : COMMANDS)
		{
			command.getLabels().forEach(label -> commands.put(label, command));
		}

		this.commands = commands.build();
	}


	@Override
	public void evaluate(@NotNull final PlaceholderAPIPlugin plugin, @NotNull final CommandSender sender, @NotNull final String alias, @NotNull @Unmodifiable final List<String> params)
	{
		if (params.isEmpty())
		{
			if (!(sender instanceof Player)) {
				Msg.msg(sender, "&b&lPlaceholderAPI &8- &7eCloud Help Menu &8- ");
				final StringBuilder builder = new StringBuilder();
				for (final PlaceholderCommand command : COMMANDS)
				{
					if (command.equals(this))
					{
						continue;
					}

					builder.append(ColorPalette.MAIN_GRAY.getColor()).append(" • ").append(ColorPalette.MAIN_BLUE.getColor()).append("/papi ecloud ").append(ColorPalette.MAIN_WHITE.getColor()).append(command.getLabel()).append("\n");
					builder.append(ColorPalette.MAIN_GRAY.getColor()).append(ColorPalette.ITALIC.getColor()).append("   ").append(command.getDescription()).append("\n");
				}

				sender.sendMessage(Msg.color(builder.toString()));
				return;
			}

			final Player player = (Player) sender;
			for (final PlaceholderCommand command : PlaceholderCommandRouter.COMMANDS)
			{
				if (command.equals(this))
				{
					continue;
				}

				final JSONMessage message = JSONMessage.create(Msg.color(ColorPalette.MAIN_GRAY.getColor() + " • " + ColorPalette.MAIN_BLUE.getColor() + "/papi ecloud" + ColorPalette.MAIN_WHITE.getColor() + command.getLabel()));
				final String tooltip = ColorPalette.MAIN_GRAY.getColor() + command.getDescription() + "\n\n" + ColorPalette.MAIN_GRAY.getColor() + "Permission: " + ColorPalette.MAIN_WHITE.getColor() + ColorPalette.UNDERLINE.getColor() + command.getPermission();

				message.tooltip(Msg.color(tooltip));
				message.send(player);
			}

			return;
			/*
			Msg.msg(sender,
					"&b&lPlaceholderAPI &8- &7eCloud Help Menu &8- ",
					" ",
					"&b/papi &fenable/disable/toggle",
					"  &7&oEnable or disable the eCloud",
					"&b/papi &fecloud status",
					"  &7&oView status of the eCloud",
					"&b/papi &fecloud list <all/{author}/installed> {page}",
					"  &7&oList all/author specific available expansions",
					"&b/papi &fecloud info <expansion name> {version}",
					"  &7&oView information about a specific expansion available on the eCloud",
					"&b/papi &fecloud placeholders <expansion name>",
					"  &7&oView placeholders for an expansion",
					"&b/papi &fecloud download <expansion name> {version}",
					"  &7&oDownload an expansion from the eCloud",
					"&b/papi &fecloud update <expansion name/all>",
					"  &7&oUpdate a specific/all installed expansions",
					"&b/papi &fecloud refresh",
					"  &7&oFetch the most up to date list of expansions available.",
					"&b/papi &fecloud clear",
					"  &7&oClear the expansion cloud cache.");

			return;
			*/
		}

		final String             search = params.get(0).toLowerCase();
		final PlaceholderCommand target = commands.get(search);

		if (target == null)
		{
			Msg.msg(sender, "&cUnknown command &7ecloud " + search);
			return;
		}

		final String permission = target.getPermission();
		if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission))
		{
			Msg.msg(sender, "&cYou do not have permission to do this!");
			return;
		}

		if (!(target instanceof CommandECloudToggle) && !plugin.getPlaceholderAPIConfig().isCloudEnabled())
		{
			Msg.msg(sender,
					"&cThe eCloud Manager is not enabled!");
			return;
		}

		target.evaluate(plugin, sender, search, params.subList(1, params.size()));
	}

	@Override
	public void complete(@NotNull final PlaceholderAPIPlugin plugin, @NotNull final CommandSender sender, @NotNull final String alias, @NotNull @Unmodifiable final List<String> params, @NotNull final List<String> suggestions)
	{
		if (params.size() <= 1)
		{
			final Stream<String> targets = filterByPermission(sender, commands.values().stream()).map(PlaceholderCommand::getLabels).flatMap(Collection::stream);
			suggestByParameter(targets, suggestions, params.isEmpty() ? null : params.get(0));

			return; // send sub commands
		}

		final String             search = params.get(0).toLowerCase();
		final PlaceholderCommand target = commands.get(search);

		if (target == null)
		{
			return;
		}

		target.complete(plugin, sender, search, params.subList(1, params.size()), suggestions);
	}

}
