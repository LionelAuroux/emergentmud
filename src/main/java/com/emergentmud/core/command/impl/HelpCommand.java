/*
 * EmergentMUD - A modern MUD with a procedurally generated world.
 * Copyright (C) 2016-2017 Peter Keeler
 *
 * This file is part of EmergentMUD.
 *
 * EmergentMUD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EmergentMUD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.emergentmud.core.command.impl;

import com.emergentmud.core.command.BaseCommand;
import com.emergentmud.core.command.Command;
import com.emergentmud.core.model.CommandMetadata;
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.CommandMetadataRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class HelpCommand extends BaseCommand {
    private static final Sort SORT = new Sort("name");

    private ApplicationContext applicationContext;
    private CommandMetadataRepository commandMetadataRepository;

    @Inject
    public HelpCommand(ApplicationContext applicationContext,
                       CommandMetadataRepository commandMetadataRepository) {

        this.applicationContext = applicationContext;
        this.commandMetadataRepository = commandMetadataRepository;

        setDescription("Shows the documentation for a command.");
        addParameter("command", true);
    }

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String command, String[] tokens, String raw) {
        if (tokens.length == 0) {
            output.append("[dwhite]=== [white]Command Listing [dwhite]===");
            output.append("[white]Type HELP [dwhite]&lt;[white]command[dwhite]&gt; [white]to get more detailed help for any of these commands.");
            output.append("");

            commandMetadataRepository.findAll(SORT)
                    .stream()
                    .filter(cm -> !cm.isAdmin() || entity.isAdmin())
                    .forEach(cm -> {
                        Command bean = (Command)applicationContext.getBean(cm.getBeanName());

                        output.append(String.format("[white]%s [dwhite]- [white]%s",
                                cm.getName(), bean.getDescription()));
                    });
        } else {
            CommandMetadata commandMetadata = commandMetadataRepository.findByName(tokens[0]);

            if (commandMetadata != null && (entity.isAdmin() || !commandMetadata.isAdmin())) {
                Command cmd = (Command)applicationContext.getBean(commandMetadata.getBeanName());

                cmd.usage(output, commandMetadata.getName());
            } else {
                output.append("There is no command by that name.");
            }
        }

        return output;
    }
}