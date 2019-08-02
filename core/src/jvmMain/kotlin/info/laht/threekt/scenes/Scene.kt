package info.laht.threekt.scenes

import info.laht.threekt.cameras.Camera
import info.laht.threekt.core.Object3DImpl
import info.laht.threekt.materials.Material
import info.laht.threekt.math.Color
import info.laht.threekt.renderers.GLRenderTarget
import info.laht.threekt.renderers.GLRenderTargetCube
import info.laht.threekt.renderers.GLRenderer
import info.laht.threekt.textures.CubeTexture

class Scene: Object3DImpl() {

    var fog: _Fog? = null

    var overrideMaterial: Material? = null

    var autoUpdate = true

    var onBeforeRenderScene: ((GLRenderer, Scene, Camera, GLRenderTarget?) -> Unit)? = null

    internal var background: Background? = null

    fun setBackground(color: Int) {
        setBackground(Color(color))
    }

    fun setBackground(color: Color) {
        background = ColorBackground(color)
    }

    fun setBackground(texture: CubeTexture) {
        background = TextureBackground(texture)
    }

    fun setBackground(renderTarget: GLRenderTargetCube) {
        background = RenderTargetBackGround(renderTarget)
    }

    fun copy( source: Scene, recursive: Boolean ): Scene {

        super.copy(source, recursive)

        source.background?.also { background = it }
        source.fog?.also { fog = it.clone() }
        source.overrideMaterial?.also { overrideMaterial = it.clone() }

        autoUpdate = source.autoUpdate

        return this

    }

    fun dispose() {
        dispatchEvent("dispose", this)
    }

}

internal sealed class Background

internal class ColorBackground(
        val color: Color
): Background()

internal class TextureBackground(
        val texture: CubeTexture
): Background()

internal class RenderTargetBackGround(
        val renderTarget: GLRenderTargetCube
): Background()
