uniform vec4 u_COLOR;		// the same color for all fragments of a model

in float v_Shade;			// shade of a fragment

#if __VERSION__ < 330
    out vec4 out_Color;		// resulting pixel color
#else
    layout(location = 0) out vec4 out_Color;
#endif

void main() {
    out_Color = vec4(u_COLOR.xyz*v_Shade, u_COLOR.w);
}
