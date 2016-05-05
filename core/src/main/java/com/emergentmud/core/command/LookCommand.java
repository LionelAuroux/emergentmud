/*
 * EmergentMUD - A modern MUD with a procedurally generated world.
 * Copyright (C) 2016 BoneVM, LLC
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
import com.emergentmud.core.model.Room;
import com.emergentmud.core.model.stomp.GameOutput;
import org.springframework.stereotype.Component;

@Component
public class LookCommand implements Command {
    @Override
    public GameOutput execute(GameOutput output, Entity entity, String[] tokens, String raw) {
        if (entity.getRoom() == null) {
            output.append("[black]You are floating in a formless void.");
        } else {
            Room room = entity.getRoom();

            output.append(String.format("[yellow]Floating in the Void [dyellow](%d, %d, %d)", room.getX(), room.getY(), room.getZ()));
            output.append("[default]There is nothing but inky blackness around you for as far as the eye can see.");
            output.append("[dcyan]Exits: none");

            room.getContents().stream()
                    .filter(content -> !content.getId().equals(entity.getId()))
                    .forEach(content -> output.append("[green]" + content.getName() + " is here."));
        }

        return output;
    }
}
