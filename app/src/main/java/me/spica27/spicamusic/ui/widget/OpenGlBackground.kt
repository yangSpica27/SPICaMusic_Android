package me.spica27.spicamusic.ui.widget

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Size
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** All callbacks run on the same GL thread, with its EGL context current. */
internal interface OpenGlBackgroundRenderer {
    fun onSurfaceCreated()

    fun onSurfaceChanged(
        width: Int,
        height: Int,
    )

    fun onDrawFrame()
}

/** TextureView participates in the source layer captured by Haze, including dialog windows. */
@Composable
internal fun OpenGlBackground(
    renderer: OpenGlBackgroundRenderer,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    key(renderer) {
        val frameVersion = remember { mutableIntStateOf(0) }
        AndroidView(
            modifier =
                modifier.drawWithContent {
                    // Observe frames in Draw only. Haze must recapture even without recomposition.
                    frameVersion.intValue
                    drawContent()
                },
            factory = { context ->
                OpenGlTextureView(context, renderer) { frameVersion.intValue++ }
            },
            update = { it.updateLifecycle(lifecycle, enabled) },
            onRelease = { it.release() },
        )
    }
}

internal class OpenGlTextureView(
    context: Context,
    private val renderer: OpenGlBackgroundRenderer,
    private val onFrameUpdated: () -> Unit,
) : TextureView(context),
    TextureView.SurfaceTextureListener,
    Choreographer.FrameCallback {
    // One executor serializes old-surface cleanup and new-surface initialization for this renderer.
    private val renderExecutor =
        Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Background-GL") }
    private val choreographer = Choreographer.getInstance()
    private var session: RenderSession? = null
    private var lifecycle: Lifecycle? = null
    private var enabled = false
    private var released = false
    private var frameScheduled = false
    private val lifecycleObserver = LifecycleEventObserver { _, _ -> updateRendering() }

    init {
        isOpaque = true
        surfaceTextureListener = this
    }

    fun updateLifecycle(
        lifecycle: Lifecycle,
        enabled: Boolean,
    ) {
        if (released) return
        this.enabled = enabled
        if (this.lifecycle !== lifecycle) {
            this.lifecycle?.removeObserver(lifecycleObserver)
            this.lifecycle = lifecycle
            lifecycle.addObserver(lifecycleObserver)
        }
        updateRendering()
    }

    private fun updateRendering() {
        // A dialog caps the underlying screen at STARTED; that screen is still visible.
        session?.enabled =
            !released &&
            enabled &&
            lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
        scheduleFrame()
    }

    private fun scheduleFrame() {
        if (session?.canRender == true) {
            if (!frameScheduled) {
                frameScheduled = true
                choreographer.postFrameCallback(this)
            }
        } else {
            choreographer.removeFrameCallback(this)
            frameScheduled = false
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        frameScheduled = false
        val currentSession = session ?: return
        if (!currentSession.canRender) return
        // Never queue more than one frame when GL is slower than the display refresh rate.
        if (currentSession.framePending.compareAndSet(false, true)) {
            renderExecutor.execute {
                try {
                    currentSession.drawFrame()
                } finally {
                    currentSession.framePending.set(false)
                }
            }
        }
        scheduleFrame()
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        if (released) return
        session = RenderSession(surface, renderer, Size(width, height))
        updateRendering()
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        session?.size = Size(width, height)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        if (!released) onFrameUpdated()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        val oldSession = session ?: return true
        oldSession.active = false
        session = null
        scheduleFrame()
        renderExecutor.execute {
            try {
                oldSession.release()
            } finally {
                surface.release()
            }
        }
        if (released) renderExecutor.shutdown()
        // Retain ownership until queued GL work ends; never block the UI thread waiting for EGL.
        return false
    }

    fun release() {
        if (released) return
        released = true
        lifecycle?.removeObserver(lifecycleObserver)
        lifecycle = null
        val oldSession = session
        oldSession?.active = false
        scheduleFrame()
        if (oldSession == null) {
            renderExecutor.shutdown()
        } else {
            renderExecutor.execute { oldSession.release() }
            // TextureView still owns the texture until onSurfaceTextureDestroyed, which shuts down
            // the executor after releasing it. onRelease may run before or after view detachment.
        }
    }
}

private class RenderSession(
    private val texture: SurfaceTexture,
    private val renderer: OpenGlBackgroundRenderer,
    @Volatile var size: Size,
) {
    @Volatile var active = true

    @Volatile var enabled = false

    @Volatile private var failed = false

    val framePending = AtomicBoolean(false)
    val canRender: Boolean get() = active && enabled && !failed

    // Only accessed by the GL executor. Pausing retains the context, textures and last frame.
    private var egl: EglWindow? = null
    private var renderedSize: Size? = null

    fun drawFrame() {
        if (!canRender) return
        try {
            val window =
                egl ?: EglWindow(texture).also {
                    egl = it
                    renderer.onSurfaceCreated()
                }
            val currentSize = size
            if (currentSize.width <= 0 || currentSize.height <= 0) return
            if (renderedSize != currentSize) {
                renderer.onSurfaceChanged(currentSize.width, currentSize.height)
                renderedSize = currentSize
            }
            renderer.onDrawFrame()
            if (!window.swapBuffers()) {
                val errorCode = EGL14.eglGetError()
                if (errorCode == EGL14.EGL_CONTEXT_LOST) {
                    // Rebuild GL resources on the next frame; renderers retain their CPU inputs.
                    release()
                } else {
                    error("eglSwapBuffers failed: 0x${errorCode.toString(16)}")
                }
            }
        } catch (exception: RuntimeException) {
            failed = true
            release()
            Timber.e(exception, "OpenGL background rendering failed")
        }
    }

    fun release() {
        egl?.release()
        egl = null
        renderedSize = null
    }
}

private class EglWindow(
    texture: SurfaceTexture,
) {
    private val surface = Surface(texture)
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var window: EGLSurface = EGL14.EGL_NO_SURFACE

    init {
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "No EGL display" }
            val versions = IntArray(2)
            check(EGL14.eglInitialize(display, versions, 0, versions, 1)) { "eglInitialize failed" }
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            val attributes =
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE,
                    EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_RED_SIZE,
                    8,
                    EGL14.EGL_GREEN_SIZE,
                    8,
                    EGL14.EGL_BLUE_SIZE,
                    8,
                    EGL14.EGL_ALPHA_SIZE,
                    8,
                    EGL14.EGL_DEPTH_SIZE,
                    0,
                    EGL14.EGL_STENCIL_SIZE,
                    0,
                    EGL14.EGL_NONE,
                )
            check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) && count[0] > 0) {
                "No RGBA8888 OpenGL ES 2 config"
            }
            val config = checkNotNull(configs[0])
            context =
                EGL14.eglCreateContext(
                    display,
                    config,
                    EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                    0,
                )
            check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed" }
            window = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
            check(window != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
            check(EGL14.eglMakeCurrent(display, window, window, context)) { "eglMakeCurrent failed" }
        } catch (exception: RuntimeException) {
            release()
            throw exception
        }
    }

    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(display, window)

    fun release() {
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (window != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, window)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
            EGL14.eglReleaseThread()
        }
        display = EGL14.EGL_NO_DISPLAY
        context = EGL14.EGL_NO_CONTEXT
        window = EGL14.EGL_NO_SURFACE
        surface.release()
    }
}
