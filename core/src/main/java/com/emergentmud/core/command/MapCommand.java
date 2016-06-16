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
import org.springframework.stereotype.Component;

@Component
public class MapCommand implements Command {
    private static final int MAP_EXTENT_X = 40;
    private static final int MAP_EXTENT_Y = 20;

    @Override
    public GameOutput execute(GameOutput output, Entity entity, String[] tokens, String raw) {
        for (long y = entity.getY() + MAP_EXTENT_Y, i = 0; y >= entity.getY() - MAP_EXTENT_Y; y--, i++) {
            StringBuilder line = new StringBuilder();

            for (long x = entity.getX() - MAP_EXTENT_X; x <= entity.getX() + MAP_EXTENT_X; x++) {
                if (x == entity.getX() && y == entity.getY()) {
                    line.append("[cyan][]</span>");
                } else {
                    line.append(String.format("<span style='color: #%02x%02x%02x'>[]</span>", 256, 256, 256));
                }
            }

            if (i % 10 == 0) {
                line.append(String.format("  [yellow]%d", y));
            }

            output.append(line.toString());
        }

        StringBuilder line = new StringBuilder("[yellow]");
        int offset = 0;

        for (long x = entity.getX() - MAP_EXTENT_X, i = 0; x <= entity.getX() + MAP_EXTENT_X; x++, i++) {
            if (i % 10 == 0) {
                line.append(x + offset);

                int length = Long.toString(x).length();

                if (length == 1) {
                    line.append("&nbsp;");
                } else {
                    offset += Math.max(0, length - 2);
                }
            } else {
                if (offset >= 2) {
                    offset -= 2;
                } else if (offset == 1) {
                    offset--;
                    line.append("&nbsp;");
                } else {
                    line.append("&nbsp;&nbsp;");
                }
            }
        }

        output.append(line.toString());

        return output;
    }
}
