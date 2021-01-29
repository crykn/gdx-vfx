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

import javax.annotation.Nullable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.DisposablePool;

import de.damios.guacamole.gdx.graphics.GLUtils;
import de.damios.guacamole.gdx.graphics.NestableFrameBuffer;

public class VfxFrameBufferPool extends DisposablePool<NestableFrameBuffer>
        implements Disposable {

    private int width;
    private int height;
    private Format format;
    private boolean hasDepth;

    private @Nullable TextureWrap textureWrapU;
    private @Nullable TextureWrap textureWrapV;
    private @Nullable TextureFilter textureFilterMin;
    private @Nullable TextureFilter textureFilterMag;

    public VfxFrameBufferPool(int width, int height) {
        this(Format.RGBA8888, width, height, false, 8);
    }

    public VfxFrameBufferPool(Format format, int width, int height,
            boolean hasDepth, int initialCapacity) {
        this(format, width, height, hasDepth, initialCapacity, null, null, null,
                null);
    }

    public VfxFrameBufferPool(Format format, int width, int height,
            boolean hasDepth, int initialCapacity, TextureWrap textureWrapU,
            TextureWrap textureWrapV, TextureFilter textureFilterMin,
            TextureFilter textureFilterMag) {
        super(initialCapacity);
        this.width = width;
        this.height = height;
        this.format = format;
        this.hasDepth = hasDepth;
        this.textureWrapU = textureWrapU;
        this.textureWrapV = textureWrapV;
        this.textureFilterMin = textureFilterMin;
        this.textureFilterMag = textureFilterMag;
    }

    @Override
    protected NestableFrameBuffer newObject() {
        NestableFrameBuffer fbo = new NestableFrameBuffer(format, width, height,
                hasDepth);

        boolean setWrap = textureWrapU != null && textureWrapV != null;
        boolean setFilter = textureFilterMin != null
                && textureFilterMag != null;

        if (setWrap || setFilter) {
            int boundHandle = GLUtils.getBoundFboHandle();
            Texture texture = fbo.getColorBufferTexture();
            if (setWrap)
                texture.setWrap(textureWrapU, textureWrapV);
            if (setFilter)
                texture.setFilter(textureFilterMin, textureFilterMag);

            fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest);
            Gdx.gl20.glBindFramebuffer(GL20.GL_FRAMEBUFFER, boundHandle);
        }

        return fbo;
    }

    public void resize(int width, int height) {
        if (this.width != width || this.height != height) {
            clear();
        }
    }

    @Override
    public void dispose() {
        this.clear();
    }

}
