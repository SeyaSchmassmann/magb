package org.lwjgl.demo.opengl;

import org.joml.Vector2d;

class Pentagon implements ObjectToRender {

    private Vector2d[] vertices;

    public Pentagon(boolean topAttached) {

        if (topAttached) {
            vertices = new Vector2d[] {
                new Vector2d(0, 0.6728163648032f),
                new Vector2d(-0.8090169943749f, 0.0850311125107f),
                new Vector2d(-0.5f, -0.8660254037844f),
                new Vector2d(0.5f, -0.8660254037844f),
                new Vector2d(0.8090169943749f, 0.0850311125107f),
            };
        } else {
            vertices = new Vector2d[] {
                new Vector2d(0, -0.6728163648032f),
                new Vector2d(0.8090169943749f, -0.0850311125107f), // (0.8090169943749, 0.0850311125107)
                new Vector2d(0.5f, 0.8660254037844f),
                new Vector2d(-0.5f, 0.8660254037844f),
                new Vector2d(-0.8090169943749f, -0.0850311125107f),
            };
        }
    }

    @Override
    public ObjectToRenderWithMatrixMoves[] getNeighbors() {
        return new ObjectToRenderWithMatrixMoves[0];
    }

    @Override
    public Vector2d[] getVertices() {
        return vertices;
    }
}