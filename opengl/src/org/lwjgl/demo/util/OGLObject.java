package org.lwjgl.demo.util;

import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

abstract public class OGLObject {
    protected final int m_POSITION_VBO;
    protected final int m_NORMAL_VBO;
    protected final FloatBuffer m_color;
    protected FloatBuffer m_positions;
    protected FloatBuffer m_normals;
    protected int m_vertexCount;

    protected OGLObject(Color4D color) {
        m_color = BufferUtils.createFloatBuffer(4);
        m_color.put(color.toArray()).flip();

        m_POSITION_VBO = glGenBuffers(); 	// generate one buffer object name
        m_NORMAL_VBO = glGenBuffers();		// generate one buffer object name
    }
    
    public int getVertexCount() { 
    	return m_vertexCount; 
    }
    
    public FloatBuffer getColor() { 
    	return m_color; 
    }

    public void setupPositions(int positionsID) {
        glBindBuffer(GL_ARRAY_BUFFER, m_POSITION_VBO);
        glEnableVertexAttribArray(positionsID); // enable vertex attribute array positionsID
        glVertexAttribPointer(positionsID, 3, GL_FLOAT, false, 0, 0);
    }
    
    public void setupNormals(int normalsID) {
        glBindBuffer(GL_ARRAY_BUFFER, m_NORMAL_VBO);
        glEnableVertexAttribArray(normalsID); // enable vertex attribute array normalsID
        glVertexAttribPointer(normalsID, 3, GL_FLOAT, false, 0, 0);
    }

    protected void allocatePositionBuffer(int size) {
        m_positions = memAllocFloat(size);
    }
    
    protected void allocateNormalBuffer(int size) {
        m_normals = memAllocFloat(size);
    }
    
    protected void bindPositionBuffer() {
        m_positions.limit(m_vertexCount*3);

        glBindBuffer(GL_ARRAY_BUFFER, m_POSITION_VBO);
        glBufferData(GL_ARRAY_BUFFER, m_positions, GL_STATIC_DRAW);

        memFree(m_positions);

        m_positions = null;
    }

    protected void bindNormalBuffer() {
        m_normals.limit(m_vertexCount*3);

        glBindBuffer(GL_ARRAY_BUFFER, m_NORMAL_VBO);
        glBufferData(GL_ARRAY_BUFFER, m_normals, GL_STATIC_DRAW);

        memFree(m_normals);

        m_normals = null;
    }
}
