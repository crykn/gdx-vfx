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

package com.crashinvaders.vfx;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.crashinvaders.vfx.framebuffer.VfxFrameBufferPool;
import com.crashinvaders.vfx.framebuffer.VfxFrameBufferRenderer;

public class VfxRenderContext implements Disposable {

    private final VfxFrameBufferPool bufferPool;
    private final VfxFrameBufferRenderer bufferRenderer;

    private int bufferWidth;
    private int bufferHeight;

    public VfxRenderContext(int bufferWidth, int bufferHeight,
            boolean hasDepth) {
        this.bufferPool = new VfxFrameBufferPool(Format.RGBA8888, bufferWidth,
                bufferHeight, hasDepth, 4, Texture.TextureWrap.ClampToEdge,
                Texture.TextureWrap.ClampToEdge, Texture.TextureFilter.Nearest,
                Texture.TextureFilter.Nearest);
        this.bufferRenderer = new VfxFrameBufferRenderer();
        this.bufferWidth = bufferWidth;
        this.bufferHeight = bufferHeight;
    }

    @Override
    public void dispose() {
        bufferPool.dispose();
        bufferRenderer.dispose();
    }

    public void resize(int bufferWidth, int bufferHeight) {
        this.bufferWidth = bufferWidth;
        this.bufferHeight = bufferHeight;
        this.bufferPool.resize(bufferWidth, bufferHeight);
    }

    public VfxFrameBufferPool getBufferPool() {
        return bufferPool;
    }

    public VfxFrameBufferRenderer getBufferRenderer() {
        return bufferRenderer;
    }

    public Mesh getViewportMesh() {
        return bufferRenderer.getMesh();
    }

    public int getBufferWidth() {
        return bufferWidth;
    }

    public int getBufferHeight() {
        return bufferHeight;
    }
}