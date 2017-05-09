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
import com.emergentmud.core.command.Parameter;
import com.emergentmud.core.model.Capability;
import com.emergentmud.core.model.CapabilityObject;
import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.AccountRepository;
import com.emergentmud.core.repository.CapabilityRepository;
import com.emergentmud.core.repository.EntityRepository;
import com.emergentmud.core.util.EntityUtil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class CapabilityEditCommand extends BaseCommand {
    static final Sort SORT = new Sort("name");

    private CapabilityRepository capabilityRepository;
    private AccountRepository accountRepository;
    private EntityRepository entityRepository;
    private EntityUtil entityUtil;

    @Inject
    public CapabilityEditCommand(CapabilityRepository capabilityRepository,
                                 AccountRepository accountRepository,
                                 EntityRepository entityRepository,
                                 EntityUtil entityUtil) {
        this.capabilityRepository = capabilityRepository;
        this.accountRepository = accountRepository;
        this.entityRepository = entityRepository;
        this.entityUtil = entityUtil;

        setDescription("Alter capabilities on entities and accounts.");
        addSubcommand("list", "List all capabilities.");
        addSubcommand("show", "Show the capabilities on something.",
                new Parameter("entity name", true));
        addSubcommand("add", "Add a role to something.",
                new Parameter("entity name", true),
                new Parameter("capability", true));
        addSubcommand("remove", "Remove a role from something.",
                new Parameter("entity name", true),
                new Parameter("capability", true));
    }

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String command, String[] tokens, String raw) {
        if (tokens.length > 0) {
            if ("list".equals(tokens[0])) {
                output.append("[yellow]All capabilities");
                capabilityRepository.findAll(SORT)
                        .forEach(capability -> output.append(String.format("[yellow]...%s\t(%s)\t(%s %s)",
                                capability.getDescription(),
                                capability.getName(),
                                capability.getObject(),
                                capability.getScope())));

                return output;
            }

            Entity target;
            Optional<Entity> entityOptional = entityUtil.entitySearchGlobal(entity, tokens[1]);

            if (!entityOptional.isPresent()) {
                output.append("[yellow]There is nothing by that name here.");

                return output;
            }

            target = entityOptional.get();

            if ("show".equals(tokens[0])) {
                if (tokens.length == 2) {
                    if (target.getAccount() != null) {
                        output.append("[yellow]Capabilities for account " + target.getAccount());
                        target.getAccount().getCapabilities().forEach(capability -> output.append(String.format("[yellow]...%s (%s)",
                                capability.getDescription(),
                                capability.getName())));
                        output.append("");
                    }

                    output.append("[yellow]Capabilities for entity " + target);
                    target.getCapabilities().forEach(capability -> output.append(String.format("[yellow]...%s (%s)",
                            capability.getDescription(),
                            capability.getName())));


                } else {
                    usage(output, command);

                    return output;
                }
            } else if ("add".equals(tokens[0])) {
                if (tokens.length == 3) {
                    Capability capability = capabilityRepository.findByNameIgnoreCase(tokens[2]);

                    if (capability == null) {
                        output.append("[yellow]No such capability exists.");

                        return output;
                    }

                    if (CapabilityObject.ENTITY == capability.getObject()) {
                        target.addCapabilities(capability);
                        entityRepository.save(target);
                    } else {
                        target.getAccount().addCapabilities(capability);
                        accountRepository.save(target.getAccount());
                    }

                    output.append("[yellow]Capability added.");

                    return output;
                } else {
                    usage(output, command);

                    return output;
                }
            } else if ("remove".equals(tokens[0])) {
                if (tokens.length == 3) {
                    Capability capability = capabilityRepository.findByNameIgnoreCase(tokens[2]);

                    if (capability == null) {
                        output.append("[yellow]No such capability exists.");

                        return output;
                    }

                    if (CapabilityObject.ENTITY == capability.getObject()) {
                        target.removeCapabilities(capability);
                        entityRepository.save(target);
                    } else {
                        target.getAccount().removeCapabilities(capability);
                        accountRepository.save(target.getAccount());
                    }

                    output.append("[yellow]Capability removed.");

                    return output;
                } else {
                    usage(output, command);

                    return output;
                }
            } else {
                usage(output, command);
            }

            return output;
        }

        usage(output, command);

        return output;
    }
}
