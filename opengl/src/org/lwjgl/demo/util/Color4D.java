package org.lwjgl.demo.util;

public class Color4D {
	public float r,g,b,a;
	
	public Color4D(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
	
	public float[] toArray() {
		return new float[] { r, g, b, a };
	}
}
