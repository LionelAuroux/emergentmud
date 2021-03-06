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

import com.emergentmud.core.command.BaseCommunicationCommandTest;
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.room.Room;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.EntityRepository;
import com.emergentmud.core.repository.RoomRepository;
import com.emergentmud.core.service.EntityService;
import com.emergentmud.core.service.RoomService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;

import static com.emergentmud.core.command.impl.ShoutCommand.SHOUT_DISTANCE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShoutCommandTest extends BaseCommunicationCommandTest {
    @Mock
    private EntityRepository entityRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private EntityService entityService;

    @Spy
    private RoomService roomService;

    @Mock
    private GameOutput output;

    @Mock
    private Room room;

    @Mock
    private Entity entity;

    @Captor
    private ArgumentCaptor<GameOutput> outputCaptor;

    @Captor
    private ArgumentCaptor<List<Room>> roomListCaptor;

    private String cmd = "shout";

    private ShoutCommand command;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(entity.getId()).thenReturn("id");
        when(entity.getName()).thenReturn("Testy");
        when(entity.getRoom()).thenReturn(room);
        when(room.getX()).thenReturn(0L);
        when(room.getY()).thenReturn(0L);
        when(room.getZ()).thenReturn(0L);

        command = new ShoutCommand(roomRepository, entityRepository, roomService, entityService);
    }

    @Test
    public void testDescription() throws Exception {
        assertNotEquals("No description.", command.getDescription());
    }

    @Test
    public void testShoutSomething() throws Exception {
        GameOutput response = command.execute(output, entity, cmd,
                new String[] { "Feed", "me", "a", "stray", "cat." },
                "Feed me a stray cat.");

        verify(response).append(eq("[dyellow]You shout 'Feed me a stray cat.[dyellow]'"));
        verify(entityService).sendMessageToListeners(anyListOf(Entity.class), eq(entity), outputCaptor.capture());

        GameOutput output = outputCaptor.getValue();

        assertTrue(output.getOutput().get(0).equals("[dyellow]Testy shouts 'Feed me a stray cat.[dyellow]'"));
    }

    @Test
    public void testShoutRadius() throws Exception {
        List<Room> rooms = new ArrayList<>();

        for (long y = -SHOUT_DISTANCE; y < SHOUT_DISTANCE; y++) {
            for (long x = -SHOUT_DISTANCE; x < SHOUT_DISTANCE; x++) {
                Room room = mock(Room.class);

                when(room.getX()).thenReturn(x);
                when(room.getY()).thenReturn(y);
                when(room.getZ()).thenReturn(0L);

                rooms.add(room);
            }
        }

        when(roomRepository.findByXBetweenAndYBetweenAndZBetween(
                eq(-7L),
                eq(7L),
                eq(-7L),
                eq(7L),
                eq(-7L),
                eq(7L)
        )).thenReturn(rooms);

        GameOutput response = command.execute(output, entity, cmd,
                new String[] { "Feed", "me", "a", "stray", "cat." },
                "Feed me a stray cat.");

        verify(entityRepository).findByRoomIn(roomListCaptor.capture());

        List<Room> filteredRooms = roomListCaptor.getValue();

        assertNotNull(response);
        assertEquals(147, filteredRooms.size()); // 196 would be the full square
                                                 // 147 is the circle
    }

    @Test
    public void testShoutSomethingWithSymbols() throws Exception {
        GameOutput response = command.execute(output, entity, cmd,
                new String[] { "<script", "type=\"text/javascript\">var", "evil", "=", "\"stuff\";</script>" },
                "<script type=\"text/javascript\">var evil = \"stuff\";</script>");

        verify(response).append(eq("[dyellow]You shout '&lt;script type=&quot;text/javascript&quot;&gt;var evil = &quot;stuff&quot;;&lt;/script&gt;[dyellow]'"));
        verify(entityService).sendMessageToListeners(anyListOf(Entity.class), eq(entity), outputCaptor.capture());

        GameOutput output = outputCaptor.getValue();

        assertTrue(output.getOutput().get(0).equals("[dyellow]Testy shouts '&lt;script type=&quot;text/javascript&quot;&gt;var evil = &quot;stuff&quot;;&lt;/script&gt;[dyellow]'"));
    }

    @Test
    public void testShoutNothing() throws Exception {
        GameOutput response = command.execute(output, entity, cmd, new String[] {}, "");

        verify(response).append(eq("What would you like to shout?"));
    }
}
