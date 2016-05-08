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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SayCommandTest {
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private GameOutput output;

    @Mock
    private Entity entity;

    @Mock
    private Entity stu;

    @Mock
    private Room room;

    @Captor
    private ArgumentCaptor<MessageHeaders> headerCaptor;

    private List<Entity> roomContents;

    private SayCommand command;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        roomContents = generateRoomContents();

        when(entity.getId()).thenReturn("id");
        when(entity.getName()).thenReturn("Testy");
        when(entity.getRoom()).thenReturn(room);
        when(room.getContents()).thenReturn(roomContents);

        command = new SayCommand(simpMessagingTemplate);
    }

    @Test
    public void testSaySomething() throws Exception {
        GameOutput response = command.execute(output, entity,
                new String[] { "Feed", "me", "a", "stray", "cat." },
                "Feed me a stray cat.");

        verify(response).append(eq("[cyan]You say 'Feed me a stray cat.[cyan]'"));
        verify(simpMessagingTemplate).convertAndSendToUser(
                eq("stuSimpUsername"),
                eq("/queue/output"),
                any(GameOutput.class),
                headerCaptor.capture()
        );

        MessageHeaders headers = headerCaptor.getValue();
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);

        assertEquals("stuSimpSessionId", accessor.getSessionId());
        assertTrue(accessor.isMutable());
    }

    @Test
    public void testSaySomethingWithSymbols() throws Exception {
        GameOutput response = command.execute(output, entity,
                new String[] { "<script", "type=\"text/javascript\">var", "evil", "=", "\"stuff\";</script>" },
                "<script type=\"text/javascript\">var evil = \"stuff\";</script>");

        verify(response).append(eq("[cyan]You say '&lt;script type=&quot;text/javascript&quot;&gt;var evil = &quot;stuff&quot;;&lt;/script&gt;[cyan]'"));
        verify(simpMessagingTemplate).convertAndSendToUser(
                eq("stuSimpUsername"),
                eq("/queue/output"),
                any(GameOutput.class),
                headerCaptor.capture()
        );

        MessageHeaders headers = headerCaptor.getValue();
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);

        assertEquals("stuSimpSessionId", accessor.getSessionId());
        assertTrue(accessor.isMutable());
    }

    @Test
    public void testSayNothing() throws Exception {
        GameOutput response = command.execute(output, entity, new String[] {}, "");

        verify(response).append(eq("What would you like to say?"));
    }

    private List<Entity> generateRoomContents() {
        List<Entity> contents = new ArrayList<>();

        stu = mock(Entity.class);

        when(stu.getId()).thenReturn("stuId");
        when(stu.getName()).thenReturn("Stu");
        when(stu.getStompSessionId()).thenReturn("stuSimpSessionId");
        when(stu.getStompUsername()).thenReturn("stuSimpUsername");

        contents.add(stu);

        return contents;
    }
}
