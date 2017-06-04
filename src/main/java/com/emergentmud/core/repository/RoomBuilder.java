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

package com.emergentmud.core.repository;

import com.emergentmud.core.model.room.FlowType;
import com.emergentmud.core.model.room.Room;
import com.emergentmud.core.model.WhittakerGridLocation;
import com.emergentmud.core.model.room.Water;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Component
public class RoomBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomBuilder.class);
    private static final int NEIGHBOR_DISTANCE = 2;
    private static final int CHANGE_TOLERANCE = 1;

    private RoomRepository roomRepository;
    private WhittakerGridLocationRepository whittakerGridLocationRepository;
    private final Random RANDOM;
    private final Double SPRING_FREQUENCY;

    @Inject
    public RoomBuilder(RoomRepository roomRepository,
                       WhittakerGridLocationRepository whittakerGridLocationRepository,
                       Random random,
                       Double springFrequency) {
        this.roomRepository = roomRepository;
        this.whittakerGridLocationRepository = whittakerGridLocationRepository;
        this.RANDOM = random;
        this.SPRING_FREQUENCY = springFrequency;
    }

    public Room generateRoom(long x, long y, long z) {
        Room room = roomRepository.findByXAndYAndZ(x, y, z);

        if (room != null) {
            return room;
        }

        room = generateRandomRoom(x, y, z);

        if (room == null) {
            LOGGER.debug("No valid biomes for room at ({}, {}, {})", x, y, z);
            return null;
        }

        return roomRepository.save(room);
    }

    private Room generateRandomRoom(long x, long y, long z) {
        List<WhittakerGridLocation> gridLocations = whittakerGridLocationRepository.findAll();
        List<Room> neighbors = roomRepository.findByXBetweenAndYBetweenAndZ(
                x - NEIGHBOR_DISTANCE,
                x + NEIGHBOR_DISTANCE,
                y - NEIGHBOR_DISTANCE,
                y + NEIGHBOR_DISTANCE,
                z);

        neighbors.forEach(neighbor -> {
            for (Iterator<WhittakerGridLocation> iterator = gridLocations.iterator(); iterator.hasNext();) {
                WhittakerGridLocation gridLocation = iterator.next();
                double elevationDiff = Math.abs(neighbor.getElevation() - gridLocation.getElevation());
                double moistureDiff = Math.abs(neighbor.getMoisture() - gridLocation.getMoisture());

                if (elevationDiff > CHANGE_TOLERANCE
                        || moistureDiff > CHANGE_TOLERANCE
                        || (elevationDiff == CHANGE_TOLERANCE && moistureDiff == CHANGE_TOLERANCE)) {
                    iterator.remove();
                }
            }
        });

        if (gridLocations.isEmpty()) {
            return null;
        }

        Collections.shuffle(gridLocations);

        WhittakerGridLocation whittaker = gridLocations.get(0);
        Room room = new Room();

        room.setLocation(x, y, z);
        room.setBiome(whittaker.getBiome());
        room.setElevation(whittaker.getElevation());
        room.setMoisture(whittaker.getMoisture());

        if (WhittakerGridLocation.MAX_ELEVATION == room.getElevation()) {
            if (RANDOM.nextDouble() < SPRING_FREQUENCY) {
                room.setWater(new Water(FlowType.SPRING, null));
            }
        }

        return room;
    }
}
