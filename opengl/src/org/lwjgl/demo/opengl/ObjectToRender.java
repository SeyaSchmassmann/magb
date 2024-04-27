package org.lwjgl.demo.opengl;

import org.joml.Vector2d;
import org.lwjgl.demo.util.Color4D;

public interface ObjectToRender {
    float getFoldingAngle();
    Color4D getColor();
    ObjectToRenderWithMatrixMoves[] getNeighbors();
    
    final record ObjectToRenderWithMatrixMoves(ObjectToRender object, Vector2d matrixMoves, Vector2d rotationTranslation, Vector2d rotationAxis, boolean negativeAngle) {}
}

