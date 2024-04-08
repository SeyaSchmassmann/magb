uniform mat4 u_PVM;			// the same geometric transforms for all vertices of a model
uniform mat3 u_VM;
uniform vec3 u_LIGHT;

#if __VERSION__ < 330
    in vec3 in_Position;	// vertex position
    in vec3 in_Normal;		// vertex normal
#else
    layout(location = 0) in vec3 in_Position;
    layout(location = 1) in vec3 in_Normal;
#endif

out float v_Shade;			// for each vertex, needed in fragment shader

void main() {
    vec3 normal = normalize(u_VM*in_Normal);
    v_Shade = max(dot(normal, u_LIGHT), 0.0);
    gl_Position = u_PVM*vec4(in_Position, 1.0);
}
