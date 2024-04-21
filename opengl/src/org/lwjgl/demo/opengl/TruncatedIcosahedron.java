package org.lwjgl.demo.opengl;

import static org.joml.Math.PI;
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

import org.joml.Matrix3d;
import org.joml.Matrix4x3d;
import org.joml.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.demo.util.Color4D;
import org.lwjgl.demo.util.OGLApp;
import org.lwjgl.demo.util.OGLModel3D;
import org.lwjgl.demo.util.OGLObject;

public class TruncatedIcosahedron extends OGLApp<TruncatedIcosahedronModel> {
    public TruncatedIcosahedron(TruncatedIcosahedronModel model) {
        super(model);
        
        m_keyCallback = (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            } else if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                switch (key) {
                    case GLFW_KEY_LEFT:  model.changeYangle(0.125); break;
                    case GLFW_KEY_RIGHT: model.changeYangle(-0.125); break;
                    case GLFW_KEY_UP:    model.changeXangle(0.125); break;
                    case GLFW_KEY_DOWN:  model.changeXangle(-0.125); break;
                    case GLFW_KEY_PAGE_UP:   model.increaseIndex(); break;
                    case GLFW_KEY_PAGE_DOWN: model.decreaseIndex(); break;
                    case GLFW_KEY_SPACE: model.resetRotation(); break;
                }
            }
        };
    }
    
    public static void main(String[] args) {
        new TruncatedIcosahedron(new TruncatedIcosahedronModel())
            .run("TruncatedIcosahedron", 1920 * 1, 1080 * 1, new Color4D(0.7f, 0.7f, 0.7f, 1));
    }
}

class TruncatedIcosahedronModel extends OGLModel3D {
    final static double deg2rad = PI/180;

    private final Matrix3d m_vm = new Matrix3d();
    private final Vector3d m_light  = new Vector3d();
    private final FloatBuffer m_vec3f = BufferUtils.createFloatBuffer(3);
    private final FloatBuffer m_mat3f = BufferUtils.createFloatBuffer(3*3);
    private final FloatBuffer m_mat4f = BufferUtils.createFloatBuffer(4*4);

    private double m_startTime = System.currentTimeMillis() / 1000.0;
    private double m_distance = 50.0f;   // camera distance
    private double m_dxAngle = 0;        // degrees
    private double m_dyAngle = 0;        // degrees
    private double m_xAngle = 0;         // degrees
    private double m_yAngle = 0;         // degrees
    private double m_zAngle = 0;         // degrees
    private long m_count;                // fps

    @Override
    public void init(int width, int height) {
        super.init(width, height);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public TruncatedIcosahedronModel() {
        var row9 = new Hexagon(new Pentagon(true), null, null, new Hexagon());
        var row8 = new Hexagon(new Hexagon(new Pentagon(true), null, null, null), null, row9, new Pentagon(false));
        var row7 = new Hexagon(new Pentagon(true), row8, null, new Hexagon(null, null, null, new Pentagon(false)));
        var row6 = new Hexagon(new Hexagon(), null, row7, new Pentagon(false));
        var row5 = new Hexagon(new Pentagon(true), row6, null, new Hexagon());
        var row4 = new Hexagon(new Hexagon(), null, row5, new Pentagon(false));
        var row3 = new Hexagon(new Pentagon(true), row4, null, new Hexagon());
        var row2 = new Hexagon(new Hexagon(), null, row3, new Pentagon(false));
        var row1 = new Hexagon(new Pentagon(true), row2, null, new Hexagon());
        objectToRender = new Hexagon(new Hexagon(), null, row1, new Pentagon(false));
    }

    private ObjectToRender objectToRender;
    private int currentIndex;

    @Override
    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // VIEW
        V.translation(0.0, 0.0, -m_distance)
         .rotateX(m_xAngle * deg2rad)
         .rotateY(m_yAngle * deg2rad)
         .rotateZ(m_zAngle * deg2rad); // V = T*Rx*Ry*Rz

        // LIGHT
        glUniform3fv(u_LIGHT, m_light.set(0.0, 0.0, 10.0)
                                     .normalize()
                                     .get(m_vec3f)); // V*m_light

        renderObject(objectToRender, new Matrix4x3d().translation(0, 0, 0), 0, false);
        renderObject(objectToRender, new Matrix4x3d().translation(0, 0, 0), 0, true);

        m_count++;

        double theTime = System.currentTimeMillis() / 1000.0;
        if (theTime >= m_startTime + 1.0) {
            System.out.format("%d fps\n", m_count); // falls die fps zu hoch sind: https://www.khronos.org/opengl/wiki/Swap_Interval#In_Windows
            m_startTime = theTime;
            m_count = 0;
        }
        
        // animation
        m_xAngle -= m_dxAngle;
        m_yAngle -= m_dyAngle;
    }

