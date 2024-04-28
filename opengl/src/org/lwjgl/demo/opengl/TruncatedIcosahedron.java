package org.lwjgl.demo.opengl;

import static org.lwjgl.glfw.GLFW.*;
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
import java.util.Map;

import org.joml.Matrix3d;
import org.joml.Matrix4x3d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.demo.util.Color4D;
import org.lwjgl.demo.util.OGLApp;
import org.lwjgl.demo.util.OGLModel3D;
import org.lwjgl.demo.util.OGLObject;

public class TruncatedIcosahedron extends OGLApp<TruncatedIcosahedronModel> {

    private static final double SCALE_VIEWPORT = 1.5;

    public TruncatedIcosahedron(TruncatedIcosahedronModel model) {
        super(model);
        
        m_keyCallback = (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            } else if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                switch (key) {
                    case GLFW_KEY_LEFT -> model.changeYangle(0.125);
                    case GLFW_KEY_RIGHT -> model.changeYangle(-0.125);
                    case GLFW_KEY_UP -> model.changeXangle(0.125);
                    case GLFW_KEY_DOWN -> model.changeXangle(-0.125);
                    case GLFW_KEY_SPACE -> model.resetRotation();
                    case GLFW_KEY_PAGE_UP -> model.increaseIndex();
                    case GLFW_KEY_PAGE_DOWN -> model.decreaseIndex();

                    case GLFW_KEY_W -> model.increaseScale();
                    case GLFW_KEY_S -> model.decreaseScale();
                    default -> {}
                }
            }
        };
    }
    
    public static void main(String[] args) {
        new TruncatedIcosahedron(new TruncatedIcosahedronModel())
            .run("TruncatedIcosahedron",
                 (int)(1920 * SCALE_VIEWPORT),
                 (int)(1080 * SCALE_VIEWPORT),
                 new Color4D(0.7f, 0.7f, 0.7f, 1));
    }
}

class TruncatedIcosahedronModel extends OGLModel3D {

    private final Matrix3d vm = new Matrix3d();
    private final Vector3d light  = new Vector3d();
    private final FloatBuffer vec3f = BufferUtils.createFloatBuffer(3);
    private final FloatBuffer mat3f = BufferUtils.createFloatBuffer(3*3);
    private final FloatBuffer mat4f = BufferUtils.createFloatBuffer(4*4);

    private double startTime = System.currentTimeMillis() / 1000.0;
    private double distance = 30.0f;   // camera distance
    private double dxAngle = 0;        // degrees
    private double dyAngle = 0;        // degrees
    private double xAngle = 0;         // degrees
    private double yAngle = 0;         // degrees
    private double zAngle = 0;         // degrees
    private long count;                // fps

    private ObjectToRender objectToRender;
    private int currentIndex;
    private Map<Class<?>, RenderObject> renderObjects;

    private double scaleFactor = 1.0;

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        
        renderObjects = Map.of(
            Hexagon.class, new HexagonRenderObject(),
            Pentagon.class, new PentagonRenderObject()
        );

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public TruncatedIcosahedronModel() {
        var chain0 = new Pentagon(6, new Hexagon(11), null, null, null,
            new Hexagon(16, new Hexagon(21, new Pentagon(26), null, null, null, null, null), new Pentagon(31), null, null, null, null));
       
        var chain1 = new Pentagon(7, new Hexagon(12), null, null, null, 
            new Hexagon(17, new Hexagon(22), new Pentagon(27), null, null, null, null));
       
        var chain2 = new Pentagon(8, new Hexagon(13), null, null, null, 
            new Hexagon(18, new Hexagon(23), new Pentagon(28), null, null, null, null));
    
        var chain3 = new Pentagon(9, new Hexagon(14), null, null, null, 
            new Hexagon(19, new Hexagon(24), new Pentagon(29), null, null, null, null));

        var chain4 = new Pentagon(10, new Hexagon(15), null, null, null, 
            new Hexagon(20, new Hexagon(25), new Pentagon(30), null, null, null, null));

        objectToRender = new Pentagon(0,
            new Hexagon(1, null, chain0, null, null, null, null),
            new Hexagon(2, null, chain1, null, null, null, null),
            new Hexagon(3, null, chain2, null, null, null, null),
            new Hexagon(4, null, chain3, null, null, null, null),
            new Hexagon(5, null, chain4, null, null, null, null));
    }

    @Override
    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // VIEW
        V.translation(0.0, 0.0, -distance)
         .rotateX(Math.toRadians(xAngle))
         .rotateY(Math.toRadians(yAngle))
         .rotateZ(Math.toRadians(zAngle)); // V = T * Rx * Ry * Rz

        // LIGHT
        glUniform3fv(u_LIGHT, light.set(0.0, 0.0, 10.0)
                                   .normalize()
                                   .get(vec3f)); // V * m_light

        
        renderObject(objectToRender, new Matrix4x3d().translation(0, 0, -2.3 * scaleFactor).scale(scaleFactor), false);
        renderObject(objectToRender, new Matrix4x3d().translation(0, 0, -2.3 * scaleFactor).scale(scaleFactor), true);

        count++;

