package squidpony.squidgrid.util;

import java.awt.Point;
import java.util.Random;
import squidpony.squidmath.RNG;

/**
 * Basic radius strategy implementations.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public enum BasicRadiusStrategy implements RadiusStrategy3D {

    /**
     * In an unobstructed area the FOV would be a square.
     *
     * This is the shape that would represent movement radius in an 8-way movement scheme with no additional cost for
     * diagonal movement.
     */
    SQUARE,
    /**
     * In an unobstructed area the FOV would be a diamond.
     *
     * This is the shape that would represent movement radius in a 4-way movement scheme.
     */
    DIAMOND,
    /**
     * In an unobstructed area the FOV would be a circle.
     *
     * This is the shape that would represent movement radius in an 8-way movement scheme with all movement cost the
     * same based on distance from the source
     */
    CIRCLE,
    /**
     * In an unobstructed area the FOV would be a cube.
     *
     * This is the shape that would represent movement radius in an 8-way movement scheme with no additional cost for
     * diagonal movement.
     */
    CUBE,
    /**
     * In an unobstructed area the FOV would be a octahedron.
     *
     * This is the shape that would represent movement radius in a 4-way movement scheme.
     */
    OCTAHEDRON,
    /**
     * In an unobstructed area the FOV would be a sphere.
     *
     * This is the shape that would represent movement radius in an 8-way movement scheme with all movement cost the
     * same based on distance from the source
     */
    SPHERE;

    private static RNG rng = null;//lazy instantiation

    @Override
    public float radius(int startx, int starty, int startz, int endx, int endy, int endz) {
        return radius((float) startx, (float) starty, (float) startz, (float) endx, (float) endy, (float) endz);
    }

    @Override
    public float radius(float startx, float starty, float startz, float endx, float endy, float endz) {
        float dx = Math.abs(startx - endx);
        float dy = Math.abs(starty - endy);
        float dz = Math.abs(startz - endz);
        return radius(dx, dy, dz);
    }

    @Override
    public float radius(int dx, int dy, int dz) {
        return radius((float) dx, (float) dy, (float) dz);
    }

    @Override
    public float radius(float dx, float dy, float dz) {
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        dz = Math.abs(dz);
        float radius = 0f;
        switch (this) {
            case SQUARE:
            case CUBE:
                radius = Math.max(dx, Math.max(dy, dz));//radius is longest axial distance
                break;
            case DIAMOND:
            case OCTAHEDRON:
                radius = dx + dy + dz;//radius is the manhattan distance
                break;
            case CIRCLE:
            case SPHERE:
                radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);//standard spherical radius
        }
        return radius;
    }

    @Override
    public float radius(int startx, int starty, int endx, int endy) {
        return radius((float) startx, (float) starty, (float) endx, (float) endy);
    }

    @Override
    public float radius(float startx, float starty, float endx, float endy) {
        float dx = Math.abs(startx - endx);
        float dy = Math.abs(starty - endy);
        return radius(dx, dy);
    }

    @Override
    public float radius(int dx, int dy) {
        return radius((float) dx, (float) dy);
    }

    @Override
    public float radius(float dx, float dy) {
        return radius(dx, dy, 0f);
    }

    @Override
    public double minZ(double distance) {
        double radius = 0;
        switch (this) {
            case CUBE:
            case SPHERE:
            case OCTAHEDRON:
                radius = -distance;
                break;
        }
        return radius;
    }

    @Override
    public double maxZ(double distance) {
        double radius = 0;
        switch (this) {
            case CUBE:
            case SPHERE:
            case OCTAHEDRON:
                radius = distance;
                break;
        }
        return radius;
    }

    @Override
    public double minX(double distance) {
        return -distance;
    }

    @Override
    public double maxX(double distance) {
        return distance;
    }

    @Override
    public double minY(double distance) {
        return -distance;
    }

    @Override
    public double maxY(double distance) {
        return distance;
    }

    @Override
    public Point onUnitShape(double distance) {
        if (rng == null) {
            rng = new RNG();
        }
        return onUnitShape(distance, rng);
    }

    @Override
    public Point onUnitShape(double distance, RNG random) {
        int x = 0, y = 0;
        switch (this) {
            case SQUARE:
            case CUBE:
                x = random.between((int) -distance, (int) distance + 1);
                y = random.between((int) -distance, (int) distance + 1);
                break;
            case DIAMOND:
            case OCTAHEDRON:
                x = random.between((int) -distance, (int) distance + 1);
                y = random.between((int) -distance, (int) distance + 1);
                if (radius(x, y) > distance) {
                    if (x > 0) {
                        if (y > 0) {
                            x = (int) (distance - x);
                            y = (int) (distance - y);
                        } else {
                            x = (int) (distance - x);
                            y = (int) (-distance - y);
                        }
                    } else {
                        if (y > 0) {
                            x = (int) (-distance - x);
                            y = (int) (distance - y);
                        } else {
                            x = (int) (-distance - x);
                            y = (int) (-distance - y);
                        }
                    }
                }
                break;
            case CIRCLE:
            case SPHERE:
                double radius = distance * Math.sqrt(random.between(0.0, 1.0));
                double theta = random.between(0, 2 * Math.PI);
                x = (int) (Math.cos(theta) * radius);
                y = (int) (Math.sin(theta) * radius);
        }

        return new Point(x, y);
    }
}