    private void renderObject(ObjectToRender object, Matrix4x3d matrix, int index, boolean invert) {
        M.set(matrix);
        if (invert) {
            M.rotateX(Math.PI);
        }

        if (invert && object instanceof Pentagon) {
            M.rotateZ(Math.PI);
        }

        drawSide(new RenderObject(invert ? new Color4D(1, 1, 0, 0.4f) : new Color4D(1, 0, 0, 0.4f), object));

        if (invert && object instanceof Pentagon) {
            M.rotateZ(-Math.PI);
        }
        if (invert) {
            M.rotateX(-Math.PI);
        }

        var neighbors = object.getNeighbors();
        for (var neighbor : neighbors) {
            if (neighbor.object() == null) {
                continue;
            }

            var currentMatrix = new Matrix4x3d(matrix);

            var matrixMoves = neighbor.matrixMoves();
            currentMatrix.translate(matrixMoves.x, matrixMoves.y, 0);
            index++;
            if (index < currentIndex) {
                var angle = (neighbor.object() instanceof Pentagon ? 180 + 142 : 180 + 138) * deg2rad;
                var rotationTranslation = neighbor.rotationTranslation();
                var rotationAxis = neighbor.rotationAxis();
                currentMatrix.translate(-rotationTranslation.x, -rotationTranslation.y, 0);
                currentMatrix.rotate(neighbor.negativeAngle() ? -angle : angle, rotationAxis.x, rotationAxis.y, 0);
                currentMatrix.translate(rotationTranslation.x, rotationTranslation.y, 0);
            }
            renderObject(neighbor.object(), currentMatrix, index, invert);
        }
    }
    
    public void resetRotation() {
        m_dxAngle = 0;
        m_dyAngle = 0;
    }

    public void changeXangle(double delta) {
        m_dxAngle += delta;
    }

    public void changeYangle(double delta) {
        m_dyAngle += delta;
    }

    public void increaseIndex() {
        currentIndex++;
    }

    public void decreaseIndex() {
        currentIndex--;
    }
    
    private void drawSide(RenderObject side) {
        // set geometric transformation matrices for all vertices of this model
        glUniformMatrix3fv(u_VM, false, V.mul(M, VM).normal(m_vm).get(m_mat3f));
        glUniformMatrix4fv(u_PVM, false, P.mul(VM, PVM).get(m_mat4f)); // get: stores in and returns m_mat4f
        
        // set color for all vertices of this model
        glUniform4fv(u_COLOR, side.getColor());

        // draw a quad
        side.setupPositions(m_POSITIONS);    
        side.setupNormals(m_NORMALS);
        glDrawArrays(GL_POLYGON, 0, side.getVertexCount());
    }

    private static class RenderObject extends OGLObject {
        final static int CoordinatesPerVertex = 3;
        
        protected RenderObject(Color4D color, ObjectToRender objectToRender) {
            super(color);
            
            var vertices = objectToRender.getVertices();

            final int nCoordinates = vertices.length * CoordinatesPerVertex;
            
            // allocate vertex positions and normals
            allocatePositionBuffer(nCoordinates);
            allocateNormalBuffer(nCoordinates);

            // add vertices
            for (var vertex : vertices) {
                addVertex((float)vertex.x, (float)vertex.y, 0f);
            }

            // bind vertex positions and normals
            bindPositionBuffer();           
            bindNormalBuffer();           
        }
        
        private void addVertex(float x, float y, float z) {
            m_positions.put(m_vertexCount * CoordinatesPerVertex + 0, x);
            m_positions.put(m_vertexCount * CoordinatesPerVertex + 1, y);
            m_positions.put(m_vertexCount * CoordinatesPerVertex + 2, z);

            m_normals.put(m_vertexCount * CoordinatesPerVertex + 0, 0);
            m_normals.put(m_vertexCount * CoordinatesPerVertex + 1, 0);
            m_normals.put(m_vertexCount * CoordinatesPerVertex + 2, 1);

            m_vertexCount++;
        }
    }    
    
}
