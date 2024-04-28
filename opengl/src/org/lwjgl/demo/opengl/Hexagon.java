package org.lwjgl.demo.opengl;

import org.lwjgl.demo.util.Color4D;

class Hexagon implements ObjectToRender {

    private static final float HALF_HEIGHT = 0.8660254037844f;
    
    private int index;

    private ObjectToRender neighborTop;
    private ObjectToRender neighborTopRight;
    private ObjectToRender neighborBottomRight;
    private ObjectToRender neighborBottom;
    private ObjectToRender neighborBottomLeft;
    private ObjectToRender neighborTopLeft;
    public Hexagon(int index) {
        this.index = index;
    }

    public Hexagon(int index,
                   ObjectToRender neighborTop,
                   ObjectToRender neighborTopRight,
                   ObjectToRender neighborBottomRight,
                   ObjectToRender neighborBottom,
                   ObjectToRender neighborBottomLeft,
                   ObjectToRender neighborTopLeft) {
        this.index = index;          
        this.neighborTop = neighborTop;
        this.neighborTopRight = neighborTopRight;
        this.neighborBottomRight = neighborBottomRight;
        this.neighborBottom = neighborBottom;
        this.neighborBottomLeft = neighborBottomLeft;
        this.neighborTopLeft = neighborTopLeft;
    }

    @Override
    public Color4D getColor() {
        return new Color4D(0, 1, 1, 0.4f);
    }

    public int getIndex() {
        return index;
    }

    @Override
    public float distanceToCenter() {
        return HALF_HEIGHT;
    }

    @Override
    public ObjectToRenderWithMatrixMoves[] getNeighbors() {
        return new ObjectToRenderWithMatrixMoves[] {
            new ObjectToRenderWithMatrixMoves(neighborTop, matrix -> matrix.rotateZ(Math.toRadians(-0)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborTopRight, matrix -> matrix.rotateZ(Math.toRadians(-60)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborBottomRight, matrix -> matrix.rotateZ(Math.toRadians(-120)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborBottom, matrix -> matrix.rotateZ(Math.toRadians(-180)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborBottomLeft, matrix -> matrix.rotateZ(Math.toRadians(-240)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborTopLeft, matrix -> matrix.rotateZ(Math.toRadians(-300)).translate(0, HALF_HEIGHT, 0)),
        };
    }
}