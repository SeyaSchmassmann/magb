package org.lwjgl.demo.opengl;

import static org.joml.Math.PI;
import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.joml.Math.sqrt;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL20C.glUniform3fv;
import static org.lwjgl.opengl.GL20C.glUniform4fv;
import static org.lwjgl.opengl.GL20C.glUniformMatrix3fv;
import static org.lwjgl.opengl.GL20C.glUniformMatrix4fv;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.demo.util.Color4D;
import org.lwjgl.demo.util.OGLApp;
import org.lwjgl.demo.util.OGLModel3D;
import org.lwjgl.demo.util.OGLObject;

public class Gears3D extends OGLApp<GearsModel> {
	public Gears3D(GearsModel model) {
		super(model);
		
		m_keyCallback = (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			if (action == GLFW_PRESS) {
				switch(key) {
				case GLFW_KEY_LEFT: model.changeSpeed(1.0); break;
				case GLFW_KEY_RIGHT: model.changeSpeed(-1.0); break;
				case GLFW_KEY_UP: model.changeXangle(10.0); break;
				case GLFW_KEY_DOWN: model.changeXangle(-10.0); break;
				}
			}
		};
	}
	
	public static void main(String[] args) {
		new Gears3D(new GearsModel()).run("Gears", 640, 640, new Color4D(0.7f, 0.7f, 0.7f, 1));
	}
}

class GearsModel extends OGLModel3D {
	final static double deg2rad = PI/180;

	private final Matrix3d m_vm = new Matrix3d();
	private final Vector3d m_light  = new Vector3d();
	private final FloatBuffer m_vec3f = BufferUtils.createFloatBuffer(3);
	private final FloatBuffer m_mat3f = BufferUtils.createFloatBuffer(3*3);
	private final FloatBuffer m_mat4f = BufferUtils.createFloatBuffer(4*4);

	private Gear m_gear1, m_gear2, m_gear3;
    private double m_startTime = System.currentTimeMillis()/1000.0;
    private double m_distance = 40.0f;	// camera distance
    private double m_angle;				// degrees
    private double m_deltaDeg =  2; 	// degrees
    private double m_xAngle = 70;		// degrees
    private long   m_count;				// fps

	@Override
	public void init(int width, int height) {
		super.init(width, height);
        m_gear1 = new Gear(1.0, 4.0, 1.0, 20, 0.7, new Color4D(0.8f, 0.1f, 0.0f, 1.0f));
        m_gear2 = new Gear(0.5, 2.0, 2.0, 10, 0.7, new Color4D(0.0f, 0.8f, 0.2f, 1.0f));
        m_gear3 = new Gear(1.3, 2.0, 0.5, 10, 0.7, new Color4D(0.2f, 0.2f, 1.0f, 1.0f));
	}

	@Override
	public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // VIEW
        V.translation(0.0, 0.0, -m_distance)
         .rotateX(m_xAngle*deg2rad)
         .rotateY(0.0f*deg2rad);
        //V.rotateZ(45.0f*deg2rad);

        // LIGHT
        glUniform3fv(u_LIGHT, V.transformDirection(m_light.set(5.0, 5.0, 10.0)).normalize().get(m_vec3f)); // V*m_light

        // GEAR 1 (model 1)
        M.translation(-3.0, -2.0, 0.0)
         .rotateZ(m_angle*deg2rad);
        drawGear(m_gear1);

        // GEAR 2 (model 2)
        M.translation(3.1, -2.0, 0.0)
         .rotateZ((-2.0*m_angle - 9.0)*deg2rad);
        drawGear(m_gear2);

        // GEAR 3 (model 3)
        M.translation(-3.1, 4.2, 0.0)
         .rotateZ((-2.0*m_angle - 25.0)*deg2rad);
        drawGear(m_gear3);

        // fps
        m_count++;

        double theTime = System.currentTimeMillis()/1000.0;
        if (theTime >= m_startTime + 1.0) {
            System.out.format("%d fps\n", m_count);
            m_startTime = theTime;
            m_count = 0;
        }
        