        double theTime = System.currentTimeMillis() / 1000.0;
        if (theTime >= startTime + 1.0) {
            System.out.format("%d fps\n", count); // falls die fps zu hoch sind: https://www.khronos.org/opengl/wiki/Swap_Interval#In_Windows
            startTime = theTime;
            count = 0;
        }
        
        // animation
        xAngle -= dxAngle;
        yAngle -= dyAngle;
    }

    private void renderObject(ObjectToRender object, Matrix4x3d matrix, boolean drawInside) {
        M.set(matrix);
        if (drawInside) {
            M.rotateY(Math.PI);
        }

        drawSide(renderObjects.get(object.getClass()).setRGBA(object.getColor()));

        if (drawInside) {
            M.rotateY(-Math.PI);
        }

        for (var neighbor : object.getNeighbors()) {
            if (neighbor.object() == null) {
                continue;
            }

            var newMatrix = neighbor.matrixMoves().apply(new Matrix4x3d(matrix));

            if (neighbor.object().getIndex() < currentIndex) {
                var angle = getAngle(object, neighbor.object());
                newMatrix.rotateX(Math.toRadians(-angle));
            }
            renderObject(neighbor.object(), newMatrix.translate(0, neighbor.object().distanceToCenter(), 0), drawInside);
        }
    }

    private float getAngle(ObjectToRender currentObject, ObjectToRender neighbor) {
        if (currentObject.getClass().equals(neighbor.getClass()) && currentObject instanceof Hexagon) {
            return 180 + 138.19f;
        }
        return 180 + 142.62f;
    }

    public void resetRotation() {
        dxAngle = 0;
        dyAngle = 0;
    }

    public void changeXangle(double delta) {
        dxAngle += delta;
    }

    public void changeYangle(double delta) {
        dyAngle += delta;
    }

    public void increaseIndex() {
        if (currentIndex == 32) {
            return;
        }
        currentIndex++;
    }

    public void decreaseIndex() {
        if (currentIndex == 0) {
            return;
        }
        currentIndex--;
    }

    public void increaseScale() {
        scaleFactor += 0.05;
    }

    public void decreaseScale() {
        scaleFactor -= 0.05;
    }

    private void drawSide(OGLObject side) {
        // set geometric transformation matrices for all vertices of this model
        glUniformMatrix3fv(u_VM, false, V.mul(M, VM).normal(vm).get(mat3f));
        glUniformMatrix4fv(u_PVM, false, P.mul(VM, PVM).get(mat4f)); // get: stores in and returns m_mat4f
        
        // set color for all vertices of this model
        glUniform4fv(u_COLOR, side.getColor());

        // draw a quad
        side.setupPositions(m_POSITIONS);    
        side.setupNormals(m_NORMALS);
        glDrawArrays(GL_POLYGON, 0, side.getVertexCount());
    }

    private abstract static class RenderObject extends OGLObject {
        protected abstract Vector2d[] getVertices();

        final static int COORDINATES_PER_VERTEX = 3;
        
        protected RenderObject() {
            super(new Color4D(0, 0, 0, 0));
            
            var vertices = getVertices();

            final int nCoordinates = vertices.length * COORDINATES_PER_VERTEX;
            
            // allocate vertex positions and normals
            allocatePositionBuffer(nCoordinates);
            allocateNormalBuffer(nCoordinates);

            // add vertices
            for (var vertex : vertices) {
                addVertex((float)(vertex.x), (float)(vertex.y), 0f);
            }

            // bind vertex positions and normals
            bindPositionBuffer();           
            bindNormalBuffer();           
        }
        
        private void addVertex(float x, float y, float z) {
            m_positions.put(m_vertexCount * COORDINATES_PER_VERTEX + 0, x);
            m_positions.put(m_vertexCount * COORDINATES_PER_VERTEX + 1, y);
            m_positions.put(m_vertexCount * COORDINATES_PER_VERTEX + 2, z);

            m_normals.put(m_vertexCount * COORDINATES_PER_VERTEX + 0, 0);
            m_normals.put(m_vertexCount * COORDINATES_PER_VERTEX + 1, 0);
            m_normals.put(m_vertexCount * COORDINATES_PER_VERTEX + 2, 1);

            m_vertexCount++;
        }
    }

    private static class HexagonRenderObject extends RenderObject {

        protected Vector2d[] getVertices() {
            return new Vector2d[] {
                new Vector2d( 1.0f,  0.0f),
                new Vector2d( 0.5f, 0.8660254037844f),
                new Vector2d( -0.5f, 0.8660254037844f),
                new Vector2d( -1.0f, 0.0f),
                new Vector2d( -0.5f, -0.8660254037844f),
                new Vector2d( 0.5f, -0.8660254037844f)
            };
        }
    }

    private static class PentagonRenderObject extends RenderObject {

        protected Vector2d[] getVertices() {
            return new Vector2d[] {
                new Vector2d(0, 0.850650808352f),
                new Vector2d(-0.8090169943749f, 0.2628655560595f),
                new Vector2d(-0.4999999999999f, -0.6881909602356f),
                new Vector2d(0.4999999999999f, -0.6881909602356f),
                new Vector2d(0.8090169943749f, 0.2628655560595f),
            };
        }
    }
}
