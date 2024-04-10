[glsl|
    precision mediump float;
    uniform float shade;
    varying vec3 vcolor;
    void main () {
        gl_FragColor = shade * vec4(vcolor, 1.0);
    }
|]

[glsl|
    1 | 2;
    true || false;
|]

[glsl|
    1 | 2;
