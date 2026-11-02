package dev.xardas.nekoroqol.mixin.client;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.xardas.nekoroqol.NekoroQol;
import dev.xardas.nekoroqol.QolConfig;

/**
 * Fullscreen as a borderless window covering the whole monitor: alt-tab and a second monitor work without
 * the game minimizing. Vanilla first puts the window on the monitor, then it becomes a borderless window;
 * leaving goes back over the monitor too. Moving straight between a normal and a borderless window only
 * worked the first time on Windows (the window then kept its old place and size).
 */
@Mixin(Window.class)
public abstract class WindowMixin {
    @Shadow @Final private long window;
    @Shadow @Final private ScreenManager screenManager;
    @Shadow private boolean fullscreen;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;

    @Unique private boolean nekoroQol$borderless;
    @Unique private int[] nekoroQol$windowed;

    @Shadow
    private void refreshFramebufferSize() {
        throw new AssertionError();
    }

    @Unique
    private boolean nekoroQol$enabled() {
        return QolConfig.get().borderlessFullscreen && !Minecraft.ON_OSX;
    }

    @Inject(method = "setMode", at = @At("HEAD"))
    private void nekoroQol$beforeSetMode(CallbackInfo ci) {
        // vanilla saves the normal window's place whenever the window is not on a monitor, which a borderless one is not
        nekoroQol$windowed = nekoroQol$borderless ? new int[] {windowedX, windowedY, windowedWidth, windowedHeight} : null;
        if (nekoroQol$borderless && (!fullscreen || !nekoroQol$enabled())) {
            GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
            Monitor monitor = screenManager.findBestMonitor((Window) (Object) this);
            if (monitor != null) {
                VideoMode mode = monitor.getCurrentMode();
                GLFW.glfwSetWindowMonitor(window, monitor.getMonitor(), 0, 0, mode.getWidth(), mode.getHeight(), mode.getRefreshRate());
            }
            nekoroQol$borderless = false;
        }
    }

    @Inject(method = "setMode", at = @At("TAIL"))
    private void nekoroQol$afterSetMode(CallbackInfo ci) {
        if (!fullscreen || !nekoroQol$enabled()) return;
        Monitor monitor = screenManager.findBestMonitor((Window) (Object) this);
        if (monitor == null) return;
        if (nekoroQol$windowed != null) {
            windowedX = nekoroQol$windowed[0];
            windowedY = nekoroQol$windowed[1];
            windowedWidth = nekoroQol$windowed[2];
            windowedHeight = nekoroQol$windowed[3];
        }
        VideoMode mode = monitor.getCurrentMode();
        x = monitor.getX();
        y = monitor.getY();
        width = mode.getWidth();
        height = mode.getHeight();
        GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowMonitor(window, 0L, x, y, width, height, GLFW.GLFW_DONT_CARE);
        refreshFramebufferSize();
        nekoroQol$borderless = true;
        nekoroQol$log(monitor, mode);
    }

    /** What the window really became, for finding problems in logs/latest.log. */
    @Unique
    private void nekoroQol$log(Monitor monitor, VideoMode mode) {
        int[] wx = new int[1], wy = new int[1], ww = new int[1], wh = new int[1], fw = new int[1], fh = new int[1];
        GLFW.glfwGetWindowPos(window, wx, wy);
        GLFW.glfwGetWindowSize(window, ww, wh);
        GLFW.glfwGetFramebufferSize(window, fw, fh);
        NekoroQol.LOGGER.info("Borderless fullscreen: monitor {},{} {}x{}, window {},{} {}x{}, framebuffer {}x{}",
                monitor.getX(), monitor.getY(), mode.getWidth(), mode.getHeight(), wx[0], wy[0], ww[0], wh[0], fw[0], fh[0]);
    }
}
