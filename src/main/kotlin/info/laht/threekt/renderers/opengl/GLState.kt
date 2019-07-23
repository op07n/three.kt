package info.laht.threekt.renderers.opengl


import info.laht.threekt.*
import info.laht.threekt.core.BufferAttribute
import info.laht.threekt.materials.Material
import info.laht.threekt.math.Vector4
import info.laht.threekt.textures.Image
import info.laht.threekt.textures.Texture
import org.lwjgl.opengl.*
import java.nio.ByteBuffer
import kotlin.math.roundToInt


class GLState internal constructor() {

    val colorBuffer = GLColorBuffer().apply {
        setClear(0f, 0f, 0f, 1f)
    }

    val depthBuffer = GLDepthBuffer().apply {
        setClear(1.0)
    }

    val stencilBuffer = GLStencilBuffer().apply {
        setClear(0)
    }

    val maxVertexAttributes = GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS)
    val newAttributes = IntArray(maxVertexAttributes)
    val enabledAttributes = IntArray(maxVertexAttributes)
    val attributeDivisors = IntArray(maxVertexAttributes)

    private val enabledCapabilities = mutableMapOf<Int, Boolean>()

    var currentProgram: Int? = null

    var currentBlendingEnabled: Boolean? = null
    var currentBlending: Int? = null
    var currentBlendEquation: Int? = null
    var currentBlendSrc: Int? = null
    var currentBlendDst: Int? = null
    var currentBlendEquationAlpha: Int? = null
    var currentBlendSrcAlpha: Int? = null
    var currentBlendDstAlpha: Int? = null
    var currentPremultipledAlpha: Boolean? = null

    var currentFlipSided: Boolean? = null
    var currentCullFace: Int? = null

    var currentLineWidth: Float? = null

    var currentPolygonOffsetFactor: Float? = null
    var currentPolygonOffsetUnits: Float? = null

    val maxTextures = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS)

    var lineWidthAvailable = false

    var currentTextureSlot: Int? = null
    var currentBoundTextures = mutableMapOf<Int?, BoundTexture>()

    var currentScissor = Vector4()
    var currentViewport = Vector4()

    private val emptyTextures = mapOf(
        GL11.GL_TEXTURE_2D to createTexture(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_2D, 1),
        GL13.GL_TEXTURE_CUBE_MAP to createTexture(GL13.GL_TEXTURE_CUBE_MAP, GL13.GL_TEXTURE_CUBE_MAP, 6)
    )

    init {
        enable(GL11.GL_DEPTH_TEST)
        depthBuffer.setFunc(LessEqualDepth)

        enable(GL11.GL_CULL_FACE)
        setBlending(NoBlending)
    }

    fun initAttributes() {
        for (i in 0 until newAttributes.size) {
            newAttributes[i] = 0
        }
    }

    fun enableAttribute( attribute: Int ) {

        enableAttributeAndDivisor( attribute, 0 );

    }

    fun enableAttributeAndDivisor( attribute: Int, meshPerAttribute: Int ) {

        newAttributes[ attribute ] = 1;

        if ( enabledAttributes[ attribute ] == 0 ) {

            GL20.glEnableVertexAttribArray( attribute );
            enabledAttributes[ attribute ] = 1;

        }

        if ( attributeDivisors[ attribute ] != meshPerAttribute ) {

            GL33.glVertexAttribDivisor(attribute, meshPerAttribute)
            attributeDivisors[ attribute ] = meshPerAttribute;

        }

    }

    fun disableUnusedAttributes() {

        for ( i in 0 until enabledAttributes.size ) {

            if ( enabledAttributes[ i ] != newAttributes[ i ] ) {

                GL20.glDisableVertexAttribArray( i );
                enabledAttributes[ i ] = 0;

            }

        }

    }


    fun createTexture(type: Int, target: Int, count: Int): Int {

        val data = IntArray(4) // 4 is required to match default unpack alignment of 4.
        val texture = GL11.glGenTextures()

        GL11.glBindTexture(type, texture);
        GL11.glTexParameteri(type, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(type, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        //TDOD
//        for (i in 0 until count) {
//            GL11.glTexImage2D(target + 1, 0, GL11.GL_RGBA, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data)
//        }

        return texture;

    }

    fun enable(id: Int) {
        if (enabledCapabilities[id] != true) {
            GL11.glEnable(id)
            enabledCapabilities[id] = true
        }
    }

    fun disable(id: Int) {
        if (enabledCapabilities[id] != false) {
            GL11.glDisable(id)
            enabledCapabilities[id] = false
        }
    }

    fun useProgram( program: Int ): Boolean {

        if ( currentProgram != program ) {

            GL20.glUseProgram( program );

            currentProgram = program;

            return true;

        }

        return false;

    }

    @Suppress("NAME_SHADOWING")
    fun setBlending(
        blending: Int,
        blendEquation: Int? = null,
        blendSrc: Int? = null,
        blendDst: Int? = null,
        blendEquationAlpha: Int? = null,
        blendSrcAlpha: Int? = null,
        blendDstAlpha: Int? = null,
        premultipliedAlpha: Boolean? = null
    ) {
        if (blending == NoBlending) {

            if (currentBlendingEnabled == true) {
                disable(GL11.GL_BLEND)
                currentBlendingEnabled = false
            }

            return

        }

        if (currentBlendingEnabled == true) {
            enable(GL11.GL_BLEND)
            currentBlendingEnabled = true
        }

        if (blending != CustomBlending) {

            if (blending != currentBlending || premultipliedAlpha != currentPremultipledAlpha) {

                if (currentBlendEquation != AddEquation || currentBlendEquationAlpha != AddEquation) {

                    GL14.glBlendEquation(GL14.GL_FUNC_ADD)

                    currentBlendEquation = AddEquation;
                    currentBlendEquationAlpha = AddEquation

                }

                if (premultipliedAlpha == true) {

                    when (blending) {
                        NormalBlending -> GL14.glBlendFuncSeparate(
                            GL11.GL_ONE,
                            GL11.GL_ONE_MINUS_SRC_ALPHA,
                            GL11.GL_ONE,
                            GL11.GL_ONE_MINUS_SRC_ALPHA
                        )
                        AdditiveBlending -> GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE)
                        SubtractiveBlending -> GL14.glBlendFuncSeparate(
                            GL11.GL_ZERO,
                            GL11.GL_ZERO,
                            GL11.GL_ONE_MINUS_SRC_COLOR,
                            GL11.GL_ONE_MINUS_SRC_ALPHA
                        )
                        MultiplyBlending -> GL14.glBlendFuncSeparate(
                            GL11.GL_ZERO,
                            GL11.GL_SRC_COLOR,
                            GL11.GL_ZERO,
                            GL11.GL_SRC_ALPHA
                        )
                        else -> System.err.println("THREE.WebGLState: Invalid blending: $blending")
                    }

                } else {

                    when (blending) {
                        NormalBlending -> GL14.glBlendFuncSeparate(
                            GL11.GL_SRC_ALPHA,
                            GL11.GL_ONE_MINUS_SRC_ALPHA,
                            GL11.GL_ONE,
                            GL11.GL_ONE_MINUS_SRC_ALPHA
                        )
                        AdditiveBlending -> GL14.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
                        SubtractiveBlending -> GL11.glBlendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR)
                        MultiplyBlending -> GL14.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_COLOR)
                        else -> System.err.println("THREE.WebGLState: Invalid blending: $blending")
                    }

                }

                currentBlendSrc = null
                currentBlendDst = null
                currentBlendSrcAlpha = null
                currentBlendDstAlpha = null

                currentBlending = blending
                currentPremultipledAlpha = premultipliedAlpha

            }

            return

        }

        // custom blending

        val blendEquationAlpha = blendEquationAlpha ?: blendEquation
        val blendSrcAlpha = blendSrcAlpha ?: blendSrc
        val blendDstAlpha = blendDstAlpha ?: blendDst

        if (blendEquation != currentBlendEquation || blendEquationAlpha != currentBlendEquationAlpha) {

            GL20.glBlendEquationSeparate(
                utils.convert(blendEquation),
                utils.convert(blendEquationAlpha)
            )

            currentBlendEquation = blendEquation
            currentBlendEquationAlpha = blendEquationAlpha

        }

        if (blendSrc != currentBlendSrc || blendDst != currentBlendDst || blendSrcAlpha != currentBlendSrcAlpha || blendDstAlpha != currentBlendDstAlpha) {

            GL14.glBlendFuncSeparate(
                utils.convert(blendSrc),
                utils.convert(blendDst),
                utils.convert(blendSrcAlpha),
                utils.convert(blendDstAlpha)
            );

            currentBlendSrc = blendSrc
            currentBlendDst = blendDst
            currentBlendSrcAlpha = blendSrcAlpha
            currentBlendDstAlpha = blendDstAlpha

        }

        currentBlending = blending
        currentPremultipledAlpha = null
    }

    fun setMaterial(material: Material, frontFaceCW: Boolean) {

        if (material.side == DoubleSide) {
            disable(GL11.GL_CULL_FACE)
        } else {
            enable(GL11.GL_CULL_FACE)
        }

        var flipSided = material.side == BackSide
        if (frontFaceCW) {
            flipSided = !flipSided
        }

        setFlipSided(flipSided)

        if (material.blending == NormalBlending && !material.transparent) {
            setBlending(NoBlending)
        } else {
            setBlending(
                material.blending,
                material.blendEquation,
                material.blendSrc,
                material.blendDst,
                material.blendEquationAlpha,
                material.blendSrcAlpha,
                material.blendDstAlpha,
                material.premultipliedAlpha
            )
        }

        depthBuffer.setFunc(material.depthFunc);
        depthBuffer.setTest(material.depthTest);
        depthBuffer.setMask(material.depthWrite);
        colorBuffer.setMask(material.colorWrite);

        setPolygonOffset(material.polygonOffset, material.polygonOffsetFactor, material.polygonOffsetUnits);

    }


    private fun setFlipSided(flipSided: Boolean) {

        if (currentFlipSided != flipSided) {

            if (flipSided) {
                GL11.glFrontFace(GL11.GL_CW);
            } else {
                GL11.glFrontFace(GL11.GL_CCW);
            }

            currentFlipSided = flipSided;

        }

    }

    fun setCullFace(cullFace: Int) {

        if (cullFace != CullFaceNone) {

            enable(GL11.GL_CULL_FACE)

            if (cullFace != currentCullFace) {

                when (cullFace) {
                    CullFaceBack -> GL11.glCullFace(GL11.GL_BACK)
                    CullFaceFront -> GL11.glCullFace(GL11.GL_FRONT)
                    else -> GL11.glCullFace(GL11.GL_FRONT_AND_BACK)
                }

            }

        } else {

            disable(GL11.GL_CULL_FACE)

        }

        currentCullFace = cullFace;

    }

    fun setLineWidth(width: Float) {

        if (width != currentLineWidth) {

            if (lineWidthAvailable) GL11.glLineWidth(width)

            currentLineWidth = width

        }

    }

    fun setPolygonOffset(polygonOffset: Boolean, factor: Float? = null, units: Float? = null) {

        if (polygonOffset) {

            enable(GL11.GL_POLYGON_OFFSET_FILL)

            if (currentPolygonOffsetFactor != factor || currentPolygonOffsetUnits != units) {

                GL11.glPolygonOffset(factor!!, units!!)

                currentPolygonOffsetFactor = factor
                currentPolygonOffsetUnits = units

            }

        } else {
            disable(GL11.GL_POLYGON_OFFSET_FILL)
        }
    }

    fun setScissorTest(scissorTest: Boolean) {
        if (scissorTest) {
            enable(GL11.GL_SCISSOR_TEST)
        } else {
            disable(GL11.GL_SCISSOR_TEST)
        }
    }

    fun activeTexture(glSlot: Int? = null) {

        @Suppress("NAME_SHADOWING")
        val glSlot = glSlot ?: GL13.GL_TEXTURE0 + maxTextures - 1;

        if (currentTextureSlot != glSlot) {

            GL13.glActiveTexture(glSlot)
            currentTextureSlot = glSlot

        }

    }

    fun bindTexture(glType: Int, glTexture: Int?) {

        if (currentTextureSlot == null) {

            activeTexture()

        }

        var boundTexture = currentBoundTextures[currentTextureSlot]

        if (boundTexture == null) {

            boundTexture = BoundTexture(type = null, texture = null)
            currentBoundTextures[currentTextureSlot] = boundTexture;

        }

        if (boundTexture.type != glType || boundTexture.texture != glTexture) {

            GL11.glBindTexture(glType, glTexture ?: emptyTextures[glType]!!)

            boundTexture.type = glType
            boundTexture.texture = glTexture

        }

    }

    fun texImage2D(
        target: Int,
        level: Int,
        internalformat: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        data: ByteBuffer?
    ) {
        GL11.glTexImage2D(target, level, internalformat, width, height, 0, format, type, data)
    }

    fun scissor(scissor: Vector4) {

        if (currentScissor != scissor) {

            GL11.glScissor(scissor.x.roundToInt(), scissor.y.roundToInt(), scissor.z.roundToInt(), scissor.w.roundToInt())
            currentScissor.copy(scissor);

        }

    }

    fun viewport(viewport: Vector4) {

        if (currentViewport != viewport) {

            GL11.glViewport(viewport.x.roundToInt(), viewport.y.roundToInt(), viewport.z.roundToInt(), viewport.w.roundToInt())
            currentViewport.copy(viewport);

        }

    }

    fun reset() {

        enabledAttributes.forEachIndexed { i, v ->
            if (v == 1) {
                GL20.glDisableVertexAttribArray(i)
                enabledAttributes[i] = 0
            }
        }

        enabledCapabilities.clear()

        currentTextureSlot = null
        currentBoundTextures.clear()

        currentProgram = null

        currentBlending = null

        currentFlipSided = null
        currentCullFace = null

        colorBuffer.reset()
        depthBuffer.reset()
        stencilBuffer.reset()

    }

    inner class GLColorBuffer {

        internal var locked = false

        private var color = Vector4()
        private var currentColorMask: Boolean? = null
        private var currentColorClear = Vector4(0.toFloat(), 0.toFloat(), 0.toFloat(), 0.toFloat())

        fun setMask(colorMask: Boolean) {
            if (currentColorMask != colorMask && !locked) {

                GL11.glColorMask(colorMask, colorMask, colorMask, colorMask)
                currentColorMask = colorMask

            }
        }

        @Suppress("NAME_SHADOWING")
        fun setClear(r: Float, g: Float, b: Float, a: Float, premultipliedAlpha: Boolean? = null) {

            var r = r
            var g = g
            var b = b

            if (premultipliedAlpha == true) {
                r *= a; g *= a; b *= a
            }

            color.set(r, g, b, a)

            if (currentColorClear != color) {
                GL11.glClearColor(r, g, b, a)
                currentColorClear.copy(color)
            }
        }

        fun reset() {
            locked = false

            currentColorMask = null
            currentColorClear.set((-1).toFloat(), 0.toFloat(), 0.toFloat(), 0.toFloat()); // set to invalid state
        }

    }

    inner class GLDepthBuffer {

        internal var locked = false

        private var currentDepthMask: Boolean? = null
        private var currentDepthFunc: Int? = null
        private var currentDepthClear: Double? = null

        fun setTest(depthTest: Boolean) {
            if (depthTest) {
                enable(GL11.GL_DEPTH_TEST);
            } else {
                disable(GL11.GL_DEPTH_TEST);
            }
        }

        fun setMask(depthMask: Boolean) {
            if (currentDepthMask != depthMask && !locked) {
                GL11.glDepthMask(depthMask);
                currentDepthMask = depthMask;
            }
        }

        fun setFunc(depthFunc: Int) {
            if (currentDepthFunc != depthFunc) {

                when (depthFunc) {
                    NeverDepth -> GL11.glDepthFunc(GL11.GL_NEVER)
                    AlwaysDepth -> GL11.glDepthFunc(GL11.GL_ALWAYS)
                    LessDepth -> GL11.glDepthFunc(GL11.GL_LESS)
                    LessEqualDepth -> GL11.glDepthFunc(GL11.GL_LEQUAL)
                    EqualDepth -> GL11.glDepthFunc(GL11.GL_EQUAL)
                    GreaterEqualDepth -> GL11.glDepthFunc(GL11.GL_GEQUAL)
                    GreaterDepth -> GL11.glDepthFunc(GL11.GL_GREATER)
                    NotEqualDepth -> GL11.glDepthFunc(GL11.GL_NOTEQUAL)
                    else -> GL11.glDepthFunc(GL11.GL_LEQUAL)
                }

            }
        }

        fun setClear(depth: Double) {
            if (currentDepthClear != depth) {

                GL11.glClearDepth(depth)
                currentDepthClear = depth

            }
        }

        fun reset() {
            locked = false

            currentDepthMask = null
            currentDepthFunc = null
            currentDepthClear = null
        }

    }

    inner class GLStencilBuffer {

        internal var locked = false

        var currentStencilMask: Int? = null
        var currentStencilFunc: Int? = null
        var currentStencilRef: Int? = null
        var currentStencilFuncMask: Int? = null
        var currentStencilFail: Int? = null
        var currentStencilZFail: Int? = null
        var currentStencilZPass: Int? = null
        var currentStencilClear: Int? = null

        fun setTest(stencilTest: Boolean) {
            if (stencilTest) {
                enable(GL11.GL_STENCIL_TEST)
            } else {
                disable(GL11.GL_STENCIL_TEST)
            }
        }

        fun setMask(stencilMask: Int) {
            if (currentStencilMask != stencilMask && !locked) {
                GL11.glStencilMask(stencilMask)
                currentStencilMask = stencilMask
            }
        }

        fun setFunc(stencilFunc: Int, stencilRef: Int, stencilMask: Int) {
            if (currentStencilFunc != stencilFunc ||
                currentStencilRef != stencilRef ||
                currentStencilFuncMask != stencilMask
            ) {

                GL11.glStencilFunc(stencilFunc, stencilRef, stencilMask);

                currentStencilFunc = stencilFunc
                currentStencilRef = stencilRef
                currentStencilFuncMask = stencilMask

            }
        }

        fun setOp(stencilFail: Int, stencilZFail: Int, stencilZPass: Int) {
            if (currentStencilFail != stencilFail ||
                currentStencilZFail != stencilZFail ||
                currentStencilZPass != stencilZPass
            ) {

                GL11.glStencilOp(stencilFail, stencilZFail, stencilZPass);

                currentStencilFail = stencilFail
                currentStencilZFail = stencilZFail
                currentStencilZPass = stencilZPass

            }
        }

        fun setClear(stencil: Int) {
            if (currentStencilClear != stencil) {

                GL11.glClearStencil(stencil)
                currentStencilClear = stencil

            }
        }

        fun reset() {
            locked = false

            currentStencilMask = null
            currentStencilFunc = null
            currentStencilRef = null
            currentStencilFuncMask = null
            currentStencilFail = null
            currentStencilZFail = null
            currentStencilZPass = null
            currentStencilClear = null
        }

    }

    data class BoundTexture(
        var type: Int?,
        var texture: Int?
    )

}
