package org.lwjgl.demo.opengl;

import org.lwjgl.demo.util.Color4D;

class Pentagon implements ObjectToRender {

    private static final float HALF_HEIGHT = 0.6881909602356f;

    private int index;

    private ObjectToRender neighborTopRight;
    private ObjectToRender neighborBottomRight;
    private ObjectToRender neighborBottom;
    private ObjectToRender neighborBottomLeft;
    private ObjectToRender neighborTopLeft;

    public Pentagon(int index) {
        this.index = index;
    }

    public Pentagon(int index,
                    ObjectToRender neighborTopRight,
                    ObjectToRender neighborBottomRight,
                    ObjectToRender neighborBottom,
                    ObjectToRender neighborBottomLeft,
                    ObjectToRender neighborTopLeft) {
        this.index = index;
        this.neighborTopRight = neighborTopRight;
        this.neighborBottomRight = neighborBottomRight;
        this.neighborBottom = neighborBottom;
        this.neighborBottomLeft = neighborBottomLeft;
        this.neighborTopLeft = neighborTopLeft;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Color4D getColor() {
        return new Color4D(1, 1, 0, 0.4f);
    }

    @Override
    public float distanceToCenter() {
        return HALF_HEIGHT;
    }

    @Override
    public ObjectToRenderWithMatrixMoves[] getNeighbors() {
        return new ObjectToRenderWithMatrixMoves[] {
            new ObjectToRenderWithMatrixMoves(neighborTopRight, matrix -> matrix.rotateZ(Math.toRadians(-36)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborBottomRight, matrix -> matrix.rotateZ(Math.toRadians(-108)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborBottom, matrix -> matrix.rotateZ(Math.toRadians(-180)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborBottomLeft, matrix -> matrix.rotateZ(Math.toRadians(-252)).translate(0, HALF_HEIGHT, 0)),
            new ObjectToRenderWithMatrixMoves(neighborTopLeft, matrix -> matrix.rotateZ(Math.toRadians(-324)).translate(0, HALF_HEIGHT, 0)),
        };
    }
}