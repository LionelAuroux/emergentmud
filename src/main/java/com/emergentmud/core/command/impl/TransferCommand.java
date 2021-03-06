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
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.room.Room;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.WorldManager;
import com.emergentmud.core.service.EntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;

@Component
public class TransferCommand extends BaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransferCommand.class);

    private ApplicationContext applicationContext;
    private WorldManager worldManager;
    private EntityService entityService;

    @Inject
    public TransferCommand(ApplicationContext applicationContext,
                           WorldManager worldManager,
                           EntityService entityService) {

        this.applicationContext = applicationContext;
        this.worldManager = worldManager;
        this.entityService = entityService;

        setDescription("Instantly transport someone from wherever they are to here.");
        addParameter("person", true);
    }

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String command, String[] tokens, String raw) {
        if (tokens.length != 1) {
            usage(output, command);
            return output;
        }

        Optional<Entity> targetOptional = entityService.entitySearchInWorld(entity, tokens[0]);

        if (!targetOptional.isPresent()) {
            output.append("[yellow]There is no one by that name.");
            return output;
        }

        Entity target = targetOptional.get();

        if (target.equals(entity)) {
            output.append("[yellow]You can't teleport yourself. Use GOTO instead.");
            return output;
        }

        if (target.getRoom().equals(entity.getRoom())) {
            output.append("[yellow]You're already there.");
            return output;
        }

        Room room = entity.getRoom();
        long[] location = new long[] {
                room.getX(),
                room.getY(),
                room.getZ()
        };

        LOGGER.trace("Location before: ({}, {}, {})",
                target.getRoom().getX(),
                target.getRoom().getY(),
                target.getRoom().getZ());

        String exitMessage = String.format("%s disappears in a puff of smoke!", target.getName());
        entityService.sendMessageToRoom(target.getRoom(), target, new GameOutput(exitMessage));

        room = worldManager.put(target, location[0], location[1], location[2]);
        LOGGER.trace("Location after: ({}, {}, {})", location[0], location[1], location[2]);

        String enterMessage = String.format("%s appears in a puff of smoke!", target.getName());

        entityService.sendMessageToRoom(room, Arrays.asList(entity, target), new GameOutput(enterMessage));
        output
                .append(String.format("[yellow]You transfer %s.", target.getName()))
                .append(enterMessage);

        Command look = (Command) applicationContext.getBean("lookCommand");
        GameOutput lookOutput = new GameOutput();

        lookOutput.append(String.format("[yellow]%s TRANSFERS you!", entity.getName()));

        look.execute(lookOutput, target, "look", new String[0], "");
        entityService.sendMessageToEntity(target, lookOutput);

        return output;
    }
}
