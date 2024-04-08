package org.lwjgl.demo.util;

//https://javadoc.lwjgl.org/index.html?org/lwjgl/opengl/GL11.html

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glViewport;

abstract public class OGLModel2D implements OGLModel {
	@Override
	public void init(int width, int height) {
        final float ratio = width/(float)height;

        /* Set viewport and clear screen */
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT);

        /* Set orthographic projection */
        glMatrixMode(GL_PROJECTION);				// choose projection matrix
        glLoadIdentity();							// set projection matrix to identity
        glOrtho(-ratio, ratio, -1f, 1f, 1f, -1f);	// set parallel projection
        glMatrixMode(GL_MODELVIEW);					// choose model view matrix
	}

	@Override
	abstract public void render();
}
