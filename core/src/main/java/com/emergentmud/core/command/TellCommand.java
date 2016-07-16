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
import com.emergentmud.core.model.stomp.GameOutput;
import com.emergentmud.core.repository.EntityRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.util.Collections;

@Component
public class TellCommand extends BaseCommunicationCommand implements Command {
    @Inject
    public TellCommand(SimpMessagingTemplate simpMessagingTemplate,
                       EntityRepository entityRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.entityRepository = entityRepository;
    }

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String[] tokens, String raw) {
        if (StringUtils.isEmpty(raw) || tokens.length < 2) {
            output.append("Usage: TELL &lt;target&gt; &lt;message&gt;");

            return output;
        }

        String targetName = tokens[0];
        String message = raw.substring(tokens[0].length() + 1);
        Entity target = entityRepository.findByNameStartingWithIgnoreCase(targetName);

        if (target == null) {
            output.append("You don't know of anyone by that name.");

            return output;
        }

        if (target.equals(entity)) {
            output.append("You murmur quietly to yourself.");

            return output;
        }

        output.append(String.format("[red]You tell %s '%s[red]'", target.getName(), htmlEscape(message)));

        GameOutput toTarget = new GameOutput(String.format("[red]%s tells you '%s[red]'", entity.getName(), htmlEscape(message)))
                .append("")
                .append("> ");

        sendMessageToListeners(Collections.singletonList(target), entity, toTarget);

        return output;
    }
}