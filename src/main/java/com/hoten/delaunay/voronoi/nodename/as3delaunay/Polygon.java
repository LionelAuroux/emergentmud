/*
 * Java implementation by Connor Clark (www.hotengames.com). Pretty much a 1:1
 * translation of a wonderful map generating algorthim by Amit Patel of Red Blob Games,
 * which can be found here (http://www-cs-students.stanford.edu/~amitp/game-programming/polygon-map-generation/)
 * Hopefully it's of use to someone out there who needed it in Java like I did!
 * Note, the only island mode implemented is Radial. Implementing more is something for another day.
 *
 * FORTUNE'S ALGORTIHIM
 *
 * This is a java implementation of an AS3 (Flash) implementation of an algorthim
 * originally created in C++. Pretty much a 1:1 translation from as3 to java, save
 * for some necessary workarounds. Original as3 implementation by Alan Shaw (of nodename)
 * can be found here (https://github.com/nodename/as3delaunay). Original algorthim
 * by Steven Fortune (see lisence for c++ implementation below)
 *
 * The author of this software is Steven Fortune.  Copyright (c) 1994 by AT&T
 * Bell Laboratories.
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHORS NOR AT&T MAKE ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 */
package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.Point;
import java.util.ArrayList;
import java.util.List;

public final class Polygon {

    private List<Point> _vertices;

    public Polygon(List<Point> vertices) {
        _vertices = vertices;
    }

    public double area() {
        return Math.abs(signedDoubleArea() * 0.5);
    }

    public Winding winding() {
        double signedDoubleArea = signedDoubleArea();
        if (signedDoubleArea < 0) {
            return Winding.CLOCKWISE;
        }
        if (signedDoubleArea > 0) {
            return Winding.COUNTERCLOCKWISE;
        }
        return Winding.NONE;
    }

    private double signedDoubleArea() {
        int index, nextIndex;
        int n = _vertices.size();
        Point point, next;
        double signedDoubleArea = 0;
        for (index = 0; index < n; ++index) {
            nextIndex = (index + 1) % n;
            point = _vertices.get(index);
            next = _vertices.get(nextIndex);
            signedDoubleArea += point.x * next.y - next.x * point.y;
        }
        return signedDoubleArea;
    }
}