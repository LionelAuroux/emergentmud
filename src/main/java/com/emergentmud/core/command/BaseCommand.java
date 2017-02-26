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

package com.emergentmud.core.command;

import com.emergentmud.core.model.Entity;
import com.emergentmud.core.model.stomp.GameOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseCommand implements Command {
    private List<Subcommand> subCommands = new ArrayList<>();
    private List<Parameter> parameters = new ArrayList<>();

    @Override
    public abstract GameOutput execute(GameOutput output, Entity entity, String command, String[] tokens, String raw);

    @Override
    public GameOutput usage(GameOutput output, String command) {
        if (!subCommands.isEmpty()) {
            output.append(String.format("[yellow]Usage: %s", command.toUpperCase()));

            subCommands.forEach(sc -> {
                StringBuilder buf = new StringBuilder("[yellow]");

                buf.append(sc.name);
                buf.append(" ");

                sc.parameters.forEach(p -> {
                    if (p.isRequired()) {
                        buf.append("&lt;");
                    } else {
                        buf.append("[");
                    }

                    buf.append(p.getName());

                    if (p.isRequired()) {
                        buf.append("&gt; ");
                    } else {
                        buf.append("] ");
                    }
                });

                if (sc.parameters.isEmpty()) {
                    buf.append(" ");
                }

                buf.append("- ");
                buf.append(sc.description);

                output.append(buf.toString());
            });
        } else if (!parameters.isEmpty()) {
            StringBuilder buf = new StringBuilder("[yellow]Usage: ");

            buf.append(command.toUpperCase());
            buf.append(" ");

            parameters.forEach(p -> {
                if (p.isRequired()) {
                    buf.append("&lt;");
                } else {
                    buf.append("[");
                }

                buf.append(p.getName());

                if (p.isRequired()) {
                    buf.append("&gt; ");
                } else {
                    buf.append("] ");
                }
            });

            output.append(buf.toString().trim());
        } else {
            output.append("[yellow]Usage: " + command.toUpperCase());
        }

        return output;
    }

    protected void addParameter(String parameter, boolean isRequired) {
        parameters.add(new Parameter(parameter, isRequired));
    }

    protected void addSubcommand(String command, String description, Parameter... parameters) {
        subCommands.add(new Subcommand(command, description, Arrays.asList(parameters)));
    }

    private static class Subcommand {
        private String name;
        private String description;
        private List<Parameter> parameters = new ArrayList<>();

        Subcommand(String name, String description, List<Parameter> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
    }
}
