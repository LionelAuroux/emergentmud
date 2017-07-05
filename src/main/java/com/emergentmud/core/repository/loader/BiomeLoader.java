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

package com.emergentmud.core.repository.loader;

import com.emergentmud.core.model.room.Biome;
import com.emergentmud.core.repository.BiomeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class BiomeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeLoader.class);

    private BiomeRepository biomeRepository;

    @Inject
    public BiomeLoader(BiomeRepository biomeRepository) {
        this.biomeRepository = biomeRepository;
    }

    @PostConstruct
    public void loadWorld() {
        if (biomeRepository.count() == 0) {
            LOGGER.warn("No biomes found! Loading default biomes...");

            List<Biome> biomes = new ArrayList<>();

//            biomes.add(new Biome("Ocean", 0x444471));
//            biomes.add(new Biome("Lake", 0x336699));
//            biomes.add(new Biome("Beach", 0xa09077));
            biomes.add(new Biome("Snow", 0xffffff,
                    "The snow is melting into a gurgling stream here.",
                    "A stream disappears under a sheet of ice here.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Tundra", 0xbbbbaa,
                    "A small mountain stream gurgles up from the dirt.",
                    "A stream ends here, seeping into the ground.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Bare", 0x888888,
                    "A small mountain stream gurgles up from the dirt.",
                    "A stream ends here, seeping into the ground.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Scorched", 0x555555,
                    "A small stream bubbles up through the rocky ground.",
                    "A stream disappears underground beneath the rocks here.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Taiga", 0x99aa77,
                    "A stream disappears underground beneath the rocks here.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Shrubland", 0x889977,
                    "A stream disappears here, flowing through the rocks into some underground cavern.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Temperate Desert", 0xc9d29b,
                    "A stream disappears here, simply absorbed into the dirt.",
                    "A stream flows [outlets] from here."));
            biomes.add(new Biome("Temperate Rain Forest", 0x448855,
                    "A creek flows through here, vanishing into the thick foliage.",
                    "A creek flows [outlets] from here."));
            biomes.add(new Biome("Temperate Deciduous Forest", 0x679459,
                    "A creek flows through here, disappearing down into a cluster of rocks.",
                    "A creek flows [outlets] from here."));
            biomes.add(new Biome("Grassland", 0x88aa55,
                    "A creek ends here, fanning out across the flat, muddy grassland.",
                    "A creek flows [outlets] from here."));
            biomes.add(new Biome("Subtropical Desert", 0xd2b98b,
                    "A river ends here, simply absorbed into the thirsty desert sand.",
                    "A river flows [outlets] from here."));
//            biomes.add(new Biome("Ice", 0x99ffff));
//            biomes.add(new Biome("Marsh", 0x2f6666));
            biomes.add(new Biome("Tropical Rain Forest", 0x337755,
                    "A river flows down through some hidden underground tunnel here.",
                    "A river flows [outlets] from here."));
            biomes.add(new Biome("Tropical Seasonal Forest", 0x559944,
                    "A river ends here, flowing under a large rock pile.",
                    "A river flows [outlets] from here."));
//            biomes.add(new Biome("River", 0x225588));

            biomeRepository.save(biomes);
        }
    }
}
