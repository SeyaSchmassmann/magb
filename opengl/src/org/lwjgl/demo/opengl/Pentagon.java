package org.lwjgl.demo.opengl;

import org.lwjgl.demo.util.Color4D;

class Pentagon implements ObjectToRender {

    @Override
    public Color4D getColor() {
        return new Color4D(1, 1, 0, 0.4f);
    }

    @Override
    public float getFoldingAngle() {
        return 180 + 142.62f;
    }

    @Override
    public ObjectToRenderWithMatrixMoves[] getNeighbors() {
        return new ObjectToRenderWithMatrixMoves[0];
    }
}