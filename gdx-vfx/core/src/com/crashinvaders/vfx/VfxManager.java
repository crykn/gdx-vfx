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

import java.util.function.Predicate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

import de.damios.guacamole.Preconditions;
import de.damios.guacamole.gdx.graphics.NestableFrameBuffer;

/**
 * Handles post processing effects. Provides a way to beginCapture the rendered
 * scene to an off-screen buffer and to apply a chain of effects on it before
 * rendering to screen.
 * <p>
 * Effects can be added or removed via {@link #addEffect(ChainVfxEffect)} and
 * {@link #removeEffect(ChainVfxEffect)}.
 *
 * @author metaphore
 */
public final class VfxManager implements Disposable {

    private static final Vector2 tmpVec = new Vector2();
    private final Array<ChainVfxEffect> tmpArray = new Array<>();

    private final ObjectIntMap<ChainVfxEffect> priorities = new ObjectIntMap<>();
    private final Array<ChainVfxEffect> allEffects = new Array<>();

    private final VfxRenderContext context;

    private final VfxPingPongWrapper pingPongWrapper;

    private boolean capturing = false;
    private boolean disabled = false;

    private boolean applyingEffects = false;

    private boolean blendingEnabled = false;

    private int width, height;

    public VfxManager() {
        this(Gdx.graphics.getBackBufferWidth(),
                Gdx.graphics.getBackBufferHeight(), false);
    }

    public VfxManager(int bufferWidth, int bufferHeight, boolean hasDepth) {
        this.width = bufferWidth;
        this.height = bufferHeight;

        this.context = new VfxRenderContext(bufferWidth, bufferHeight,
                hasDepth);
        this.pingPongWrapper = new VfxPingPongWrapper(
                context.getBufferPool().obtain(),
                context.getBufferPool().obtain());
    }

