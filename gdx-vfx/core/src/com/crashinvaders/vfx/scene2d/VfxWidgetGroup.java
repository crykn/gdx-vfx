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

package com.crashinvaders.vfx.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.VfxEffect;

import de.damios.guacamole.gdx.graphics.NestableFrameBuffer;

/**
 * A widget group that manages {@link VfxManager} internally and applies the
 * effects to the child actors.
 * <p>
 * All the internal VFX related instances are managed by the widget itself and
 * should not be disposed or resized manually. However you're still responsible
 * for all the {@link VfxEffect effects}' lifecycle and shall dispose them as
 * usual.
 * <p>
 * While working with VFX effects within {@link Stage}'s actor hierarchy, keep
 * in mind that not every effect is made to support transparency, so there might
 * be issues.
 */
public class VfxWidgetGroup extends WidgetGroup {

    private final VfxManager vfxManager;
    private boolean initialized = false;
    private boolean resizePending = false;

    /**
     * If true, the internal {@link VfxManager} will be resized to match
     * {@link VfxWidgetGroup}'s size. Means framebuffer pixels will correspond
     * to the virtual units of stage's viewport.
     */
    private boolean matchWidgetSize = false;

    /**
     * Whether internal {@link VfxManager} instance should be updated with
     * {@link com.badlogic.gdx.scenes.scene2d.Actor#act(float)} calls.
     */
    private boolean updateManager = true;

    public VfxWidgetGroup() {
        vfxManager = new VfxManager();
        super.setTransform(false);
    }

    public VfxManager getVfxManager() {
        return vfxManager;
    }

    /** @see #matchWidgetSize */
    public boolean isMatchWidgetSize() {
        return matchWidgetSize;
    }

    /** @see #matchWidgetSize */
    public void setMatchWidgetSize(boolean matchWidgetSize) {
        if (this.matchWidgetSize == matchWidgetSize)
            return;

        this.matchWidgetSize = matchWidgetSize;
        resizePending = true;
    }

    /** @see #updateManager */
    public boolean isUpdateManager() {
        return updateManager;
    }

    /** @see #updateManager */
    public void setUpdateManager(boolean updateManager) {
        this.updateManager = updateManager;
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);

        if (stage != null) {
            initialize();
        } else {
            reset();
        }
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        resizePending = true;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (updateManager) {
            // Update effects chain.
            vfxManager.update(delta);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();

        NestableFrameBuffer captureBuffer = vfxManager.getResultBuffer();

        batch.end();

        performPendingResize();

        vfxManager.clear();

        vfxManager.beginCapture();

        batch.begin();

        validate();
        drawChildren(batch, parentAlpha);

        batch.end();

        vfxManager.endCapture();

        vfxManager.applyEffects();

        batch.begin();

        // Render result to the screen.
        batch.setColor(getColor().r, getColor().g, getColor().b,
                getColor().a * parentAlpha);
        batch.draw(vfxManager.getResultBuffer().getColorBufferTexture(), getX(),
                getY(), getWidth(), getHeight(), 0f, 0f, 1f, 1f);
    }

    @Override
    protected void drawChildren(Batch batch, float parentAlpha) {
        boolean capturing = vfxManager.isCapturing();

        if (capturing) {
            // Imitate "transform" child drawing for when capturing into
            // VfxManager.
            super.setTransform(true);
        }
        if (!capturing) {
            // Clip children to VfxWidget area when not capturing into FBO.
            clipBegin();
        }

        super.drawChildren(batch, parentAlpha);
        batch.flush();

        if (capturing) {
            super.setTransform(false);
        }

        if (!capturing) {
            clipEnd();
        }
    }

    /**
     * VfxWidgetGroup doesn't support culling area. Any calls to it will be
     * ignored.
     */
    @Deprecated
    @Override
    public void setCullingArea(Rectangle cullingArea) {
        throw new UnsupportedOperationException(
                "VfxWidgetGroup doesn't support culling area.");
    }

    /**
     * VfxWidgetGroup doesn't support transform. Any calls to the method be
     * ignored.
     */
    @Deprecated
    @Override
    public void setTransform(boolean transform) {
        throw new UnsupportedOperationException(
                "VfxWidgetGroup doesn't support transform.");
    }

    private void initialize() {
        if (initialized)
            return;

        performPendingResize();

        resizePending = false;
        initialized = true;
    }

    private void reset() {
        if (!initialized)
            return;

        vfxManager.dispose();

        resizePending = false;
        initialized = false;
    }

    private void performPendingResize() {
        if (!resizePending)
            return;

        final int width;
        final int height;

        // Size may be zero if the widget wasn't laid out yet.
        if ((int) getWidth() == 0 || (int) getHeight() == 0) {
            // If the size of the widget is not defined,
            // just resize to a small buffer to keep the memory footprint low.
            width = 16;
            height = 16;

        } else if (matchWidgetSize) {
            // Set buffer to match the size of the widget.
            width = MathUtils.floor(getWidth());
            height = MathUtils.floor(getHeight());

        } else {
            // Set buffer to match the screen pixel density.
            Viewport viewport = getStage().getViewport();
            float ppu = viewport.getScreenWidth() / viewport.getWorldWidth();
            width = MathUtils.floor(getWidth() * ppu);
            height = MathUtils.floor(getHeight() * ppu);
        }

        vfxManager.resize(width, height);

        resizePending = false;
    }

}
