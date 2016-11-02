/*
 * EmergentMUD - A modern MUD with a procedurally generated world.
 * Copyright (C) 2016 Peter Keeler
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
 *
 * Parts of this file are adapted from Connor Clark's map generation
 * implementation available here: https://github.com/Hoten/Java-Delaunay
 */

package com.emergentmud.core.repository.zonebuilder.polygonal;

import com.emergentmud.core.model.Biome;
import com.emergentmud.core.model.Room;
import com.emergentmud.core.model.Zone;
import com.emergentmud.core.repository.BiomeRepository;
import com.emergentmud.core.repository.RoomRepository;
import com.emergentmud.core.repository.ZoneBuilder;
import com.emergentmud.core.repository.ZoneRepository;
import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;
import com.hoten.delaunay.voronoi.Center;
import com.hoten.delaunay.voronoi.Corner;
import com.hoten.delaunay.voronoi.Edge;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.LineSegment;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.Voronoi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static com.emergentmud.core.config.WorldConfiguration.SEED;

@Component
public class PolygonalZoneBuilder implements ZoneBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolygonalZoneBuilder.class);
    private static final int EXTENT = 2000;
    private static final int SITES = 30000;

    private final List<Edge> edges = new ArrayList<>();
    private final List<Center> centers = new ArrayList<>();
    private final List<Corner> corners = new ArrayList<>();

    private Random random;
    private ZoneRepository zoneRepository;
    private BiomeRepository biomeRepository;
    private RoomRepository roomRepository;
    private BiomeSelector biomeSelector;
    private IslandShape islandShape;

    private Rectangle bounds;
    private BufferedImage pixelCenterMap;

    @Inject
    public PolygonalZoneBuilder(Random random,
                                ZoneRepository zoneRepository,
                                BiomeRepository biomeRepository,
                                RoomRepository roomRepository,
                                BiomeSelector biomeSelector,
                                IslandShape islandShape) {
        this.random = random;
        this.zoneRepository = zoneRepository;
        this.biomeRepository = biomeRepository;
        this.roomRepository = roomRepository;
        this.biomeSelector = biomeSelector;
        this.islandShape = islandShape;
    }

    @Override
    public Zone build(Long x, Long y, Long z) {
        edges.clear();
        centers.clear();
        corners.clear();

        Zone zone = new Zone();
        zone = zoneRepository.save(zone);

        LOGGER.info("Generating points...");
        Voronoi voronoi = new Voronoi(SITES, EXTENT, EXTENT, random, null);
        voronoi = relaxPoints(voronoi);

        bounds = voronoi.get_plotBounds();

        buildGraph(voronoi);
        improveCorners();

        assignCornerElevations();
        assignOceanCoastAndLand();
        redistributeElevations(landCorners());
        assignPolygonElevations();

        calculateDownslopes();
        createRivers();
        assignCornerMoisture();
        redistributeMoisture(landCorners());
        assignPolygonMoisture();
        assignBiomes();

        pixelCenterMap = new BufferedImage((int) bounds.width, (int) bounds.width, BufferedImage.TYPE_4BYTE_ABGR);

        BufferedImage map = createMap();

        int[] pixels = new int[map.getHeight() * map.getWidth()];
        pixels = map.getRGB(0, 0, map.getWidth(), map.getHeight(), pixels, 0, map.getWidth());

        LOGGER.info("Created bitmap: width = {}, height = {}", map.getWidth(), map.getHeight());

        try {
            File file = new File(String.format("maps/seed-%d-sites-%d-lloyds-%d.png", SEED, SITES, 1));

            if (file.mkdirs()) {
                ImageIO.write(map, "PNG", file);
            } else {
                LOGGER.error("Unable to make directory for map export!");
            }
        } catch (IOException ioe) {
            LOGGER.error("Unable to export map as PNG image: ", ioe);
        }

        List<Biome> allBiomes = biomeRepository.findAll();
        Map<Integer, Biome> biomesByColor = new HashMap<>();
        Biome oceanBiome = biomeRepository.findByName("Ocean");

        allBiomes.forEach(biome -> biomesByColor.put(biome.getColor(), biome));

        // create a new Room for each pixel
        for (long scanY = 0; scanY < map.getHeight(); scanY++) {
            List<Room> rooms = new ArrayList<>();

            for (long scanX = 0; scanX < map.getWidth(); scanX++) {
                Room room = new Room();

                room.setX(scanX);
                room.setY((map.getHeight() - 1) - scanY);
                room.setZ(0L);
                room.setZone(zone);

                int color = pixels[(int)(scanY * map.getWidth() + scanX)];
                color &= 0xFFFFFF;

                room.setBiome(biomesByColor.get(color));

                if (room.getBiome() == null) {
                    LOGGER.debug("Failed to set biome for room at ({}, {}, {}) using color: {}",
                            room.getX(), room.getY(), room.getZ(), Integer.toHexString(color));

                    room.setBiome(oceanBiome); // hide glitches around the edge of the map
                }

                rooms.add(room);
            }

            roomRepository.save(rooms);

            LOGGER.info("Saved {} rooms, row {} of {}.", rooms.size(), scanY + 1, map.getHeight());
        }

        return zone;
    }

    private Color getColor(Biome biome) {
        if (biome == null) {
            return Color.MAGENTA;
        }

        return new Color(biome.getColor());
    }

    private BufferedImage createMap() {
        final BufferedImage img = new BufferedImage((int)bounds.width, (int)bounds.height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = img.createGraphics();

        paint(graphics, true, true, false, false, false);

        return img;
    }

    //also records the area of each voronoi cell
    private void paint(Graphics2D g, boolean drawBiomes, boolean drawRivers, boolean drawSites, boolean drawCorners, boolean drawDelaunay) {
        Biome riverBiome = biomeRepository.findByName("River");
        Graphics2D pixelCenterGraphics = pixelCenterMap.createGraphics();

        //draw via triangles
        for (Center c : centers) {
            drawPolygon(g, c, drawBiomes ? getColor(c.biome) : new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            drawPolygon(pixelCenterGraphics, c, new Color(c.index));
        }

        for (Edge e : edges) {
            if (drawDelaunay) {
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.YELLOW);
                g.drawLine((int) e.d0.loc.x, (int) e.d0.loc.y, (int) e.d1.loc.x, (int) e.d1.loc.y);
            }
            if (drawRivers && e.river > 0) {
                g.setStroke(new BasicStroke(1 + (int) Math.sqrt(e.river * 2)));
                g.setColor(new Color(riverBiome.getColor()));
                g.drawLine((int) e.v0.loc.x, (int) e.v0.loc.y, (int) e.v1.loc.x, (int) e.v1.loc.y);
            }
        }

        if (drawSites) {
            g.setColor(Color.RED);
            centers.forEach((s) -> g.fillOval((int) (s.loc.x - 2), (int) (s.loc.y - 2), 4, 4));
        }

        if (drawCorners) {
            g.setColor(Color.WHITE);
            corners.forEach((c) -> g.fillOval((int) (c.loc.x - 2), (int) (c.loc.y - 2), 4, 4));
        }
//        g.setColor(Color.DARK_GRAY);
//        g.drawRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height);
    }

    private void drawPolygon(Graphics2D g, Center c, Color color) {
        g.setColor(color);

        //only used if Center c is on the edge of the graph. allows for completely filling in the outer polygons
        Corner edgeCorner1 = null;
        Corner edgeCorner2 = null;
        c.area = 0;
        for (Center n : c.neighbors) {
            Edge e = edgeWithCenters(c, n);

            if (e == null || e.v0 == null) {
                //outermost voronoi edges aren't stored in the graph
                continue;
            }

            //find a corner on the exterior of the graph
            //if this Edge e has one, then it must have two,
            //finding these two corners will give us the missing
            //triangle to render. this special triangle is handled
            //outside this for loop
            Corner cornerWithOneAdjacent = e.v0.border ? e.v0 : e.v1;
            if (cornerWithOneAdjacent.border) {
                if (edgeCorner1 == null) {
                    edgeCorner1 = cornerWithOneAdjacent;
                } else {
                    edgeCorner2 = cornerWithOneAdjacent;
                }
            }

            drawTriangle(g, e.v0, e.v1, c);
            c.area += Math.abs(c.loc.x * (e.v0.loc.y - e.v1.loc.y)
                    + e.v0.loc.x * (e.v1.loc.y - c.loc.y)
                    + e.v1.loc.x * (c.loc.y - e.v0.loc.y)) / 2;
        }

        //handle the missing triangle
        if (edgeCorner2 != null) {
            //if these two outer corners are NOT on the same exterior edge of the graph,
            //then we actually must render a polygon (w/ 4 points) and take into consideration
            //one of the four corners (either 0,0 or 0,height or width,0 or width,height)
            //note: the 'missing polygon' may have more than just 4 points. this
            //is common when the number of sites are quite low (less than 5), but not a problem
            //with a more useful number of sites.
            //TODO: find a way to fix this

            if (closeEnough(edgeCorner1.loc.x, edgeCorner2.loc.x, 1)) {
                drawTriangle(g, edgeCorner1, edgeCorner2, c);
            } else {
                int[] x = new int[4];
                int[] y = new int[4];
                x[0] = (int) c.loc.x;
                y[0] = (int) c.loc.y;
                x[1] = (int) edgeCorner1.loc.x;
                y[1] = (int) edgeCorner1.loc.y;

                //determine which corner this is
                x[2] = (int) ((closeEnough(edgeCorner1.loc.x, bounds.x, 1) || closeEnough(edgeCorner2.loc.x, bounds.x, .5)) ? bounds.x : bounds.right);
                y[2] = (int) ((closeEnough(edgeCorner1.loc.y, bounds.y, 1) || closeEnough(edgeCorner2.loc.y, bounds.y, .5)) ? bounds.y : bounds.bottom);

                x[3] = (int) edgeCorner2.loc.x;
                y[3] = (int) edgeCorner2.loc.y;

                g.fillPolygon(x, y, 4);
                c.area += 0; //TODO: area of polygon given vertices
            }
        }
    }

    private Edge edgeWithCenters(Center c1, Center c2) {
        for (Edge e : c1.borders) {
            if (e.d0 == c2 || e.d1 == c2) {
                return e;
            }
        }
        return null;
    }

    private void drawTriangle(Graphics2D g, Corner c1, Corner c2, Center center) {
        int[] x = new int[3];
        int[] y = new int[3];
        x[0] = (int) center.loc.x;
        y[0] = (int) center.loc.y;
        x[1] = (int) c1.loc.x;
        y[1] = (int) c1.loc.y;
        x[2] = (int) c2.loc.x;
        y[2] = (int) c2.loc.y;
        g.fillPolygon(x, y, 3);
    }

    private boolean closeEnough(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) <= diff;
    }

    /**
     * Lloyd's Relaxation. The random number generator tends to make clumps of points
     * and this will smooth them out so they're more evenly distributed.
     *
     * https://en.wikipedia.org/wiki/Lloyd%27s_algorithm
     */
    private Voronoi relaxPoints(Voronoi voronoi) {
        LOGGER.info("Relaxing points...");
        List<Point> points = voronoi.siteCoords();

        points.forEach(p -> {
            List<Point> region = voronoi.region(p);
            double x = 0;
            double y = 0;

            for (Point c : region) {
                x += c.x;
                y += c.y;
            }

            x /= region.size();
            y /= region.size();
            p.x = x;
            p.y = y;
        });

        return new Voronoi(points, null, voronoi.get_plotBounds());
    }

    private List<Corner> landCorners() {
        return corners.stream().filter(c -> !c.ocean && !c.coast).collect(Collectors.toList());
    }

    private void redistributeElevations(List<Corner> landCorners) {
        LOGGER.info("Redistributing elevations...");
        Collections.sort(landCorners, (o1, o2) -> {
            if (o1.elevation > o2.elevation) {
                return 1;
            } else if (o1.elevation < o2.elevation) {
                return -1;
            }
            return 0;
        });

        final double SCALE_FACTOR = 1.1;
        for (int i = 0; i < landCorners.size(); i++) {
            double y = (double) i / landCorners.size();
            double x = Math.sqrt(SCALE_FACTOR) - Math.sqrt(SCALE_FACTOR * (1 - y));
            x = Math.min(x, 1);
            landCorners.get(i).elevation = x;
        }

        corners.stream().filter(c -> c.ocean || c.coast).forEach(c -> c.elevation = 0.0);
    }

    private void assignOceanCoastAndLand() {
        LOGGER.info("Assigning ocean and land...");
        Deque<Center> queue = new ArrayDeque<>();
        final double waterThreshold = .3;
        for (final Center center : centers) {
            int numWater = 0;
            for (final Corner c : center.corners) {
                if (c.border) {
                    center.border = center.water = center.ocean = true;
                    queue.add(center);
                }
                if (c.water) {
                    numWater++;
                }
            }
            center.water = center.ocean || ((double) numWater / center.corners.size() >= waterThreshold);
        }
        while (!queue.isEmpty()) {
            final Center center = queue.pop();
            center.neighbors.stream().filter(n -> n.water && !n.ocean).forEach(n -> {
                n.ocean = true;
                queue.add(n);
            });
        }
        for (Center center : centers) {
            boolean oceanNeighbor = false;
            boolean landNeighbor = false;
            for (Center n : center.neighbors) {
                oceanNeighbor |= n.ocean;
                landNeighbor |= !n.water;
            }
            center.coast = oceanNeighbor && landNeighbor;
        }

        for (Corner c : corners) {
            int numOcean = 0;
            int numLand = 0;
            for (Center center : c.touches) {
                numOcean += center.ocean ? 1 : 0;
                numLand += !center.water ? 1 : 0;
            }
            c.ocean = numOcean == c.touches.size();
            c.coast = numOcean > 0 && numLand > 0;
            c.water = c.border || ((numLand != c.touches.size()) && !c.coast);
        }
    }

    private void assignCornerElevations() {
        LOGGER.info("Assigning corner elevations...");
        Deque<Corner> queue = new ArrayDeque<>();
        for (Corner c : corners) {
            c.water = islandShape.isWater(bounds, c.loc);
            if (c.border) {
                c.elevation = 0;
                queue.add(c);
            } else {
                c.elevation = Double.MAX_VALUE;
            }
        }

        while (!queue.isEmpty()) {
            Corner c = queue.pop();
            for (Corner a : c.adjacent) {
                double newElevation = 0.01 + c.elevation;
                if (!c.water && !a.water) {
                    newElevation += 1;
                }
                if (newElevation < a.elevation) {
                    a.elevation = newElevation;
                    queue.add(a);
                }
            }
        }
    }

    private void improveCorners() {
        LOGGER.info("Improving graph corners...");
        Point[] newP = new Point[corners.size()];
        for (Corner c : corners) {
            if (c.border) {
                newP[c.index] = c.loc;
            } else {
                double x = 0;
                double y = 0;
                for (Center center : c.touches) {
                    x += center.loc.x;
                    y += center.loc.y;
                }
                newP[c.index] = new Point(x / c.touches.size(), y / c.touches.size());
            }
        }
        corners.forEach((c) -> c.loc = newP[c.index]);
        edges.stream().filter((e) -> (e.v0 != null && e.v1 != null)).forEach((e) -> e.setVornoi(e.v0, e.v1));
    }

    private void buildGraph(Voronoi v) {
        LOGGER.info("Building graph...");
        final HashMap<Point, Center> pointCenterMap = new HashMap<>();
        final List<Point> points = v.siteCoords();
        points.forEach((p) -> {
            Center c = new Center();
            c.loc = p;
            c.index = centers.size();
            centers.add(c);
            pointCenterMap.put(p, c);
        });

        //bug fix
        centers.forEach(c -> v.region(c.loc));

        final List<com.hoten.delaunay.voronoi.nodename.as3delaunay.Edge> libedges = v.edges();
        final HashMap<Integer, Corner> pointCornerMap = new HashMap<>();

        for (com.hoten.delaunay.voronoi.nodename.as3delaunay.Edge libedge : libedges) {
            final LineSegment vEdge = libedge.voronoiEdge();
            final LineSegment dEdge = libedge.delaunayLine();

            final Edge edge = new Edge();
            edge.index = edges.size();
            edges.add(edge);

            edge.v0 = makeCorner(pointCornerMap, vEdge.p0);
            edge.v1 = makeCorner(pointCornerMap, vEdge.p1);
            edge.d0 = pointCenterMap.get(dEdge.p0);
            edge.d1 = pointCenterMap.get(dEdge.p1);

            // Centers point to edges. Corners point to edges.
            if (edge.d0 != null) {
                edge.d0.borders.add(edge);
            }
            if (edge.d1 != null) {
                edge.d1.borders.add(edge);
            }
            if (edge.v0 != null) {
                edge.v0.protrudes.add(edge);
            }
            if (edge.v1 != null) {
                edge.v1.protrudes.add(edge);
            }

            // Centers point to centers.
            if (edge.d0 != null && edge.d1 != null) {
                addToCenterList(edge.d0.neighbors, edge.d1);
                addToCenterList(edge.d1.neighbors, edge.d0);
            }

            // Corners point to corners
            if (edge.v0 != null && edge.v1 != null) {
                addToCornerList(edge.v0.adjacent, edge.v1);
                addToCornerList(edge.v1.adjacent, edge.v0);
            }

            // Centers point to corners
            if (edge.d0 != null) {
                addToCornerList(edge.d0.corners, edge.v0);
                addToCornerList(edge.d0.corners, edge.v1);
            }
            if (edge.d1 != null) {
                addToCornerList(edge.d1.corners, edge.v0);
                addToCornerList(edge.d1.corners, edge.v1);
            }

            // Corners point to centers
            if (edge.v0 != null) {
                addToCenterList(edge.v0.touches, edge.d0);
                addToCenterList(edge.v0.touches, edge.d1);
            }
            if (edge.v1 != null) {
                addToCenterList(edge.v1.touches, edge.d0);
                addToCenterList(edge.v1.touches, edge.d1);
            }
        }
    }

    //ensures that each corner is represented by only one corner object
    private Corner makeCorner(HashMap<Integer, Corner> pointCornerMap, Point p) {
        if (p == null) {
            return null;
        }
        int index = (int) ((int) p.x + (int) (p.y) * bounds.width * 2);
        Corner c = pointCornerMap.get(index);
        if (c == null) {
            c = new Corner();
            c.loc = p;
            c.border = bounds.liesOnAxes(p);
            c.index = corners.size();
            corners.add(c);
            pointCornerMap.put(index, c);
        }
        return c;
    }

    // Helper functions for the following for loop; ideally these would be inlined
    private void addToCornerList(List<Corner> list, Corner c) {
        if (c != null && !list.contains(c)) {
            list.add(c);
        }
    }

    private void addToCenterList(List<Center> list, Center c) {
        if (c != null && !list.contains(c)) {
            list.add(c);
        }
    }

    private void assignPolygonElevations() {
        LOGGER.info("Assigning elevations to polygons...");
        for (Center center : centers) {
            double total = 0;
            for (Corner c : center.corners) {
                total += c.elevation;
            }
            center.elevation = total / center.corners.size();
        }
    }

    private void calculateDownslopes() {
        LOGGER.info("Calculating slopes...");
        for (Corner c : corners) {
            Corner down = c;
            //System.out.println("ME: " + c.elevation);
            for (Corner a : c.adjacent) {
                //System.out.println(a.elevation);
                if (a.elevation <= down.elevation) {
                    down = a;
                }
            }
            c.downslope = down;
        }
    }

    private void createRivers() {
        LOGGER.info("Creating rivers...");
        for (int i = 0; i < bounds.width / 2; i++) {
            Corner c = corners.get(random.nextInt(corners.size()));
            if (c.ocean || c.elevation < 0.3 || c.elevation > 0.9) {
                continue;
            }
            // Bias rivers to go west: if (q.downslope.x > q.x) continue;
            while (!c.coast) {
                if (c == c.downslope) {
                    break;
                }
                Edge edge = lookupEdgeFromCorner(c, c.downslope);
                if (edge != null && (!edge.v0.water || !edge.v1.water)) {
                    edge.river++;
                    c.river++;
                    c.downslope.river++;  // TODO: fix double count
                }
                c = c.downslope;
            }
        }
    }

    private Edge lookupEdgeFromCorner(Corner c, Corner downslope) {
        for (Edge e : c.protrudes) {
            if (e.v0 == downslope || e.v1 == downslope) {
                return e;
            }
        }
        return null;
    }

    private void assignCornerMoisture() {
        LOGGER.info("Assigning corner moisture...");
        Deque<Corner> queue = new ArrayDeque<>();
        for (Corner c : corners) {
            if ((c.water || c.river > 0) && !c.ocean) {
                c.moisture = c.river > 0 ? Math.min(3.0, (0.2 * c.river)) : 1.0;
                queue.push(c);
            } else {
                c.moisture = 0.0;
            }
        }

        while (!queue.isEmpty()) {
            Corner c = queue.pop();
            for (Corner a : c.adjacent) {
                double newM = .9 * c.moisture;
                if (newM > a.moisture) {
                    a.moisture = newM;
                    queue.add(a);
                }
            }
        }

        // Salt water
        corners.stream().filter(c -> c.ocean || c.coast).forEach(c -> c.moisture = 1.0);
    }

    private void redistributeMoisture(List<Corner> landCorners) {
        LOGGER.info("Redistributing moisture...");
        Collections.sort(landCorners, (o1, o2) -> {
            if (o1.moisture > o2.moisture) {
                return 1;
            } else if (o1.moisture < o2.moisture) {
                return -1;
            }
            return 0;
        });
        for (int i = 0; i < landCorners.size(); i++) {
            landCorners.get(i).moisture = (double) i / landCorners.size();
        }
    }

    private void assignPolygonMoisture() {
        LOGGER.info("Assigning moisture...");
        for (Center center : centers) {
            double total = 0;
            for (Corner c : center.corners) {
                total += c.moisture;
            }
            center.moisture = total / center.corners.size();
        }
    }


    private void assignBiomes() {
        LOGGER.info("Assigning biomes...");
        for (Center center : centers) {
            center.biome = biomeSelector.getBiome(center);
        }
    }
}
