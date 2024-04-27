package org.lwjgl.demo.opengl;

import org.joml.Vector2d;
import org.lwjgl.demo.util.Color4D;

class Hexagon implements ObjectToRender {

    private ObjectToRender neighborTop;
    private ObjectToRender neighborTopLeft;
    private ObjectToRender neighborBottomLeft;
    private ObjectToRender neighborBottom;
    public Hexagon() { }

    public Hexagon(ObjectToRender neighborTop,
                   ObjectToRender neighborTopLeft,
                   ObjectToRender neighborBottomLeft,
                   ObjectToRender neighborBottom) {
        this.neighborTop = neighborTop;
        this.neighborTopLeft = neighborTopLeft;
        this.neighborBottomLeft = neighborBottomLeft;
        this.neighborBottom = neighborBottom;
    }

    @Override
    public Color4D getColor() {
        return new Color4D(1, 0, 1, 0.4f);
    }

    @Override
    public float getFoldingAngle() {
        return 180 + 138.19f;
    }

    @Override
    public ObjectToRenderWithMatrixMoves[] getNeighbors() {
        var halfHeight = 0.8660254037844f;
        var height = halfHeight * 2;

        return new ObjectToRenderWithMatrixMoves[] {
            new ObjectToRenderWithMatrixMoves(neighborTop, new Vector2d(0, height), new Vector2d(0, halfHeight), new Vector2d(1, 0), false),
            new ObjectToRenderWithMatrixMoves(neighborBottom, new Vector2d(0, -height), new Vector2d(0, -halfHeight), new Vector2d(1, 0), true),
            new ObjectToRenderWithMatrixMoves(neighborTopLeft, new Vector2d(-1.5, halfHeight), new Vector2d(-1, 0), new Vector2d(0.5f, halfHeight), false),
            new ObjectToRenderWithMatrixMoves(neighborBottomLeft, new Vector2d(-1.5, -halfHeight), new Vector2d(-1, 0), new Vector2d(0.5f, -halfHeight), true)
        };
    }
}