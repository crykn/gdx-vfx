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

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import de.damios.guacamole.Preconditions;
import de.damios.guacamole.gdx.graphics.NestableFrameBuffer;

/**
 * Provides looped access to an array of {@link NestableFrameBuffer}s.
 */
public class VfxFrameBufferQueue implements Disposable {
    private final Array<NestableFrameBuffer> buffers;
    private int currentIdx = 0;

    private boolean depth;
    private TextureWrap wrapU = TextureWrap.ClampToEdge;
    private TextureWrap wrapV = Texture.TextureWrap.ClampToEdge;
    private TextureFilter filterMin = TextureFilter.Nearest;
    private TextureFilter filterMag = TextureFilter.Nearest;

    public VfxFrameBufferQueue(int width, int height, boolean depth,
            int fboCount) {
        Preconditions.checkArgument(fboCount >= 1,
                "The number of fbos needs to be at least 1");

        this.depth = depth;

        buffers = new Array<>(true, fboCount);
        for (int i = 0; i < fboCount; i++) {
            buffers.add(new NestableFrameBuffer(Format.RGBA8888, width, height,
                    depth));
        }
    }

    @Override
    public void dispose() {
        for (int i = 0; i < buffers.size; i++) {
            buffers.get(i).dispose();
        }
        buffers.clear();
    }

    public void resize(int width, int height) {
        int amount = buffers.size;

        for (int i = 0; i < amount; i++) {
            buffers.get(i).dispose();
        }
        buffers.clear();

        for (int i = 0; i < amount; i++) {
            buffers.add(new NestableFrameBuffer(Format.RGBA8888, width, height,
                    depth));
        }
    }

    /**
     * Restores buffer OpenGL parameters. Could be useful in case of OpenGL
     * context loss.
     */
    public void rebind() {
        for (int i = 0; i < buffers.size; i++) {
            NestableFrameBuffer fbo = buffers.get(i);

            Texture texture = fbo.getColorBufferTexture();
            texture.setWrap(wrapU, wrapV);
            texture.setFilter(filterMin, filterMag);
        }
    }

    public NestableFrameBuffer getCurrent() {
        return buffers.get(currentIdx);
    }

    public NestableFrameBuffer changeToNext() {
        currentIdx = (currentIdx + 1) % buffers.size;
        return getCurrent();
    }

    public void setTextureParams(Texture.TextureWrap u, Texture.TextureWrap v,
            Texture.TextureFilter min, Texture.TextureFilter mag) {
        wrapU = u;
        wrapV = v;
        filterMin = min;
        filterMag = mag;
        rebind();
    }
}
