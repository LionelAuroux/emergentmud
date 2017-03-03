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
import com.emergentmud.core.exception.NoAccountException;
import com.emergentmud.core.model.Account;
import com.emergentmud.core.model.CommandMetadata;
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.Essence;
import com.emergentmud.core.model.Room;
import com.emergentmud.core.model.SocialNetwork;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.AccountRepository;
import com.emergentmud.core.repository.CommandMetadataRepository;
import com.emergentmud.core.repository.EntityBuilder;
import com.emergentmud.core.repository.EntityRepository;
import com.emergentmud.core.repository.EssenceRepository;
import com.emergentmud.core.repository.WorldManager;
import com.emergentmud.core.util.EntityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class MainResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainResource.class);

    private ApplicationContext applicationContext;
    private List<SocialNetwork> networks;
    private SecurityContextLogoutHandler securityContextLogoutHandler;
    private AccountRepository accountRepository;
    private EssenceRepository essenceRepository;
    private EntityRepository entityRepository;
    private CommandMetadataRepository commandMetadataRepository;
    private WorldManager worldManager;
    private EntityUtil entityUtil;
    private Long worldExtent;

    @Inject
    public MainResource(ApplicationContext applicationContext,
                        @Qualifier("worldExtent") Long worldExtent,
                        List<SocialNetwork> networks,
                        SecurityContextLogoutHandler securityContextLogoutHandler,
                        AccountRepository accountRepository,
                        EssenceRepository essenceRepository,
                        EntityRepository entityRepository,
                        CommandMetadataRepository commandMetadataRepository,
                        WorldManager worldManager,
                        EntityUtil entityUtil) {

        this.applicationContext = applicationContext;
        this.worldExtent = worldExtent;
        this.networks = networks;
        this.securityContextLogoutHandler = securityContextLogoutHandler;
        this.accountRepository = accountRepository;
        this.essenceRepository = essenceRepository;
        this.entityRepository = entityRepository;
        this.commandMetadataRepository = commandMetadataRepository;
        this.worldManager = worldManager;
        this.entityUtil = entityUtil;
    }

    @RequestMapping("/")
    public String index(HttpSession httpSession, Principal principal, Model model) {
        model.addAttribute("networks", networks);

        if (principal == null) {
            return "index";
        }

        String network = (String)httpSession.getAttribute("social");
        String networkId = principal.getName();
        Account account = accountRepository.findBySocialNetworkAndSocialNetworkId(network, networkId);

        if (account == null) {
            account = new Account();
            account.setSocialNetwork(network);
            account.setSocialNetworkId(networkId);
            account = accountRepository.save(account);

            LOGGER.info("Created new account {}:{} -> {}", network, networkId, account.getId());
        }

        LOGGER.info("Successful login for: {}:{}", network, networkId);

        List<Essence> essences = essenceRepository.findByAccountId(account.getId());

        model.addAttribute("account", account);
        model.addAttribute("essences", essences);

        return "essence";
    }

    @RequestMapping("/social/{network}")
    public String social(@PathVariable String network, HttpSession session) {
        session.setAttribute("social", network);

        LOGGER.info("Logging in via social network: {}", network);

        return "redirect:/login/" + network;
    }

    @RequestMapping("/new-essence")
    public String newEssence() {
        return "new-essence";
    }

    @RequestMapping(method=RequestMethod.POST, value="/new-essence")
    public String saveNewEssence(HttpSession session, Principal principal, Essence essence, Model model) {
        String network = (String)session.getAttribute("social");
        String networkId = principal.getName();
        Account account = accountRepository.findBySocialNetworkAndSocialNetworkId(network, networkId);

        if (account == null) {
            throw new NoAccountException(network, networkId);
        }

        if (essence.getName().length() < 3) {
            model.addAttribute("essenceName", essence.getName());
            model.addAttribute("errorName", "Names must be at least 3 letters long.");
            return "new-essence";
        }

        if (essence.getName().length() > 30) {
            model.addAttribute("essenceName", essence.getName());
            model.addAttribute("errorName", "Names must be less than 30 letters long.");
            return "new-essence";
        }

        if (!essence.getName().matches("^[A-Z].+$")) {
            model.addAttribute("essenceName", essence.getName());
            model.addAttribute("errorName", "Names must begin with a capital letter.");
            return "new-essence";
        }

        if (!essence.getName().matches("^[A-Za-z'-]+$")) {
            model.addAttribute("essenceName", essence.getName());
            model.addAttribute("errorName", "Names may only contain letters, hyphens and apostrophes.");
            return "new-essence";
        }

        if (essence.getName().matches("^[A-Za-z'-]\\w+['-]$")) {
            model.addAttribute("essenceName", essence.getName());
            model.addAttribute("errorName", "Names may not end with a hyphen or apostrophe.");
            return "new-essence";
        }

        if (essence.getName().matches("^\\w+['-].*['-]\\w+$")) {
            model.addAttribute("essenceName", essence.getName());
            model.addAttribute("errorName", "Names may only contain one hyphen or apostrophe.");
            return "new-essence";
        }

        if (essenceRepository.count() == 0) {
            LOGGER.info("Making {} into an administrator", essence.getName());
            essence.setAdmin(true);
        }

        essence.setCreationDate(System.currentTimeMillis());
        essence.setAccountId(account.getId());
        essence = essenceRepository.save(essence);
        LOGGER.info("Saved new Essence: {} -> {}", essence.getName(), essence.getId());

        return "redirect:/";
    }

    @RequestMapping("/play/{id}")
    public String play(@PathVariable("id") String essenceId, HttpSession session, Principal principal, Model model) {
        if (StringUtils.isEmpty(essenceId)) {
            LOGGER.info("No ID provided.");
            return "redirect:/";
        }

        String network = (String)session.getAttribute("social");
        String networkId = principal.getName();
        Account account = accountRepository.findBySocialNetworkAndSocialNetworkId(network, networkId);

        if (account == null) {
            LOGGER.info("No such account: {}:{}", network, networkId);
            return "redirect:/";
        }

        List<Essence> essences = essenceRepository.findByAccountId(account.getId());
        Optional<Essence> eOptional = essences.stream()
                .filter(e -> essenceId.equals(e.getId()))
                .findFirst();

        if (!eOptional.isPresent()) {
            LOGGER.info("No such essence: {}", essenceId);
            return "redirect:/";
        }

        Essence essence = eOptional.get();
        Entity entity = essence.getEntity();

        if (entity == null) {
            LOGGER.info("Creating a new entity for {}", essence.getName());
            entity = new EntityBuilder()
                    .withName(essence.getName())
                    .withAdmin(essence.isAdmin())
                    .build();

            entity = entityRepository.save(entity);

            essence.setEntity(entity);
        }

        essence.setLastLoginDate(System.currentTimeMillis());
        essence = essenceRepository.save(essence);

        if (entity.getRoom() != null && entity.getStompSessionId() != null && entity.getStompUsername() != null) {
            LOGGER.info("Reconnecting: {}@{}", entity.getStompSessionId(), entity.getStompUsername());

            GameOutput out = new GameOutput("[red]This session has been reconnected in another browser.");
            entityUtil.sendMessageToEntity(entity, out);
        }

        long worldCenter = worldExtent / 2;

        if (worldManager.test(worldCenter, worldCenter, 0L)) {
            Room room = worldManager.put(entity, worldCenter, worldCenter, 0L);
            GameOutput enterMessage = new GameOutput(String.format("[yellow]%s has entered the game.", entity.getName()));

            entityUtil.sendMessageToRoom(room, entity, enterMessage);

            LOGGER.info("{} has entered the game", entity.getName());
        } else {
            LOGGER.error("Starting room does not exist!");
        }

        String breadcrumb = UUID.randomUUID().toString();
        Map<String, String> sessionMap = new HashMap<>();

        sessionMap.put("account", account.getId());
        sessionMap.put("essence", essence.getId());
        sessionMap.put("entity", entity.getId());

        session.setAttribute(breadcrumb, sessionMap);

        model.addAttribute("breadcrumb", breadcrumb);
        model.addAttribute("account", account);
        model.addAttribute("essence", essence);

        return "play";
    }

    @RequestMapping("/commands")
    public String commands(Model model) {
        List<CommandMetadata> metadata = commandMetadataRepository.findByAdmin(false);
        Map<String, Command> commandMap = new HashMap<>();

        metadata.forEach(m -> {
            Command command = (Command)applicationContext.getBean(m.getBeanName());
            commandMap.put(m.getName(), command);
        });

        model.addAttribute("metadataList", metadata);
        model.addAttribute("commandMap", commandMap);

        return "commands";
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         HttpSession httpSession,
                         Principal principal) {

        String network = (String)httpSession.getAttribute("social");
        String networkId = principal.getName();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        LOGGER.info("Logged out: {}:{}", network, networkId);
        securityContextLogoutHandler.logout(request, response, authentication);

        return "redirect:/";
    }
}
