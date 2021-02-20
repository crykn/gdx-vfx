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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

import de.damios.guacamole.Preconditions;
import de.damios.guacamole.gdx.graphics.NestableFrameBuffer;

/**
 * Encapsulates a pair of {@link NestableFrameBuffer}s with the ability to swap
 * between them.
 * <p>
 *
 * Upon {@link #begin()} the buffer is reset to a known initial state, this is
 * usually done just before the first usage of the buffer. Subsequent
 * {@link #swap()} calls will initiate writing to the next available buffer,
 * effectively ping-ponging between the two. Chained rendering will be possible
 * by retrieving the necessary buffers via {@link #getSrcBuffer()} or
 * {@link #getDstBuffer()}. <br/>
 * When rendering is finished, {@link #end()} should be called to stop
 * capturing.
 *
 * @author metaphore
 */
public class VfxPingPongWrapper implements Disposable {

    protected NestableFrameBuffer bufDst;
    protected NestableFrameBuffer bufSrc;

    /**
     * Where capturing is started. Should be true between {@link #begin()} and
     * {@link #end()}.
     */
    protected boolean capturing;

    public VfxPingPongWrapper(NestableFrameBuffer bufDst,
            NestableFrameBuffer bufSrc) {
        this.bufSrc = bufSrc;
        this.bufDst = bufDst;
    }

    /**
     * Start capturing into the destination buffer. To swap buffers during
     * capturing, call {@link #swap()}. {@link #end()} shall be called after
     * rendering to ping-pong buffer is done.
     */
    public void begin() {
        Preconditions.checkState(!capturing,
                "Ping pong buffer is already in capturing state.");
        capturing = true;
        bufDst.begin();
    }

    /**
     * Finishes ping-ponging. Must be called after {@link #begin()}.
     **/
    public void end() {
        Preconditions.checkState(capturing,
                "Ping pong is not in capturing state. You must call begin() before calling end().");
        bufDst.end();
        capturing = false;
    }

    /**
     * Swaps source/target buffers. May be called outside of capturing state.
     */
    public void swap() {
        if (capturing) {
            bufDst.end();
        }

        // Swap buffers
        NestableFrameBuffer tmp = this.bufDst;
        bufDst = bufSrc;
        bufSrc = tmp;

        if (capturing) {
            bufDst.begin();
        }
    }

    public boolean isCapturing() {
        return capturing;
    }

    /** @return the source buffer of the current ping-pong chain. */
    public NestableFrameBuffer getSrcBuffer() {
        return bufSrc;
    }

    /** @return Returns the result's buffer of the latest {@link #swap()}. */
    public NestableFrameBuffer getDstBuffer() {
        return bufDst;
    }

    /**
     * Cleans up managed {@link NestableFrameBuffer}s with the color specified.
     */
    public void clear(Color clearColor) {
        clear(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
    }

    /**
     * Cleans up managed {@link NestableFrameBuffer}s with the color specified.
     */
    public void clear(float r, float g, float b, float a) {
        final boolean wasCapturing = this.capturing;

        if (!wasCapturing) {
            begin();
        }

        ScreenUtils.clear(r, g, b, a, true);
        swap();
        ScreenUtils.clear(r, g, b, a, true);

        if (!wasCapturing) {
            end();
        }
    }

    public void resize(int bufferWidth, int bufferHeight) {
        boolean hasDepth = bufSrc.hasDepth();
        bufSrc.dispose();
        bufSrc = new NestableFrameBuffer(Format.RGBA8888, bufferWidth,
                bufferHeight, hasDepth);

        bufDst.dispose();
        bufDst = new NestableFrameBuffer(Format.RGBA8888, bufferWidth,
                bufferHeight, hasDepth);
    }

    @Override
    public void dispose() {
        if (bufSrc != null)
            bufSrc.dispose();
        if (bufDst != null)
            bufDst.dispose();
    }
}