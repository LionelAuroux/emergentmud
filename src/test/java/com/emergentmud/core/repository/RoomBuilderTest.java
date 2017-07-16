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

import com.emergentmud.core.model.Direction;
import com.emergentmud.core.model.room.Biome;
import com.emergentmud.core.model.room.Room;
import com.emergentmud.core.model.WhittakerGridLocation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class RoomBuilderTest {
    @Mock
    private RoomRepository roomRepository;

    @Mock
    private WhittakerGridLocationRepository whittakerGridLocationRepository;

    @Spy
    private Random random;

    private List<WhittakerGridLocation> whittakerGridLocations = new ArrayList<>();
    private RoomBuilder roomBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Double springFrequency = 0.01;

        roomBuilder = new RoomBuilder(roomRepository, whittakerGridLocationRepository, random, springFrequency);

        doReturn(whittakerGridLocations).when(whittakerGridLocationRepository).findAll();
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgumentAt(0, Room.class));
    }

    @Test
    public void testRoomAlreadyExists() throws Exception {
        Room existing = mock(Room.class);

        when(roomRepository.findByXAndYAndZ(anyLong(), anyLong(), anyLong())).thenReturn(existing);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertEquals(existing, room);
    }

    @Test
    public void testGenerateFirstRoom() throws Exception {
        generateGridLocations(24);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        verify(roomRepository).findByXAndYAndZ(eq(0L), eq(0L), eq(0L));
        verify(whittakerGridLocationRepository).findAll();
        verify(roomRepository).findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.EAST.getX(), Direction.EAST.getY(), Direction.EAST.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.SOUTH.getX(), Direction.SOUTH.getY(), Direction.SOUTH.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.WEST.getX(), Direction.WEST.getY(), Direction.WEST.getZ());
        verify(roomRepository).save(any(Room.class));

        assertEquals(0L, (long)room.getX());
        assertEquals(0L, (long)room.getY());
        assertEquals(0L, (long)room.getZ());
        assertNotNull(room.getBiome());
        assertNotNull(room.getElevation());
        assertNotNull(room.getMoisture());
    }

    @Test
    public void testGenerateWithLegalNeighbor() throws Exception {
        generateGridLocations(2);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(1);
        when(neighbor.getMoisture()).thenReturn(1);
        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        verify(roomRepository).findByXAndYAndZ(eq(0L), eq(0L), eq(0L));
        verify(whittakerGridLocationRepository).findAll();
        verify(roomRepository).findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.EAST.getX(), Direction.EAST.getY(), Direction.EAST.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.SOUTH.getX(), Direction.SOUTH.getY(), Direction.SOUTH.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.WEST.getX(), Direction.WEST.getY(), Direction.WEST.getZ());
        verify(roomRepository).save(any(Room.class));

        assertEquals(0L, (long)room.getX());
        assertEquals(0L, (long)room.getY());
        assertEquals(0L, (long)room.getZ());
        assertNotNull(room.getBiome());
        assertNotNull(room.getElevation());
        assertNotNull(room.getMoisture());

        assertTrue(1 - room.getElevation() <= 1);
        assertTrue(1 - room.getMoisture() <= 1);
    }

    @Test
    public void testGenerateWithIllegalNeighbor() throws Exception {
        generateGridLocations(2);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(5); // this neighbor is too different for our biomes
        when(neighbor.getMoisture()).thenReturn(1);
        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        verify(roomRepository).findByXAndYAndZ(eq(0L), eq(0L), eq(0L));
        verify(whittakerGridLocationRepository).findAll();
        verify(roomRepository).findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.EAST.getX(), Direction.EAST.getY(), Direction.EAST.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.SOUTH.getX(), Direction.SOUTH.getY(), Direction.SOUTH.getZ());
        verify(roomRepository).findByXAndYAndZ(Direction.WEST.getX(), Direction.WEST.getY(), Direction.WEST.getZ());
        verify(roomRepository, never()).save(any(Room.class));

        assertNull(room);
    }

    @Test
    public void testNeighborHigher() throws Exception {
        generateGridLocations(1);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(2);
        when(neighbor.getMoisture()).thenReturn(1);

        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNotNull(room);
    }

    @Test
    public void testNeighborWetter() throws Exception {
        generateGridLocations(1);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(1);
        when(neighbor.getMoisture()).thenReturn(2);

        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNotNull(room);
    }

    @Test
    public void testNeighborSame() throws Exception {
        generateGridLocations(1);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(1);
        when(neighbor.getMoisture()).thenReturn(1);

        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNotNull(room);
    }

    @Test
    public void testNeighborTooWet() throws Exception {
        generateGridLocations(1);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(1);
        when(neighbor.getMoisture()).thenReturn(3);

        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNull(room);
    }

    @Test
    public void testNeighborTooHigh() throws Exception {
        generateGridLocations(1);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(3);
        when(neighbor.getMoisture()).thenReturn(1);

        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNull(room);
    }

    @Test
    public void testNeighborTooWetAndHigh() throws Exception {
        generateGridLocations(1);

        Room neighbor = mock(Room.class);

        when(neighbor.getElevation()).thenReturn(2);
        when(neighbor.getMoisture()).thenReturn(2);

        when(roomRepository.findByXAndYAndZ(Direction.NORTH.getX(), Direction.NORTH.getY(), Direction.NORTH.getZ())).thenReturn(neighbor);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNull(room);
    }

    @Test
    public void testSpringGeneration() throws Exception {
        Biome biome = mock(Biome.class);
        WhittakerGridLocation whittakerGridLocation = mock(WhittakerGridLocation.class);

        when(biome.getName()).thenReturn("Snow");
        when(whittakerGridLocation.getBiome()).thenReturn(biome);
        when(whittakerGridLocation.getElevation()).thenReturn(WhittakerGridLocation.MAX_ELEVATION);
        when(whittakerGridLocation.getMoisture()).thenReturn(3);

        whittakerGridLocations.clear();
        whittakerGridLocations.add(whittakerGridLocation);

        when(random.nextDouble()).thenReturn(0.0, 0.01);

        Room room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNotNull(room.getWater());

        room = roomBuilder.generateRoom(0L, 0L, 0L);

        assertNotNull(room);
    }

    private void generateGridLocations(int count) {
        whittakerGridLocations.clear();

        for (int i = 1; i <= count; i++) {
            Biome biome = mock(Biome.class);

            when(biome.getName()).thenReturn("Biome " + i);

            WhittakerGridLocation whittakerGridLocation = mock(WhittakerGridLocation.class);

            when(whittakerGridLocation.getBiome()).thenReturn(biome);
            when(whittakerGridLocation.getElevation()).thenReturn(i);
            when(whittakerGridLocation.getMoisture()).thenReturn(i);

            whittakerGridLocations.add(whittakerGridLocation);
        }
    }
}
