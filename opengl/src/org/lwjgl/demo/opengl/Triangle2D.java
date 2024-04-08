package org.lwjgl.demo.opengl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glVertex3f;

import org.lwjgl.demo.util.Color4D;
import org.lwjgl.demo.util.OGLApp;
import org.lwjgl.demo.util.OGLModel2D;

public class Triangle2D extends OGLApp<TriangleModel> {

	public Triangle2D(TriangleModel model) {
		super(model);
		
		m_keyCallback = (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) model.changeSpeed(1.0);
			if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) model.changeSpeed(-1.0);;
		};
	}
	
	public static void main(String[] args) {
		new Triangle2D(new TriangleModel()).run("Triangle 2D", 640, 640, new Color4D(0.7f, 0.7f, 0.7f, 1));
	}
}

class TriangleModel extends OGLModel2D {
	final static float s = 1f; 						// triangle side length
	final static float s2 = s/2;					// half triangle side length
	final static float h = s2*(float)Math.sqrt(3); 	// triangle height

	private float m_speed = 2.0f;

	@Override
	public void render() {
        // legacy style 2D rendering (compatibility profile)
        
        // manipulate model-view matrix
        glLoadIdentity();
        glRotatef((float) glfwGetTime()*25f*m_speed, 0f, 0f, 1f);

        // render axis
        glBegin(GL_LINES);
        glColor3f(0f, 0f, 0f);
        glVertex3f(-1f, 0, 0f);
        glVertex3f(1f, 0, 0f);
        glVertex3f(0, -1f, 0f);
        glVertex3f(0, 1f, 0f);
        glEnd();
        
        /* Render triangle */
        glBegin(GL_TRIANGLES);
        glColor3f(1f, 0f, 0f);
        glVertex3f(-s2, -h/3, 0f);
        glColor3f(0f, 1f, 0f);
        glVertex3f(s2, -h/3, 0f);
        glColor3f(0f, 0f, 1f);
        glVertex3f(0f, 2*h/3, 0f);
        glEnd();
	}
	
	public void changeSpeed(double delta) {
		m_speed += delta;
	}
	
}