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

package com.crashinvaders.vfx.gl;

import java.nio.ByteBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;

import de.damios.guacamole.Preconditions;
import de.damios.guacamole.gdx.graphics.ShaderCompatibilityHelper;
import de.damios.guacamole.gdx.log.Logger;
import de.damios.guacamole.gdx.log.LoggerService;

public class VfxGLUtils {

    private static final Logger LOG = LoggerService.getLogger(VfxGLUtils.class);
    private static final ByteBuffer tmpByteBuffer = BufferUtils
            .newByteBuffer(32);

    public static ShaderProgram compileShader(FileHandle vertexFile,
            FileHandle fragmentFile) {
        return compileShader(vertexFile, fragmentFile, "");
    }

    public static ShaderProgram compileShader(FileHandle vertexFile,
            FileHandle fragmentFile, String defines) {
        Preconditions.checkNotNull(fragmentFile,
                "Vertex shader file cannot be null.");
        Preconditions.checkNotNull(vertexFile,
                "Fragment shader file cannot be null.");
        Preconditions.checkNotNull(defines, "Defines cannot be null.");

        StringBuilder sb = new StringBuilder();
        sb.append("Compiling \"").append(vertexFile.name()).append('/')
                .append(fragmentFile.name()).append('\"');
        if (defines.length() > 0) {
            sb.append(" w/ (").append(defines.replace("\n", ", ")).append(")");
        }
        sb.append("...");
        LOG.debug(sb.toString());

        String srcVert = vertexFile.readString();
        String srcFrag = fragmentFile.readString();

        return ShaderCompatibilityHelper.fromString(defines + "\n" + srcVert,
                defines + "\n" + srcFrag);
    }

    // region GL state queries

    /** Enable pipeline state queries: beware the pipeline can stall! */
    public static boolean enableGLQueryStates = false;

    /**
     * Provides a simple mechanism to query OpenGL pipeline states. Note: state
     * queries are costly and stall the pipeline, especially on mobile devices!
     * <br/>
     * Queries switched off by default. Update {@link #enableGLQueryStates} flag
     * to enable them.
     */
    public static boolean isGLEnabled(int pName) {
        if (!enableGLQueryStates)
            return false;

        boolean result;

        switch (pName) {
        case GL20.GL_BLEND:
            Gdx.gl20.glGetBooleanv(GL20.GL_BLEND, tmpByteBuffer);
            result = (tmpByteBuffer.get() == 1);
            tmpByteBuffer.clear();
            break;
        default:
            result = false;
        }

        return result;
    }
    // endregion
}
