package org.lwjgl.demo.opengl;

import java.util.function.Function;

import org.joml.Matrix4x3d;
import org.lwjgl.demo.util.Color4D;

public interface ObjectToRender {

    Color4D getColor();
    float distanceToCenter();
    int getIndex();
    ObjectToRenderWithMatrixMoves[] getNeighbors();
    
    final record ObjectToRenderWithMatrixMoves(ObjectToRender object, Function<Matrix4x3d, Matrix4x3d> matrixMoves) {}
}

