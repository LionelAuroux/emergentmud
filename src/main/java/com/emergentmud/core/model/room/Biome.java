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

package com.emergentmud.core.model.room;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Biome {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private Integer color;

    private String springText = null;
    private String sinkText = null;
    private String flowText = null;

    public Biome() {
    }

    public Biome(String name, Integer color, String sinkText, String flowText) {
        this.name = name;
        this.color = color;
        this.sinkText = sinkText;
        this.flowText = flowText;
    }

    public Biome(String name, Integer color, String springText, String sinkText, String flowText) {
        this.name = name;
        this.color = color;
        this.springText = springText;
        this.sinkText = sinkText;
        this.flowText = flowText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getColor() {
        return color;
    }

    public String getSpringText() {
        return springText;
    }

    public String getSinkText() {
        return sinkText;
    }

    public String getFlowText() {
        return flowText;
    }
}
