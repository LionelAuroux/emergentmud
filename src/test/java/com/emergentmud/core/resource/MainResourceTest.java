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

package com.emergentmud.core.resource;

import com.emergentmud.core.command.Command;
import com.emergentmud.core.command.Emote;
import com.emergentmud.core.exception.NoAccountException;
import com.emergentmud.core.model.Account;
import com.emergentmud.core.model.CommandMetadata;
import com.emergentmud.core.model.EmoteMetadata;
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.Essence;
import com.emergentmud.core.model.Room;
import com.emergentmud.core.model.SocialNetwork;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.AccountRepository;
import com.emergentmud.core.repository.CommandMetadataRepository;
import com.emergentmud.core.repository.EmoteMetadataRepository;
import com.emergentmud.core.repository.EntityRepository;
import com.emergentmud.core.repository.EssenceRepository;
import com.emergentmud.core.repository.RoomBuilder;
import com.emergentmud.core.repository.WorldManager;
import com.emergentmud.core.resource.model.PlayRequest;
import com.emergentmud.core.util.EntityUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class MainResourceTest {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NETWORK_NAME = "AlteraNet";
    private static final String NETWORK_ID = "alteranet";
    private static final String NETWORK_USER = "007";
    private static final String ACCOUNT_ID = "1234567890";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private SecurityContextLogoutHandler securityContextLogoutHandler;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EssenceRepository essenceRepository;

    @Mock
    private EntityRepository entityRepository;

    @Mock
    private CommandMetadataRepository commandMetadataRepository;

    @Mock
    private EmoteMetadataRepository emoteMetadataRepository;

    @Mock
    private RoomBuilder roomBuilder;

    @Mock
    private WorldManager worldManager;

    @Mock
    private EntityUtil entityUtil;

    @Mock
    private Emote emote;

    @Mock
    private HttpSession httpSession;

    @Mock
    private OAuth2Authentication principal;

    @Mock
    private Model model;

    @Mock
    private PlayRequest playRequest;

    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;

    @Captor
    private ArgumentCaptor<GameOutput> outputCaptor;

    @Captor
    private ArgumentCaptor<Entity> entityCaptor;

    private Account account;
    private Essence essence;
    private List<SocialNetwork> socialNetworks = new ArrayList<>();
    private List<Essence> essences = new ArrayList<>();
    private List<EmoteMetadata> emoteMetadata = new ArrayList<>();

    private MainResource mainResource;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        generateSocialNetworks();
        account = generateAccount();
        generateEssences();
        essence = essences.get(0);
        generateEmoteMetadata();

        when(worldManager.test(eq(0L), eq(0L), eq(0L))).thenReturn(true);
        when(httpSession.getAttribute(eq("social"))).thenReturn(NETWORK_ID);
        when(principal.getName()).thenReturn(NETWORK_USER);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = (Account)invocation.getArguments()[0];
            account.setId(UUID.randomUUID().toString());
            return account;
        });
        when(essenceRepository.save(any(Essence.class))).thenAnswer(invocation -> {
            Essence essence = (Essence)invocation.getArguments()[0];
            essence.setId(UUID.randomUUID().toString());
            return essence;
        });
        when(entityRepository.save(any(Entity.class))).thenAnswer(invocation -> {
            Entity entity = (Entity)invocation.getArguments()[0];
            entity.setId(UUID.randomUUID().toString());
            return entity;
        });
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq(NETWORK_ID), eq(NETWORK_USER))).thenReturn(account);
        when(essenceRepository.findByAccountId(anyString())).thenReturn(essences);
        when(playRequest.getEssenceId()).thenReturn("essence0");

        mainResource = new MainResource(
                applicationContext,
                socialNetworks,
                securityContextLogoutHandler,
                accountRepository,
                essenceRepository,
                entityRepository,
                commandMetadataRepository,
                emoteMetadataRepository,
                roomBuilder,
                worldManager,
                entityUtil,
                emote
        );
    }

    @Test
    public void testIndexNotAuthenticated() throws Exception {
        String view = mainResource.index(httpSession, null, model);

        verify(model).addAttribute(eq("networks"), eq(socialNetworks));
        verifyZeroInteractions(accountRepository);
        verifyZeroInteractions(essenceRepository);
        assertEquals("index", view);
    }

    @Test
    public void testIndexNewAccount() throws Exception {
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq(NETWORK_ID), eq(NETWORK_USER))).thenReturn(null);

        String view = mainResource.index(httpSession, principal, model);

        verify(model).addAttribute(eq("networks"), eq(socialNetworks));
        verify(accountRepository).save(accountCaptor.capture());
        verify(model).addAttribute(eq("account"), any(Account.class));
        verify(model).addAttribute(eq("essences"), eq(essences));
        assertEquals("essence", view);

        Account generated = accountCaptor.getValue();

        assertEquals(NETWORK_ID, generated.getSocialNetwork());
        assertEquals(NETWORK_USER, generated.getSocialNetworkId());
    }

    @Test
    public void testIndexExistingAccount() throws Exception {
        String view = mainResource.index(httpSession, principal, model);

        verify(model).addAttribute(eq("networks"), eq(socialNetworks));
        verify(accountRepository, never()).save(any(Account.class));
        verify(account, never()).setSocialNetwork(anyString());
        verify(account, never()).setSocialNetworkId(anyString());
        verify(model).addAttribute(eq("account"), eq(account));
        verify(model).addAttribute(eq("essences"), eq(essences));
        assertEquals("essence", view);
    }

    @Test
    public void testSocial() throws Exception {
        String view = mainResource.social(NETWORK_ID, httpSession);

        verify(httpSession).setAttribute(eq("social"), eq(NETWORK_ID));
        assertEquals("redirect:/login/" + NETWORK_ID, view);
    }

    @Test
    public void testNewEssence() throws Exception {
        String view = mainResource.newEssence();

        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveFirstNewEssence() throws Exception {
        when(essenceRepository.count()).thenReturn(0L);

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence).setAccountId(eq(ACCOUNT_ID));
        verify(essence).setAdmin(eq(true));
        verify(essence).setCreationDate(anyLong());
        assertEquals("redirect:/", view);
    }

    @Test
    public void testSaveNewEssence() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(essence).setCreationDate(anyLong());
        assertEquals("redirect:/", view);
    }

    @Test
    public void testSaveNewEssenceNameTooShort() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("A");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("A"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameTooLong() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("Supercalifragilisticexpealadocious");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("Supercalifragilisticexpealadocious"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameNotCapitalized() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("abraham");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("abraham"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameInvalidCharacters() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("Abra!ham");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("Abra!ham"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameStartsWithHyphen() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("-Abraham");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("-Abraham"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameStartsWithApostrophe() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("'Abraham");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("'Abraham"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameEndsWithHyphen() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("Abraham-");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("Abraham-"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameEndsWithApostrophe() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("Abraham'");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("Abraham'"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameMultipleSymbols1() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("Abra--ham");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("Abra--ham"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test
    public void testSaveNewEssenceNameMultipleSymbols2() throws Exception {
        when(essenceRepository.count()).thenReturn(100L);
        when(essence.getName()).thenReturn("Ab-ra-ham");

        String view = mainResource.saveNewEssence(httpSession, principal, essence, model);

        verify(essence, never()).setAccountId(eq(ACCOUNT_ID));
        verify(essence, never()).setAdmin(anyBoolean());
        verify(model).addAttribute(eq("essenceName"), eq("Ab-ra-ham"));
        verify(model).addAttribute(eq("errorName"), anyString());
        assertEquals("new-essence", view);
    }

    @Test(expected = NoAccountException.class)
    public void testSaveNewEssenceMissingAccount() throws Exception {
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq(NETWORK_ID), eq(NETWORK_USER))).thenReturn(null);

        mainResource.saveNewEssence(httpSession, principal, essence, model);
    }

    @Test
    public void testPlayExisting() throws Exception {
        String view = mainResource.play(playRequest, httpSession, principal, model);
        Entity entity = essence.getEntity();

        verify(roomBuilder, never()).generateRoom(eq(0L), eq(0L), eq(0L));
        verify(entityUtil).sendMessageToRoom(any(Room.class), any(Entity.class), outputCaptor.capture());
        verify(worldManager).put(eq(entity), eq(0L), eq(0L), eq(0L));
        verify(httpSession).setAttribute(anyString(), mapCaptor.capture());
        verify(model).addAttribute(eq("breadcrumb"), anyString());
        verify(model).addAttribute(eq("account"), eq(account));
        verify(model).addAttribute(eq("essence"), eq(essence));
        assertEquals("play", view);

        GameOutput output = outputCaptor.getValue();

        assertTrue(output.getOutput().get(0).startsWith("[yellow]"));

        Map<String, String> sessionMap = mapCaptor.getValue();

        assertEquals(account.getId(), sessionMap.get("account"));
        assertEquals(essence.getId(), sessionMap.get("essence"));
        assertEquals(entity.getId(), sessionMap.get("entity"));
    }

    @Test
    public void testPlayNoWorld() throws Exception {
        when(worldManager.test(eq(0L), eq(0L), eq(0L))).thenReturn(false);

        String view = mainResource.play(playRequest, httpSession, principal, model);
        Entity entity = essence.getEntity();

        verify(roomBuilder).generateRoom(eq(0L), eq(0L), eq(0L));
        verify(entityUtil).sendMessageToRoom(any(Room.class), any(Entity.class), outputCaptor.capture());
        verify(worldManager).put(eq(entity), eq(0L), eq(0L), eq(0L));
        verify(httpSession).setAttribute(anyString(), mapCaptor.capture());
        verify(model).addAttribute(eq("breadcrumb"), anyString());
        verify(model).addAttribute(eq("account"), eq(account));
        verify(model).addAttribute(eq("essence"), eq(essence));
        assertEquals("play", view);

        Map<String, String> sessionMap = mapCaptor.getValue();

        assertEquals(account.getId(), sessionMap.get("account"));
        assertEquals(essence.getId(), sessionMap.get("essence"));
        assertEquals(entity.getId(), sessionMap.get("entity"));
    }

    @Test
    public void testPlayNoId() throws Exception {
        when(playRequest.getEssenceId()).thenReturn(null);

        String view = mainResource.play(playRequest, httpSession, principal, model);

        verifyZeroInteractions(model);
        verifyZeroInteractions(httpSession);
        assertEquals("redirect:/", view);
    }

    @Test
    public void testPlayNoAccount() throws Exception {
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq(NETWORK_ID), eq(NETWORK_USER))).thenReturn(null);

        String view = mainResource.play(playRequest, httpSession, principal, model);

        verify(httpSession).getAttribute(eq("social"));
        verifyNoMoreInteractions(httpSession);
        verifyZeroInteractions(model);
        assertEquals("redirect:/", view);
    }

    @Test
    public void testPlayNoEssence() throws Exception {
        when(essenceRepository.findByAccountId(anyString())).thenReturn(new ArrayList<>());

        String view = mainResource.play(playRequest, httpSession, principal, model);

        verify(httpSession).getAttribute(eq("social"));
        verifyNoMoreInteractions(httpSession);
        verifyZeroInteractions(model);
        assertEquals("redirect:/", view);
    }

    @Test
    public void testPlayNoEntity() throws Exception {
        Essence essence1 = essences.get(1);

        when(essence1.getEntity()).thenReturn(null);
        when(playRequest.getEssenceId()).thenReturn("essence1");

        String view = mainResource.play(playRequest, httpSession, principal, model);

        verify(entityUtil).sendMessageToRoom(any(Room.class), any(Entity.class), outputCaptor.capture());
        verify(entityRepository).save(any(Entity.class));
        verify(essence1).setEntity(any(Entity.class));
        verify(essenceRepository).save(eq(essence1));
        verify(worldManager).put(any(Entity.class), eq(0L), eq(0L), eq(0L));
        verify(httpSession).setAttribute(anyString(), mapCaptor.capture());
        verify(model).addAttribute(eq("breadcrumb"), anyString());
        verify(model).addAttribute(eq("account"), eq(account));
        verify(model).addAttribute(eq("essence"), eq(essence1));
        assertEquals("play", view);

        GameOutput output = outputCaptor.getValue();

        assertTrue(output.getOutput().get(0).startsWith("[yellow]"));

        Map<String, String> sessionMap = mapCaptor.getValue();

        assertEquals(account.getId(), sessionMap.get("account"));
        assertEquals(essence1.getId(), sessionMap.get("essence"));
        assertTrue(sessionMap.containsKey("entity"));
    }

    @Test
    public void testPlayReconnect() throws Exception {
        Room room = mock(Room.class);
        Essence essence0 = essences.get(0);
        Entity entity0 = essence0.getEntity();

        when(entity0.getRoom()).thenReturn(room);
        when(entity0.getStompSessionId()).thenReturn("stompSessionId");
        when(entity0.getStompUsername()).thenReturn("stompUsername");

        String view = mainResource.play(playRequest, httpSession, principal, model);

        verify(entityUtil).sendMessageToEntity(any(Entity.class), outputCaptor.capture());
        verify(worldManager).put(any(Entity.class), eq(0L), eq(0L), eq(0L));
        verify(httpSession).setAttribute(anyString(), mapCaptor.capture());
        verify(model).addAttribute(eq("breadcrumb"), anyString());
        verify(model).addAttribute(eq("account"), eq(account));
        verify(model).addAttribute(eq("essence"), eq(essence));
        assertEquals("play", view);

        GameOutput output = outputCaptor.getValue();

        assertTrue(output.getOutput().get(0).startsWith("[red]"));
    }

    @Test
    public void testCommandsNotAuthenticated() throws Exception {
        List<CommandMetadata> metadata = generateCommandMetadata(false);

        when(commandMetadataRepository.findByAdmin(eq(false))).thenReturn(metadata);

        String view = mainResource.commands(model, null, httpSession);

        verifyZeroInteractions(httpSession);
        verify(commandMetadataRepository).findByAdmin(eq(false));
        verify(applicationContext, times(5)).getBean(startsWith("command"));
        verify(model).addAttribute(eq("metadataList"), anyListOf(CommandMetadata.class));
        verify(model).addAttribute(eq("commandMap"), anyMapOf(String.class, Command.class));

        assertEquals("commands", view);
    }

    @Test
    public void testCommandsAuthenticatedNoAdmins() throws Exception {
        List<CommandMetadata> metadata = generateCommandMetadata(false);

        when(commandMetadataRepository.findByAdmin(eq(false))).thenReturn(metadata);
        when(httpSession.getAttribute(eq("social"))).thenReturn("alteraBook");
        when(principal.getName()).thenReturn("2928749020");
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq("alteraBook"), eq("2928749020"))).thenReturn(account);
        when(account.getId()).thenReturn("accountId");
        when(essenceRepository.findByAccountId(eq("accountId"))).thenReturn(essences);

        String view = mainResource.commands(model, principal, httpSession);

        verify(httpSession).getAttribute(eq("social"));
        verify(commandMetadataRepository).findByAdmin(eq(false));
        verify(applicationContext, times(5)).getBean(startsWith("command"));
        verify(model).addAttribute(eq("metadataList"), anyListOf(CommandMetadata.class));
        verify(model).addAttribute(eq("commandMap"), anyMapOf(String.class, Command.class));

        assertEquals("commands", view);
    }

    @Test
    public void testCommandsAuthenticatedWithAdmins() throws Exception {
        List<CommandMetadata> metadata = generateCommandMetadata(true);

        when(commandMetadataRepository.findAll()).thenReturn(metadata);
        when(httpSession.getAttribute(eq("social"))).thenReturn("alteraBook");
        when(principal.getName()).thenReturn("2928749020");
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq("alteraBook"), eq("2928749020"))).thenReturn(account);
        when(account.getId()).thenReturn("accountId");
        when(essences.get(2).isAdmin()).thenReturn(true);
        when(essenceRepository.findByAccountId(eq("accountId"))).thenReturn(essences);

        String view = mainResource.commands(model, principal, httpSession);

        verify(httpSession).getAttribute(eq("social"));
        verify(commandMetadataRepository).findAll();
        verify(applicationContext, times(5)).getBean(startsWith("command"));
        verify(model).addAttribute(eq("metadataList"), anyListOf(CommandMetadata.class));
        verify(model).addAttribute(eq("commandMap"), anyMapOf(String.class, Command.class));

        assertEquals("commands", view);
    }

    @Test
    public void testEmotesNotAuthenticated() throws Exception {
        when(emoteMetadataRepository.findAll()).thenReturn(emoteMetadata);

        String view = mainResource.emotes(model, null, httpSession);

        verify(emoteMetadataRepository).findAll();
        verifyZeroInteractions(httpSession);
        verify(model).addAttribute(eq("self"), entityCaptor.capture());
        verify(model).addAttribute(eq("target"), entityCaptor.capture());
        verify(model).addAttribute(eq("metadataList"), anyListOf(EmoteMetadata.class));
        verify(model).addAttribute(eq("emoteMap"), anyMapOf(String.class, EmoteMetadata.class));
        verifyAllEmoteMetadata(emoteMetadata);

        List<Entity> captures = entityCaptor.getAllValues();

        assertEquals("emotes", view);
        assertEquals("Alice", captures.get(0).getName());
        assertEquals("Bob", captures.get(1).getName());
    }

    @Test
    public void testEmotesAuthenticatedNoEssences() throws Exception {
        when(emoteMetadataRepository.findAll()).thenReturn(emoteMetadata);
        when(httpSession.getAttribute(eq("social"))).thenReturn("social");
        when(principal.getName()).thenReturn("principal");
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq("social"), eq("principal"))).thenReturn(account);
        when(account.getId()).thenReturn("accountId");
        when(essenceRepository.findByAccountId(eq("accountId"))).thenReturn(new ArrayList<>());

        String view = mainResource.emotes(model, principal, httpSession);

        verify(emoteMetadataRepository).findAll();
        verify(httpSession).getAttribute(eq("social"));
        verify(model).addAttribute(eq("self"), entityCaptor.capture());
        verify(model).addAttribute(eq("target"), entityCaptor.capture());
        verify(model).addAttribute(eq("metadataList"), anyListOf(EmoteMetadata.class));
        verify(model).addAttribute(eq("emoteMap"), anyMapOf(String.class, EmoteMetadata.class));
        verifyAllEmoteMetadata(emoteMetadata);

        List<Entity> captures = entityCaptor.getAllValues();

        assertEquals("emotes", view);
        assertEquals("Alice", captures.get(0).getName());
        assertEquals("Bob", captures.get(1).getName());
    }

    @Test
    public void testEmotesAuthenticatedOneEssence() throws Exception {
        ArrayList<Essence> oneEssence = new ArrayList<>();

        oneEssence.add(essences.get(0));

        when(emoteMetadataRepository.findAll()).thenReturn(emoteMetadata);
        when(httpSession.getAttribute(eq("social"))).thenReturn("social");
        when(principal.getName()).thenReturn("principal");
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq("social"), eq("principal"))).thenReturn(account);
        when(account.getId()).thenReturn("accountId");
        when(essenceRepository.findByAccountId(eq("accountId"))).thenReturn(oneEssence);

        String view = mainResource.emotes(model, principal, httpSession);

        verify(emoteMetadataRepository).findAll();
        verify(httpSession).getAttribute(eq("social"));
        verify(model).addAttribute(eq("self"), entityCaptor.capture());
        verify(model).addAttribute(eq("target"), entityCaptor.capture());
        verify(model).addAttribute(eq("metadataList"), anyListOf(EmoteMetadata.class));
        verify(model).addAttribute(eq("emoteMap"), anyMapOf(String.class, EmoteMetadata.class));
        verifyAllEmoteMetadata(emoteMetadata);

        List<Entity> captures = entityCaptor.getAllValues();

        assertEquals("emotes", view);
        assertEquals("EssenceA", captures.get(0).getName());
        assertEquals("Bob", captures.get(1).getName());
    }

    @Test
    public void testEmotesAuthenticatedTwoEssences() throws Exception {
        ArrayList<Essence> twoEssences = new ArrayList<>();

        twoEssences.add(essences.get(0));
        twoEssences.add(essences.get(1));

        when(emoteMetadataRepository.findAll()).thenReturn(emoteMetadata);
        when(httpSession.getAttribute(eq("social"))).thenReturn("social");
        when(principal.getName()).thenReturn("principal");
        when(accountRepository.findBySocialNetworkAndSocialNetworkId(eq("social"), eq("principal"))).thenReturn(account);
        when(account.getId()).thenReturn("accountId");
        when(essenceRepository.findByAccountId(eq("accountId"))).thenReturn(twoEssences);

        String view = mainResource.emotes(model, principal, httpSession);

        verify(emoteMetadataRepository).findAll();
        verify(httpSession).getAttribute(eq("social"));
        verify(model).addAttribute(eq("self"), entityCaptor.capture());
        verify(model).addAttribute(eq("target"), entityCaptor.capture());
        verify(model).addAttribute(eq("metadataList"), anyListOf(EmoteMetadata.class));
        verify(model).addAttribute(eq("emoteMap"), anyMapOf(String.class, EmoteMetadata.class));
        verifyAllEmoteMetadata(emoteMetadata);

        List<Entity> captures = entityCaptor.getAllValues();

        assertEquals("emotes", view);
        assertEquals("EssenceA", captures.get(0).getName());
        assertEquals("EssenceB", captures.get(1).getName());
    }

    @Test
    public void testLogout() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        String view = mainResource.logout(request, response, httpSession, principal);

        verify(securityContextLogoutHandler).logout(eq(request), eq(response), any(Authentication.class));
        assertEquals("redirect:/", view);
    }

    private void generateSocialNetworks() {
        socialNetworks.add(new SocialNetwork(NETWORK_ID, NETWORK_NAME));
    }

    private Account generateAccount() {
        Account account = mock(Account.class);

        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getSocialNetwork()).thenReturn(NETWORK_ID);
        when(account.getSocialNetworkId()).thenReturn(NETWORK_USER);

        return account;
    }

    private void generateEssences() {
        for (int i = 0; i < 3; i++) {
            Essence essence = mock(Essence.class);
            Entity entity = mock(Entity.class);

            when(essence.getId()).thenReturn("essence" + i);
            when(essence.getName()).thenReturn("Essence" + ALPHABET.charAt(i));
            when(essence.getAccountId()).thenReturn(ACCOUNT_ID);
            when(essence.getEntity()).thenReturn(entity);

            when(entity.getId()).thenReturn("entity" + i);
            when(entity.getName()).thenReturn("Entity" + ALPHABET.charAt(i));

            essences.add(essence);
        }
    }

    private List<CommandMetadata> generateCommandMetadata(boolean admin) {
        List<CommandMetadata> commandMetadata = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            CommandMetadata metadata = mock(CommandMetadata.class);
            Command command = mock(Command.class);

            when(metadata.getBeanName()).thenReturn("command" + ALPHABET.charAt(i));
            when(metadata.getName()).thenReturn("command" + ALPHABET.charAt(i));

            if (admin) {
                when(metadata.isAdmin()).thenReturn(i % 2 == 0);
            } else {
                when(metadata.isAdmin()).thenReturn(false);
            }

            when(applicationContext.getBean(eq(metadata.getBeanName()))).thenReturn(command);

            commandMetadata.add(metadata);
        }

        return commandMetadata;
    }

    private void generateEmoteMetadata() {
        for (int i = 0; i < 5; i++) {
            EmoteMetadata metadata = mock(EmoteMetadata.class);

            emoteMetadata.add(metadata);
        }
    }

    private void verifyAllEmoteMetadata(List<EmoteMetadata> metadata) {
        metadata.forEach(m -> {
            verify(m).setToSelfUntargeted(anyString());
            verify(m).setToRoomUntargeted(anyString());
            verify(m).setToSelfWithTarget(anyString());
            verify(m).setToTarget(anyString());
            verify(m).setToRoomWithTarget(anyString());
            verify(m).setToSelfAsTarget(anyString());
            verify(m).setToRoomTargetingSelf(anyString());
        });
    }
}