    @Override
    public void dispose() {
        pingPongWrapper.dispose();
        context.dispose();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isCapturing() {
        return capturing;
    }

    public boolean isDisabled() {
        return disabled;
    }

    /** Sets whether or not the post-processor should be disabled */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isBlendingEnabled() {
        return blendingEnabled;
    }

    /**
     * Enables OpenGL blending for the effect chain rendering stage. Disabled by
     * default.
     */
    public void setBlendingEnabled(boolean blendingEnabled) {
        this.blendingEnabled = blendingEnabled;
    }

    public boolean isApplyingEffects() {
        return applyingEffects;
    }

    /** @return the last active destination frame buffer. */
    public NestableFrameBuffer getResultBuffer() {
        return pingPongWrapper.getDstBuffer();
    }

    /** @return the internal ping-pong buffer. */
    public VfxPingPongWrapper getPingPongWrapper() {
        return pingPongWrapper;
    }

    public VfxRenderContext getRenderContext() {
        return context;
    }

    /**
     * Adds an effect to the effect chain and transfers ownership to the
     * VfxManager. The order of the inserted effects IS important, since effects
     * will be applied in a FIFO fashion, the first added is the first being
     * applied.
     * <p>
     * For more control over the order supply the effect with a priority -
     * {@link #addEffect(ChainVfxEffect, int)}.
     * 
     * @see #addEffect(ChainVfxEffect, int)
     */
    public void addEffect(ChainVfxEffect effect) {
        addEffect(effect, 0);
    }

    public void addEffect(ChainVfxEffect effect, int priority) {
        allEffects.add(effect);
        priorities.put(effect, priority);
        allEffects.sort((e1, e2) -> Integer.compare(priorities.get(e1, 0),
                priorities.get(e2, 0)));
        effect.resize(width, height);
    }

    /** Removes the specified effect from the effect chain. */
    public void removeEffect(ChainVfxEffect effect) {
        allEffects.removeValue(effect, false);
        priorities.remove(effect, 0);
    }

    /** Removes all effects from the effect chain. */
    public void removeAllEffects() {
        allEffects.clear();
        priorities.clear();
    }

    /** Changes the order of the effect in the effect chain. */
    public void setEffectPriority(ChainVfxEffect effect, int priority) {
        priorities.put(effect, priority);
        allEffects.sort((e1, e2) -> Integer.compare(priorities.get(e1, 0),
                priorities.get(e2, 0)));
    }

    /**
     * Cleans up the {@link VfxPingPongWrapper}'s buffers with
     * {@link Color#CLEAR}.
     */
    public void clear() {
        clear(Color.CLEAR);
    }

    /**
     * Cleans up the {@link VfxPingPongWrapper}'s buffers with the color
     * specified.
     */
    public void clear(Color color) {
        Preconditions.checkState(!applyingEffects,
                "Cannot clear when applying effects.");
        Preconditions.checkState(!capturing, "Cannot clear when capturing.");

        pingPongWrapper.clear(color);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;

        context.resize(width, height);

        for (int i = 0; i < allEffects.size; i++) {
            allEffects.get(i).resize(width, height);
        }
    }

    public void update(float delta) {
        for (int i = 0; i < allEffects.size; i++) {
            allEffects.get(i).update(delta);
        }
    }

    /** Starts capturing the input buffer. */
    public void beginCapture() {
        Preconditions.checkState(!applyingEffects,
                "Capture is not available when VfxManager is applying the effects.");

        if (capturing)
            return;

        capturing = true;
        pingPongWrapper.begin();
    }

    /** Stops capturing the input buffer. */
    public void endCapture() {
        Preconditions.checkState(capturing,
                "The capturing is not started. Did you forget to call #beginInputCapture()?");

        capturing = false;
        pingPongWrapper.end();
    }

    public void useAsInput(NestableFrameBuffer fbo) {
        Preconditions.checkState(!capturing,
                "Cannot set captured input when capture helper is currently capturing.");
        Preconditions.checkState(!applyingEffects,
                "Cannot update the input buffer when applying effects.");

        context.getBufferRenderer().renderToFbo(fbo,
                pingPongWrapper.getDstBuffer());
    }

    /** Applies the effect chain. */
    public void applyEffects() {
        Preconditions.checkState(!capturing,
                "You must call endCapture() before applying the effects.");

        if (disabled)
            return;

        selectFrom(tmpArray, allEffects, e -> !e.isDisabled());

        if (tmpArray.size == 0) {
            return;
        }

        applyingEffects = true;

        // Enable blending to preserve buffer's alpha values.
        if (blendingEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }

        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        pingPongWrapper.swap(); // Swap buffers to get the input buffer in the
                                // src buffer.
        pingPongWrapper.begin();

        // Render the effect chain.
        for (int i = 0; i < tmpArray.size; i++) {
            ChainVfxEffect effect = tmpArray.get(i);
            effect.render(context, pingPongWrapper);
            if (i < tmpArray.size - 1) {
                pingPongWrapper.swap();
            }
        }
        pingPongWrapper.end();

        // Ensure default texture unit #0 is active.
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        if (blendingEnabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        applyingEffects = false;
    }

    public void renderToScreen() {
        Preconditions.checkState(!capturing,
                "You must call endCapture() before rendering the result.");

        // Enable blending to preserve buffer's alpha values.
        if (blendingEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }
        context.getBufferRenderer().renderToScreen(
                pingPongWrapper.getDstBuffer(), context.getBufferWidth(),
                context.getBufferHeight());
        if (blendingEnabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    public void renderToScreen(int x, int y, int width, int height) {
        Preconditions.checkState(!capturing,
                "You must call endCapture() before rendering the result.");

        // Enable blending to preserve buffer's alpha values.
        if (blendingEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }
        context.getBufferRenderer().renderToScreen(
                pingPongWrapper.getDstBuffer(), x, y, width, height);
        if (blendingEnabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    public void renderToFbo(NestableFrameBuffer output) {
        Preconditions.checkState(!capturing,
                "You must call endCapture() before rendering the result.");

        // Enable blending to preserve buffer's alpha values.
        if (blendingEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }
        context.getBufferRenderer().renderToFbo(pingPongWrapper.getDstBuffer(),
                output);
        if (blendingEnabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    public boolean hasEffects() {
        for (int i = 0; i < allEffects.size; i++) {
            if (!allEffects.get(i).isDisabled()) {
                return true;
            }
        }
        return false;
    }

    private static <T> Array<T> selectFrom(final Array<T> ret,
            final Iterable<T> from, final Predicate<T> predicate) {
        ret.clear();
        from.forEach(t -> {
            if (predicate.test(t)) {
                ret.add(t);
            }
        });
        return ret;
    }

}