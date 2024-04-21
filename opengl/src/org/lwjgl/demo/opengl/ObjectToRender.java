package org.lwjgl.demo.opengl;

import org.joml.Vector2d;

public interface ObjectToRender {    
    ObjectToRenderWithMatrixMoves[] getNeighbors();
    Vector2d[] getVertices();
    
    final record ObjectToRenderWithMatrixMoves(ObjectToRender object, Vector2d matrixMoves, Vector2d rotationTranslation, Vector2d rotationAxis, boolean negativeAngle) {}
}

