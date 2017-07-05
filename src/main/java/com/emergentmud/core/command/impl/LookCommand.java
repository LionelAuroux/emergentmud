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
import com.emergentmud.core.model.Direction;
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.room.FlowType;
import com.emergentmud.core.model.room.Room;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.EntityRepository;
import com.emergentmud.core.repository.RoomRepository;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class LookCommand extends BaseCommand {
    private EntityRepository entityRepository;
    private RoomRepository roomRepository;

    @Inject
    public LookCommand(EntityRepository entityRepository, RoomRepository roomRepository) {
        this.entityRepository = entityRepository;
        this.roomRepository = roomRepository;

        setDescription("Describes the things in the world around you.");
        addParameter("target", false);
    }

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String command, String[] tokens, String raw) {
        if (entity.getRoom() == null) {
            output.append("[black]You are floating in a formless void.");
        } else {
            String roomName;
            StringBuilder roomDescription = new StringBuilder();
            Room room = entity.getRoom();

            if (room.getBiome() == null) {
                roomName = "Incomplete Geography";
                roomDescription.append("The geography around here seems... unfinished. The ground is just one " +
                        "seemingly endless, seamless, flat gray plane. No native plant or animal life is evident at all.");
            } else {
                roomName = room.getBiome().getName();
                roomDescription.append(String.format(" The biome here is %s.", room.getBiome().getName()));
            }

            if (room.getWater() != null) {
                room.getWater().keySet()
                        .stream()
                        .filter(k -> room.getWater().get(k).equals(FlowType.IN))
                        .forEach(i -> {
                            roomDescription.append(" ");
                            if (i.equals(Direction.NOWHERE.getName())) {
                                roomDescription.append(room.getBiome().getSpringText());
                            } else {
                                roomDescription.append(room.getBiome().getFlowText().replaceAll("\\[outlets]", i));
                            }
                        });

                room.getWater().keySet()
                        .stream()
                        .filter(k -> room.getWater().get(k).equals(FlowType.OUT))
                        .forEach(o -> {
                            roomDescription.append(" ");
                            if (o.equals(Direction.NOWHERE.getName())) {
                                roomDescription.append(room.getBiome().getSpringText());
                            } else {
                                roomDescription.append(room.getBiome().getFlowText().replaceAll("\\[outlets]", o));
                            }
                        });
            }

            StringBuilder exits = new StringBuilder("[dcyan]Exits:");

            Direction.DIRECTIONS.forEach(d -> {
                exits.append(" ");

                Room neighbor = roomRepository.findByXAndYAndZ(
                        room.getX() + d.getX(),
                        room.getY() + d.getY(),
                        room.getZ() + d.getZ());

                exits.append(neighbor == null ? "[red]" : "[cyan]");
                exits.append(d.getName());
            });

            output.append(String.format("[yellow]%s [dyellow](%d, %d, %d)",
                    roomName,
                    room.getX(),
                    room.getY(),
                    room.getZ()));
            output.append(String.format("[default]%s", roomDescription.toString().trim()));
            output.append(exits.toString());

            List<Entity> contents = entityRepository.findByRoom(room);

            contents.stream()
                    .filter(content -> !content.getId().equals(entity.getId()))
                    .forEach(content -> output.append("[green]" + content.getName() + " is here."));
        }

        return output;
    }
}
