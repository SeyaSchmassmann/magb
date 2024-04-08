package org.lwjgl.demo.opengl;

import static org.joml.Math.PI;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL20C.glUniform3fv;
import static org.lwjgl.opengl.GL20C.glUniform4fv;
import static org.lwjgl.opengl.GL20C.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;

import java.nio.FloatBuffer;

import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.demo.util.Color4D;
import org.lwjgl.demo.util.OGLApp;
import org.lwjgl.demo.util.OGLModel3D;
import org.lwjgl.demo.util.OGLObject;

public class Cube3D extends OGLApp<CubeModel> {
	public Cube3D(CubeModel model) {
		super(model);
		
		m_keyCallback = (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			else if (action == GLFW_PRESS || action == GLFW_REPEAT) {
				switch(key) {
				case GLFW_KEY_LEFT: model.changeYangle(0.125); break;
				case GLFW_KEY_RIGHT: model.changeYangle(-0.125); break;
				case GLFW_KEY_UP: model.changeXangle(0.125); break;
				case GLFW_KEY_DOWN: model.changeXangle(-0.125); break;
				}
			}
		};
	}
	
	public static void main(String[] args) {
		new Cube3D(new CubeModel()).run("Cube", 640, 640, new Color4D(0.7f, 0.7f, 0.7f, 1));
	}
}

class CubeModel extends OGLModel3D {
	final static double deg2rad = PI/180;

	private final Matrix3d m_vm = new Matrix3d();
	private final Vector3d m_light  = new Vector3d();
	private final FloatBuffer m_vec3f = BufferUtils.createFloatBuffer(3);
	private final FloatBuffer m_mat3f = BufferUtils.createFloatBuffer(3*3);
	private final FloatBuffer m_mat4f = BufferUtils.createFloatBuffer(4*4);

	private Side m_side;
    private double m_startTime = System.currentTimeMillis()/1000.0;
    private double m_distance = 10.0f;	// camera distance
    private double m_dxAngle = 0;		// degrees
    private double m_dyAngle = 0; 		// degrees
    private double m_xAngle = 0;		// degrees
    private double m_yAngle = 0;		// degrees
    private double m_zAngle = 0;		// degrees
    private long   m_count;				// fps

	@Override
	public void init(int width, int height) {
		super.init(width, height);
		m_side = new Side(new Color4D(0, 0, 0, 1));
	}

	@Override
	public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // VIEW
        V.translation(0.0, 0.0, -m_distance).rotateX(m_xAngle*deg2rad).rotateY(m_yAngle*deg2rad).rotateZ(m_zAngle*deg2rad); // V = T*Rx*Ry*Rz

        // LIGHT
        glUniform3fv(u_LIGHT, m_light.set(0.0, 0.0, 10.0).normalize().get(m_vec3f)); // V*m_light

        // front
        M.translation(0, 0, 1); // translation = identity.translate
		drawSide(m_side.setRGBA(1, 0, 0, 1));

		// right
		M.rotationY(Math.PI/2).translate(0, 0, 1); // M = Ry*T
	    drawSide(m_side.setRGBA(1, 1, 0, 1));

		// back
		M.rotationY(Math.PI).translate(0, 0, 1);
        drawSide(m_side.setRGBA(0, 1, 1, 1));

		// left
	    M.rotationY(-Math.PI/2).translate(0, 0, 1);
		drawSide(m_side.setRGBA(0, 0, 1, 1));

		// bottom
		M.rotationX(Math.PI/2).translate(0, 0, 1);
	    drawSide(m_side.setRGBA(0, 1, 0, 1));

		// top
	    M.rotationX(-Math.PI/2).translate(0, 0, 1);
		drawSide(m_side.setRGBA(1, 0, 1, 1));

	    // fps
        m_count++;

        double theTime = System.currentTimeMillis()/1000.0;
        if (theTime >= m_startTime + 1.0) {
            System.out.format("%d fps\n", m_count); // falls die fps zu hoch sind: https://www.khronos.org/opengl/wiki/Swap_Interval#In_Windows
            m_startTime = theTime;
            m_count = 0;
        }
        
        // animation
        m_xAngle -= m_dxAngle;
        m_yAngle -= m_dyAngle;
	}
	
	public void changeXangle(double delta) {
		m_dxAngle += delta;
	}

	public void changeYangle(double delta) {
		m_dyAngle += delta;
	}
	
	private void drawSide(Side side) {
		// set geometric transformation matrices for all vertices of this model
        glUniformMatrix3fv(u_VM, false, V.mul(M, VM).normal(m_vm).get(m_mat3f));
        glUniformMatrix4fv(u_PVM, false, P.mul(VM, PVM).get(m_mat4f)); // get: stores in and returns m_mat4f
        
        // set color for all vertices of this model
        glUniform4fv(u_COLOR, side.getColor());

        // draw a quad
        side.setupPositions(m_POSITIONS);	
        side.setupNormals(m_NORMALS);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, side.getVertexCount());
    }

    private static class Side extends OGLObject {
    	final static int CoordinatesPerVertex = 3;
    	
		protected Side(Color4D color) {
			super(color);
        	
			final int nVertices = 4;
			final int nCoordinates = nVertices*CoordinatesPerVertex;
			
			// allocate vertex positions and normals
            allocatePositionBuffer(nCoordinates);
            allocateNormalBuffer(nCoordinates);
            
            // GL_TRIANGLE_STRIP because GL_QUADS are deprecated
            addVertex(-1, +1, 0);
            addVertex(-1, -1, 0);
            addVertex(+1, +1, 0);
            addVertex(+1, -1, 0);
            
            // bind vertex positions and normals
            bindPositionBuffer();           
            bindNormalBuffer();           
		}
    	
        private void addVertex(float x, float y, float z) {
            m_positions.put(m_vertexCount*CoordinatesPerVertex + 0, x);
            m_positions.put(m_vertexCount*CoordinatesPerVertex + 1, y);
            m_positions.put(m_vertexCount*CoordinatesPerVertex + 2, z);

            m_normals.put(m_vertexCount*CoordinatesPerVertex + 0, 0);
            m_normals.put(m_vertexCount*CoordinatesPerVertex + 1, 0);
            m_normals.put(m_vertexCount*CoordinatesPerVertex + 2, 1);

            m_vertexCount++;
        }
        
        public Side setRGBA(float r, float g, float b, float a) {
        	m_color.put(0, r);
        	m_color.put(1, g);
        	m_color.put(2, b);
        	m_color.put(3, a);
        	return this;
        }
    }

}
