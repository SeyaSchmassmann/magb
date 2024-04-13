package org.lwjgl.demo.util;

// https://javadoc.lwjgl.org/index.html?org/lwjgl/opengl/GL30.html
    
import static org.lwjgl.demo.util.IOUtil.ioResourceToByteBuffer;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_TRUE;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL20C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20C.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20C.glAttachShader;
import static org.lwjgl.opengl.GL20C.glCompileShader;
import static org.lwjgl.opengl.GL20C.glCreateProgram;
import static org.lwjgl.opengl.GL20C.glCreateShader;
import static org.lwjgl.opengl.GL20C.glGetAttribLocation;
import static org.lwjgl.opengl.GL20C.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20C.glGetProgrami;
import static org.lwjgl.opengl.GL20C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20C.glGetShaderi;
import static org.lwjgl.opengl.GL20C.glGetUniformLocation;
import static org.lwjgl.opengl.GL20C.glLinkProgram;
import static org.lwjgl.opengl.GL20C.glShaderSource;
import static org.lwjgl.opengl.GL20C.glUseProgram;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.joml.Matrix4d;
import org.joml.Matrix4x3d;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

public abstract class OGLModel3D implements OGLModel {
    protected int m_PROGRAM;    // OpenGL attribute identifiers
    protected int m_POSITIONS;
    protected int m_NORMALS;
    
    protected int u_VM;         // OpenGL uniform identifiers
    protected int u_PVM;
    protected int u_LIGHT;
    protected int u_COLOR;

    protected final Matrix4d
        P   = new Matrix4d(),
        PVM = new Matrix4d();    // Projection*View*Model transform for positions
    protected final Matrix4x3d
        V   = new Matrix4x3d(),
        M   = new Matrix4x3d(),
        VM  = new Matrix4x3d();    // View*Model transform used for normals

    public void init(int width, int height) {
        GLCapabilities caps = GL.getCapabilities();
        if (!caps.OpenGL30) {
            throw new IllegalStateException("This demo requires OpenGL 3.0 or higher.");
        }

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);

        setSize(width, height);

        // compile shaders
        try {
            ByteBuffer vs = ioResourceToByteBuffer("resources/shader1.vert", 4096);
            ByteBuffer fs = ioResourceToByteBuffer("resources/shader1.frag", 4096);

            int version;
            if (caps.OpenGL33) {
                version = 330;
            } else if (caps.OpenGL21) {
                version = 210;
            } else {
                version = 110;
            }

            m_PROGRAM = compileShaders(version, vs, fs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // define shader uniform locations
        u_PVM = glGetUniformLocation(m_PROGRAM, "u_PVM");
        u_VM = glGetUniformLocation(m_PROGRAM, "u_VM");
        u_LIGHT = glGetUniformLocation(m_PROGRAM, "u_LIGHT");
        u_COLOR = glGetUniformLocation(m_PROGRAM, "u_COLOR");

        // define shader attribute locations
        m_POSITIONS = glGetAttribLocation(m_PROGRAM, "in_Position");
        m_NORMALS = glGetAttribLocation(m_PROGRAM, "in_Normal");

        if (caps.OpenGL30) {
            int vao = glGenVertexArrays();
            glBindVertexArray(vao); // bind and forget
        }
    }

    abstract public void render();

    protected void setSize(int width, int height) {
        float h = height/(float)width;

        glViewport(0, 0, width, height);
        if (h < 1.0f) {
            P.setFrustum(-1.0/h, 1.0/h, -1.0, 1.0, 5.0, 100.0);
        } else {
            P.setFrustum(-1.0, 1.0, -h, h, 5.0, 100.0);
        }
    }

    private static int compileShaders(int version, ByteBuffer vs, ByteBuffer fs) {
        int v = glCreateShader(GL_VERTEX_SHADER);
        int f = glCreateShader(GL_FRAGMENT_SHADER);

        compileShader(version, v, vs);
        compileShader(version, f, fs);

        int p = glCreateProgram();
        glAttachShader(p, v);
        glAttachShader(p, f);
        glLinkProgram(p);
        printProgramInfoLog(p);

        if (glGetProgrami(p, GL_LINK_STATUS) != GL_TRUE) {
            throw new IllegalStateException("Failed to link program.");
        }

        glUseProgram(p);
        return p;
    }

    private static void compileShader(int version, int shader, ByteBuffer code) {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer header = stack.ASCII("#version " + version + "\n#line 0\n", false);

            glShaderSource(
                shader,
                stack.pointers(header, code),
                stack.ints(header.remaining(), code.remaining())
            );

            glCompileShader(shader);
            printShaderInfoLog(shader);

            if (glGetShaderi(shader, GL_COMPILE_STATUS) != GL_TRUE) {
                throw new IllegalStateException("Failed to compile shader.");
            }
        }
    }

    private static void printShaderInfoLog(int obj) {
        int infologLength = glGetShaderi(obj, GL_INFO_LOG_LENGTH);
        if (infologLength > 0) {
            glGetShaderInfoLog(obj);
            System.out.format("%s\n", glGetShaderInfoLog(obj));
        }
    }

    private static void printProgramInfoLog(int obj) {
        int infologLength = glGetProgrami(obj, GL_INFO_LOG_LENGTH);
        if (infologLength > 0) {
            glGetProgramInfoLog(obj);
            System.out.format("%s\n", glGetProgramInfoLog(obj));
        }
    }

}
