![gdx-vfx Logo](https://i.imgur.com/kVBGQHx.png)

[![](https://jitpack.io/v/crykn/gdx-vfx.svg)](https://jitpack.io/#crykn/gdx-vfx) [![Build Status](https://travis-ci.com/crykn/gdx-vfx.svg?branch=master)](https://travis-ci.com/crykn/gdx-vfx)

**This is a fork of [gdx-vfx](https://github.com/crashinvaders/gdx-vfx), a flexible post-processing library for libGDX. The main changes in this fork are:**

- An update to libGDX 1.9.13
- Support for OpenGL 3 on macOS
- Support for depth
- Uses `NestableFrameBuffer`s from [guacamole](https://github.com/crykn/guacamole) instead of `VfxFrameBuffer`s; removes the coupled `Renderer`s
- `beginInputCapture()` -> `beginCapture()`, `endInputCapture()` -> `endCapture()`, `cleanUpBuffers()` -> `clear()`, `anyEnabledEffects()` -> `hasEffects()` 
- Heavily refactors a lot of the internal classes in the library

Gdx-vfx itself is based on [libgdx-contribs-postprocessing](https://github.com/manuelbua/libgdx-contribs/tree/master/postprocessing), with lots of improvements and heavy refactoring. The goal is to focus on stability, offer lightweight integration and provide a simple mechanism to implement effects.

Read more about the library at the [wiki introduction page](https://github.com/crashinvaders/gdx-vfx/wiki/Library-overview).

# Demo

Visit https://crashinvaders.github.io/gdx-vfx

Or clone and play with the demo locally:
```
git clone https://github.com/crashinvaders/gdx-vfx.git
cd gdx-vfx
./gradlew demo:desktop:run
```

![Demo GIF](https://imgur.com/dCsVhoo.gif)

# How to use

### 1. Add the library to the project

#### Gradle dependency
The library's releases are available through Jitpack.

Add it in your root `build.gradle` at the end of repositories:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Add the dependency:
```gradle
dependencies {
    implementation "com.github.crykn.gdx-vfx:gdx-vfx-core:$vfxVersion"
    implementation "com.github.crykn.gdx-vfx:gdx-vfx-effects:$vfxVersion"    // Optional, if you need standard filter/effects.
}
```

#### HTML/GWT support
The library is fully HTML/GWT compatible, but requires an extra dependency to be included in the GWT module in order to work properly.  
Please take a look at the [GWT integration guide](https://github.com/crashinvaders/gdx-vfx/wiki/GWT-HTML-Library-Integration).
```gradle
dependencies {
    implementation "com.github.crykn.gdx-vfx:gdx-vfx-gwt:$vfxVersion"
}
```

### 2. Sample code

A simple example of a LibGDX application that applies gaussian blur effect to a geometry drawn with `ShapeRenderer`.

```java
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.GaussianBlurEffect;

public class VfxExample extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private VfxManager vfxManager;
    private GaussianBlurEffect vfxEffect;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();

        // VfxManager is a host for the effects.
        // It captures rendering into internal off-screen buffer and applies a chain of defined effects.
        vfxManager = new VfxManager();

        // Create and add an effect.
        // VfxEffect derivative classes serve as controllers for the effects.
        // They provide public properties to configure and control them.
        vfxEffect = new GaussianBlurEffect();
        vfxManager.addEffect(vfxEffect);
    }

    @Override
    public void resize(int width, int height) {
        // VfxManager manages internal off-screen buffers,
        // which should always match the required viewport (whole screen in our case).
        vfxManager.resize(width, height);

        shapeRenderer.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
        shapeRenderer.updateMatrices();
    }

    @Override
    public void render() {
        // Clean up the screen.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Clean up internal buffers, as we don't need any information from the last render.
        vfxManager.clear();

        // Begin render to an off-screen buffer.
        vfxManager.beginCapture();

        // Here's where game render should happen.
        // For demonstration purposes we just render some simple geometry.
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.PINK);
        shapeRenderer.rect(250f, 100f, 250f, 175f);
        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.circle(200f, 250f, 100f);
        shapeRenderer.end();

        // End render to an off-screen buffer.
        vfxManager.endCapture();
        
        vfxManager.update(Gdx.graphics.getDeltaTime());

        // Apply the effects chain to the captured frame.
        // In our case, only one effect (gaussian blur) will be applied.
        vfxManager.applyEffects();

        // Render result to the screen.
        vfxManager.renderToScreen();
    }

    @Override
    public void dispose() {
        // Since VfxManager has internal frame buffers,
        // it implements Disposable interface and thus should be utilized properly.
        vfxManager.dispose();

        // *** PLEASE NOTE ***
        // VfxManager doesn't dispose attached VfxEffects.
        // This is your responsibility to manage their lifecycle.
        vfxEffect.dispose();

        shapeRenderer.dispose();
    }
}
``` 

![Result](https://i.imgur.com/XjBynGw.png)

_The actual example code can be found [here](https://github.com/crashinvaders/gdx-vfx/blob/master/demo/core/src/com/crashinvaders/vfx/demo/screens/example/VfxExample.java)._
