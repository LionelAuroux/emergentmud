/*
 * EmergentMUD - A modern MUD with a procedurally generated world.
 * Copyright (C) 2016 Peter Keeler
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

package com.emergentmud.core.command;

import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.WorldManager;
import com.emergentmud.core.util.EntityUtil;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class QuitCommand implements Command {
    private EntityUtil entityUtil;
    private WorldManager worldManager;

    @Inject
    public QuitCommand(EntityUtil entityUtil,
                       WorldManager worldManager) {
        this.entityUtil = entityUtil;
        this.worldManager = worldManager;
    }

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String command, String[] tokens, String raw) {
        if (!"quit".equals(command.toLowerCase()) || !"now".equals(raw.toLowerCase())) {
            output.append("Usage: QUIT &lt;now&gt;");
            output.append("Please note that you must type out \"quit now\" in full to avoid doing it accidentally.");

            return output;
        }

        GameOutput enterMessage = new GameOutput(String.format("[yellow]%s has left the game.", entity.getName()))
                .append("")
                .append("> ");

        entityUtil.sendMessageToRoom(entity.getRoom(), entity, enterMessage);

        output.append("[yellow]Goodbye, " + entity.getName() + "[yellow]! Returning to the main menu...");
        output.append("<script type=\"text/javascript\">setTimeout(function(){ window.location=\"/\"; }, 2000);</script>");

        worldManager.remove(entity);

        return output;
    }
}
