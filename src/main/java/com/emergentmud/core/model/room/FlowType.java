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

public enum FlowType {
    SPRING("Clear, fresh water is gurgling out of the ground here."),
    SINK("Water is sinking into cracks in the ground."),
    STRAIGHT("A stream is flowing from [origin] to the [straight] here."),
    LEFT("A stream is flowing from [origin] to [left] here."),
    RIGHT("A stream is flowing from [origin] to the [right] here."),
    STRAIGHT_LEFT("The stream forks here, flowing [straight] and [left]."),
    STRAIGHT_RIGHT("The stream forks here, flowing [straight] and [right]."),
    LEFT_RIGHT("The stream splits here, flowing [left] and [right]."),
    STRAIGHT_LEFT_RIGHT("The stream flows from the [origin] and fans out in all directions.");

    private String description;

    FlowType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
