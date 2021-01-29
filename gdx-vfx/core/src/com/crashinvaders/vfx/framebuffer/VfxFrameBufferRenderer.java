/*******************************************************************************
 * Copyright 2019 metaphore
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.crashinvaders.vfx.framebuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

import de.damios.guacamole.gdx.graphics.NestableFrameBuffer;
import de.damios.guacamole.gdx.graphics.QuadMeshGenerator;
import de.damios.guacamole.gdx.graphics.ShaderCompatibilityHelper;

/**
 * Simple renderer that is capable of drawing a {@link FrameBuffer}'s texture
 * onto the screen or into another buffer.
 * <p>
 * This is a lightweight {@link com.badlogic.gdx.graphics.g2d.SpriteBatch}
 * replacement for the library's needs.
 */
public class VfxFrameBufferRenderer implements Disposable {

    // @formatter:off
    private static final String VERT_SHADER = 
                      "#ifdef GL_ES\n" 
                    + "    #define PRECISION mediump\n"
                    + "    precision PRECISION float;\n" 
                    + "#else\n"
                    + "    #define PRECISION\n" 
                    + "#endif\n"
                    + "attribute vec4 a_position;\n"
                    + "attribute vec2 a_texCoord0;\n"
                    + "varying vec2 v_texCoords;\n" 
                    + "void main() {\n"
                    + "    v_texCoords = a_texCoord0;\n"
                    + "    gl_Position = a_position;\n" 
                    + "}";
    private static final String FRAG_SHADER = 
                    "#ifdef GL_ES\n" 
                    + "    #define PRECISION mediump\n"
                    + "    precision PRECISION float;\n" 
                    + "#else\n"
                    + "    #define PRECISION\n" 
                    + "#endif\n"
                    + "varying vec2 v_texCoords;\n"
                    + "uniform sampler2D u_texture0;\n" 
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(u_texture0, v_texCoords);\n"
                    + "}";
    // @formatter:on

    private final Mesh mesh;
    private final ShaderProgram shader;

    public VfxFrameBufferRenderer() {
        mesh = QuadMeshGenerator.createQuad(-1, -1, 2, 2, true);

        shader = ShaderCompatibilityHelper.fromString(VERT_SHADER, FRAG_SHADER);

        shader.bind();
        shader.setUniformi("u_texture0", 0);
    }

    @Override
    public void dispose() {
        shader.dispose();
        mesh.dispose();
    }

    public void renderToScreen(NestableFrameBuffer srcBuf, int width,
            int height) {
        renderToScreen(srcBuf, 0, 0, width, height);
    }

    public void renderToScreen(NestableFrameBuffer srcBuf, int x, int y,
            int width, int height) {
        srcBuf.getColorBufferTexture().bind(0);

        // Update viewport to fit the area specified.
        Gdx.graphics.getGL20().glViewport(x, y, width, height);

        shader.bind();
        mesh.render(shader, GL20.GL_TRIANGLE_STRIP);
    }

    public void renderToFbo(NestableFrameBuffer srcBuf,
            NestableFrameBuffer dstBuf) {
        srcBuf.getColorBufferTexture().bind(0);

        dstBuf.begin();
        shader.bind();
        mesh.render(shader, GL20.GL_TRIANGLE_STRIP);
        dstBuf.end();
    }

    public Mesh getMesh() {
        return mesh;
    }
}