        // animation
        m_angle += m_deltaDeg;
	}
	
	public void changeSpeed(double delta) {
		m_deltaDeg += delta;
	}
	
	public void changeXangle(double delta) {
		m_xAngle += delta;
	}

	private void drawGear(Gear gear) {
		// compute shader data structures
        glUniformMatrix3fv(u_VM, false, V.mul(M, VM).normal(m_vm).get(m_mat3f));
        glUniformMatrix4fv(u_PVM, false, P.mul(VM, PVM).get(m_mat4f)); // get: stores in and returns m_mat4f
        glUniform4fv(u_COLOR, gear.getColor());

        gear.setupPositions(m_POSITIONS);	
        gear.setupNormals(m_NORMALS);
        glDrawArrays(GL_TRIANGLES, 0, gear.getVertexCount());
    }

    private static class Gear extends OGLObject {
        private double m_normalX, m_normalY, m_normalZ;
        private final double[] m_quads = new double[4*3];
        private int m_quadCount;
        
        private Gear(double innerRadius, double outerRadius, double width, int teeth, double toothDepth, Color4D color) {
        	super(color);
        	
            allocatePositionBuffer(2000*3); // allocate vertex positions
            allocateNormalBuffer(2000*3); 	// allocate vertex normals
            build(innerRadius, outerRadius, width, teeth, toothDepth);
            bindPositionBuffer();           
            bindNormalBuffer();           
        }

        private void build(double innerRadius, double outerRadius, double width, int teeth, double toothDepth) {
            double r0 = innerRadius;
            double r1 = outerRadius - toothDepth/2.0;
            double r2 = outerRadius + toothDepth/2.0;
            double da = 2.0*PI/teeth/4.0;

            normal3f(0.0, 0.0, 1.0);

            /* draw front face */
            m_quadCount = 0;
            for (int i = 0; i <= teeth; i++) {
                double angle = i*2.0*PI/teeth;
                vertex3f(r0*cos(angle), r0*sin(angle), width*0.5);
                vertex3f(r1*cos(angle), r1*sin(angle), width*0.5);
                if (i < teeth) {
                    vertex3f(r0*cos(angle), r0*sin(angle), width*0.5);
                    vertex3f(r1*cos(angle + 3*da), r1*sin(angle + 3*da), width*0.5);
                }
            }

            /* draw front sides of teeth */
            da = 2.0*PI/teeth/4.0;
            for (int i = 0; i < teeth; i++) {
                double angle = i*2.0*PI/teeth;

                m_quadCount = 0;

                vertex3f(r1*cos(angle), r1*sin(angle), width*0.5);
                vertex3f(r2*cos(angle + da), r2*sin(angle + da), width*0.5);
                vertex3f(r1*cos(angle + 3*da), r1*sin(angle + 3*da), width*0.5);
                vertex3f(r2*cos(angle + 2*da), r2*sin(angle + 2*da), width*0.5);
            }

            normal3f(0.0, 0.0, -1.0);

            /* draw back face */
            m_quadCount = 0;
            for (int i = 0; i <= teeth; i++) {
                double angle = i*2.0*PI/teeth;
                
                vertex3f(r1*cos(angle), r1*sin(angle), -width*0.5);
                vertex3f(r0*cos(angle), r0*sin(angle), -width*0.5);
                if (i < teeth) {
                    vertex3f(r1*cos(angle + 3*da), r1*sin(angle + 3*da), -width*0.5);
                    vertex3f(r0*cos(angle), r0*sin(angle), -width*0.5);
                }
            }

            /* draw back sides of teeth */
            da = 2.0*PI/teeth/4.0;
            for (int i = 0; i < teeth; i++) {
                double angle = i*2.0*PI/teeth;

                m_quadCount = 0;

                vertex3f(r1*cos(angle + 3*da), r1*sin(angle + 3*da), -width*0.5);
                vertex3f(r2*cos(angle + 2*da), r2*sin(angle + 2*da), -width*0.5);
                vertex3f(r1*cos(angle), r1*sin(angle), -width*0.5);
                vertex3f(r2*cos(angle + da), r2*sin(angle + da), -width*0.5);
            }

            /* draw outward faces of teeth */
            m_quadCount = 0;
            for (int i = 0; i < teeth; i++) {
                double angle = i*2.0*PI/teeth;

                vertex3f(r1*cos(angle), r1*sin(angle), width*0.5);
                vertex3f(r1*cos(angle), r1*sin(angle), -width*0.5);
                double u = r2*cos(angle + da) - r1*cos(angle);
                double v = r2*sin(angle + da) - r1*sin(angle);
                double len = sqrt(u*u + v*v);
                u /= len;
                v /= len;
                normal3f(v, -u, 0.0);
                vertex3f(r2*cos(angle + da), r2*sin(angle + da), width*0.5);
                vertex3f(r2*cos(angle + da), r2*sin(angle + da), -width*0.5);

                normal3f(cos(angle), sin(angle), 0.0);
                vertex3f(r2*cos(angle + 2*da), r2*sin(angle + 2*da), width*0.5);
                vertex3f(r2*cos(angle + 2*da), r2*sin(angle + 2*da), -width*0.5);

                u = r1*cos(angle + 3*da) - r2*cos(angle + 2*da);
                v = r1*sin(angle + 3*da) - r2*sin(angle + 2*da);
                normal3f(v, -u, 0.0);
                vertex3f(r1*cos(angle + 3*da), r1*sin(angle + 3*da), width*0.5);
                vertex3f(r1*cos(angle + 3*da), r1*sin(angle + 3*da), -width*0.5);

                normal3f(cos(angle), sin(angle), 0.0);
            }

            vertex3f(r1*cos(0), r1*sin(0), width*0.5);
            vertex3f(r1*cos(0), r1*sin(0), -width*0.5);

            /* draw inside radius cylinder */
            m_quadCount = 0;
            for (int i = 0; i <= teeth; i++) {
                double angle = (i == teeth ? 0 : i)*2.0*PI/teeth; // Map 2*PI to 0 to get an exact hash below
                
                normal3f(-cos(angle), -sin(angle), 0.0);
                vertex3f(r0*cos(angle), r0*sin(angle), -width*0.5);
                vertex3f(r0*cos(angle), r0*sin(angle), width*0.5);
            }

            /* Emulate glShadeModel(GL_SMOOTH) for inside radius cylinder */
            Map<Vector3f, Vector3f> smoothMap = new HashMap<>(teeth*2);
            // Sum normals around same position
            for (int i = m_vertexCount - teeth*6; i < m_vertexCount; i++) {
                float
                    x = m_normals.get(i*3),
                    y = m_normals.get(i*3 + 1),
                    z = m_normals.get(i*3 + 2);

                smoothMap.compute(new Vector3f(
                    m_positions.get(i*3),
                    m_positions.get(i*3 + 1),
                    m_positions.get(i*3 + 2)
                ), (key, normal) -> normal == null
                    ? new Vector3f(x, y, z)
                    : normal.add(x, y, z));
            }
            
            // Normalize
            smoothMap.values().forEach(Vector3f::normalize);
            
            // Apply smooth normals
            for (int i = m_vertexCount - teeth*6; i < m_vertexCount; i++) {
                Vector3f normal = smoothMap.get(new Vector3f(
                    m_positions.get(i*3 + 0),
                    m_positions.get(i*3 + 1),
                    m_positions.get(i*3 + 2)
                ));

                m_normals.put(i*3 + 0, normal.x);
                m_normals.put(i*3 + 1, normal.y);
                m_normals.put(i*3 + 2, normal.z);
            }
       }

        private void normal3f(double x, double y, double z) {
            m_normalX = x;
            m_normalY = y;
            m_normalZ = z;
        }

        private void vertex3f(double x, double y, double z) {
            m_quads[m_quadCount*3 + 0] = x;
            m_quads[m_quadCount*3 + 1] = y;
            m_quads[m_quadCount*3 + 2] = z;

            if (++m_quadCount == 4) {
                addVertex(m_quads[0], m_quads[1], m_quads[2]);
                addVertex(m_quads[3], m_quads[4], m_quads[5]);
                addVertex(m_quads[6], m_quads[7], m_quads[8]);

                addVertex(m_quads[6], m_quads[7], m_quads[8]);
                addVertex(m_quads[3], m_quads[4], m_quads[5]);
                addVertex(m_quads[9], m_quads[10], m_quads[11]);

                System.arraycopy(m_quads, 2*3, m_quads, 0, 2*3);
                m_quadCount = 2;
            }
        }

        private void addVertex(double x, double y, double z) {
            m_positions.put(m_vertexCount*3 + 0, (float)x);
            m_positions.put(m_vertexCount*3 + 1, (float)y);
            m_positions.put(m_vertexCount*3 + 2, (float)z);

            m_normals.put(m_vertexCount*3 + 0, (float)m_normalX);
            m_normals.put(m_vertexCount*3 + 1, (float)m_normalY);
            m_normals.put(m_vertexCount*3 + 2, (float)m_normalZ);

            m_vertexCount++;
        }

    }
	
}
