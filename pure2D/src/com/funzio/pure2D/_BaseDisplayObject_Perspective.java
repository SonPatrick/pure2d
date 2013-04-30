/**
 * 
 */
package com.funzio.pure2D;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLU;

import com.funzio.pure2D.animators.Manipulator;
import com.funzio.pure2D.containers.Container;
import com.funzio.pure2D.gl.GLColor;
import com.funzio.pure2D.gl.gl10.BlendFunc;
import com.funzio.pure2D.gl.gl10.GLState;

/**
 * @author long
 */
public abstract class _BaseDisplayObject_Perspective implements DisplayObject {
    public static final String TAG = _BaseDisplayObject_Perspective.class.getSimpleName();

    // for debugging
    protected int mDebugFlags = 0;

    // dimensions and size
    protected PointF mPosition = new PointF(0, 0);
    protected PointF mOrigin = new PointF(0, 0);
    protected PointF mSize = new PointF(1, 1);
    protected PointF mScale = new PointF(1, 1);
    // rotation
    protected float mRotation = 0;
    protected float mRotationX = 0;
    protected float mRotationY = 0;
    protected float mRotationVectorX = 0;
    protected float mRotationVectorY = 1;
    protected float mRotationVectorZ = 0;
    protected float mZ = 0;// z-order
    // extra transformation
    protected float[] mTransform3DValues;

    // life
    protected boolean mVisible = true;
    protected boolean mAlive = true;

    // framerate
    private int mFps = 0; // frames per second
    protected float mFrameDuration = 0; // ms per frame

    // reference to the parent container
    protected Container mParent;
    protected Scene mScene;

    protected Maskable mMask;

    // extra
    protected GLColor mColor = null;// new GLColor(1f, 0f, 0f, 1f);
    protected float mAlpha = 1;
    protected BlendFunc mBlendFunc;
    private boolean mAlphaTestEnabled = false;

    private boolean mHasOrigin = false;
    private GLColor mSumColor;

    protected ArrayList<Manipulator> mManipulators;
    protected int mNumManipulators = 0;

    // rect and bounds
    protected int mInvalidateFlags = 0;
    protected Matrix mMatrix = new Matrix();
    protected Camera mTransformCamera; // for 3d transformation
    protected Matrix mTransformMatrix;
    protected boolean mAutoUpdateBounds = false;
    // global bounds
    protected RectF mBounds = new RectF(-mOrigin.x, -mOrigin.y, -mOrigin.x + mSize.x - 1, -mOrigin.y + mSize.y - 1);

    abstract protected boolean drawChildren(final GLState glState);

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.DisplayObject#draw(javax.microedition.khronos.opengles.GL10, int)
     */
    @Override
    public boolean draw(final GLState glState) {
        drawStart(glState);

        // blend mode
        // final boolean blendChanged = glState.setBlendFunc(mBlendFunc);
        glState.setBlendFunc(mBlendFunc);
        // color and alpha
        glState.setColor(getSumColor());

        // draw the content
        drawChildren(glState);

        // if (blendChanged) {
        // // recover the blending
        // glState.setBlendFunc(null);
        // }

        // wrap up
        drawEnd(glState);

        return true;
    }

    protected void drawStart(final GLState glState) {
        glState.mGL.glMatrixMode(GL10.GL_PROJECTION);
        glState.mGL.glPushMatrix();
        glState.mGL.glLoadIdentity();
        final PointF global = mPosition;// localToGlobal(new PointF());
        // Log.e("long", " " + global.x + " " + global.y);

        float aspect = getScene().getSize().x / getScene().getSize().y;
        GLU.gluPerspective(glState.mGL, 54, aspect, 0.001f, Math.max(getScene().getSize().x, getScene().getSize().y)); // works!
        // GLU.gluLookAt(glState.mGL, getScene().getSize().x / 2, getScene().getSize().y / 2, 1000, getScene().getSize().x / 2, getScene().getSize().y / 2, 0, 0, 1, 0);
        GLU.gluLookAt(glState.mGL, 0, 0, Math.max(getScene().getSize().x, getScene().getSize().y), 0, 0, 0, 0, 1, 0); // work but not perfect
        // GLU.gluLookAt(glState.mGL, global.x, global.y, Math.max(getScene().getSize().x, getScene().getSize().y), 0, 0, 0, 0, 1, 0);
        // GLU.gluLookAt(glState.mGL, global.x, global.y, 1000, global.x, global.y, 0, 0, 1, 0);
        // glState.mGL.glTranslatef(-getScene().getSize().x / 2, -getScene().getSize().y / 2, mZ);
        glState.mGL.glMatrixMode(GL10.GL_MODELVIEW);

        // keep the matrix
        glState.mGL.glPushMatrix();
        // GLU.gluLookAt(glState.mGL, global.x, global.y, 1000, global.x, global.y, 0, 0, 1, 0);
        // GLU.gluLookAt(glState.mGL, 0, 0, Math.max(getScene().getSize().x, getScene().getSize().y), 0, 0, 0, 0, 1, 0);

        // translating
        if (mPosition.x != 0 || mPosition.y != 0 || mZ != 0) {
            // glState.mGL.glTranslatef(mPosition.x, mPosition.y, mZ);
            glState.mGL.glTranslatef(mPosition.x - getScene().getSize().x / 2, mPosition.y - getScene().getSize().y / 2, mZ); // works!
        }

        // scaling
        if (mScale.x != 1 || mScale.y != 1) {
            glState.mGL.glScalef(mScale.x, mScale.y, 0);
        }
        // rotating
        if (mRotation != 0) {
            glState.mGL.glRotatef(mRotation, mRotationVectorX, mRotationVectorY, mRotationVectorZ);
        }

        // extra transformation
        if (mTransform3DValues != null) {
            // glState.mGL.glMatrixMode(GL10.GL_PROJECTION);
            // glState.mGL.glPushMatrix();
            glState.mGL.glMultMatrixf(mTransform3DValues, 0);
            // glState.mGL.glMatrixMode(GL10.GL_MODELVIEW);
        }

        // shift off the origin
        if (mHasOrigin) {
            glState.mGL.glTranslatef(-mOrigin.x, -mOrigin.y, 0);
        }

        // check and turn on alpha test
        glState.setAlphaTestEnabled(mAlphaTestEnabled);

        // check mask
        if (mMask != null) {
            mMask.enableMask();
        }
    }

    protected void drawEnd(final GLState glState) {

        // extra transformation
        // if (mTransform3DValues != null) {
        // glState.mGL.glMatrixMode(GL10.GL_PROJECTION);
        // glState.mGL.glPopMatrix();
        // glState.mGL.glMatrixMode(GL10.GL_MODELVIEW);
        // }

        glState.mGL.glMatrixMode(GL10.GL_PROJECTION);
        glState.mGL.glPopMatrix();
        glState.mGL.glMatrixMode(GL10.GL_MODELVIEW);

        // check mask
        if (mMask != null) {
            mMask.disableMask();
        }

        // restore the matrix
        glState.mGL.glPopMatrix();

        // for debugging
        final int debugFlags = Pure2D.DEBUG_FLAGS | mDebugFlags;
        if (debugFlags != 0) {

            // local rect
            if ((debugFlags & Pure2D.DEBUG_FLAG_LOCAL_SHAPE) != 0 && mSize.x > 0 && mSize.y > 0) {
                Pure2D.drawDebugRect(glState, 0, 0, mSize.x - 1, mSize.y - 1, Pure2D.DEBUG_FLAG_LOCAL_SHAPE);
            }

            // global bounds
            if ((debugFlags & Pure2D.DEBUG_FLAG_GLOBAL_BOUNDS) != 0 && mBounds.width() > 0 && mBounds.height() > 0) {
                glState.mGL.glPushMatrix();
                glState.mGL.glLoadIdentity();
                Pure2D.drawDebugRect(glState, mBounds.left, mBounds.bottom, mBounds.right, mBounds.top, Pure2D.DEBUG_FLAG_GLOBAL_BOUNDS);
                glState.mGL.glPopMatrix();
            }
        }

        // clear all visual flags
        validate(InvalidateFlags.VISUAL);
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.DisplayObject#update(int)
     */
    @Override
    public boolean update(final int deltaTime) {
        rotate(1);

        // if ((mInvalidateFlags & InvalidateFlags.ROTATION) != 0 && (mRotationX != 0 || mRotationY != 0)) {
        // if (mTransformCamera == null) {
        // mTransformCamera = new Camera();
        // mTransformCamera.setLocation(0, 0, -0.1f);
        // mTransformMatrix = new Matrix();
        // mTransform3DValues = new float[16];
        // } else {
        // mTransformCamera.restore();
        // }
        // mTransformCamera.save();
        // if (mRotationX != 0) {
        // mTransformCamera.rotateX(mRotationX);
        // }
        // if (mRotationY != 0) {
        // mTransformCamera.rotateY(mRotationY);
        // }
        // mTransformCamera.getMatrix(mTransformMatrix);
        // // mTransformMatrix.postTranslate(0, 50); // testing skew
        // Pure2DUtils.getMatrix3DValues(mTransformMatrix, mTransform3DValues);
        // }

        if (mAutoUpdateBounds && (mInvalidateFlags & InvalidateFlags.BOUNDS) != 0) {
            // re-cal the matrix
            updateBounds();
        }

        // update the manipulators if there's any
        if (mNumManipulators > 0) {
            for (int i = 0; i < mNumManipulators; i++) {
                mManipulators.get(i).update(deltaTime);
            }
            return true;
        }

        return false;
    }

    /**
     * @hide
     */
    final public void invalidate() {
        if (mParent != null) {
            mParent.invalidate();
        }
    }

    /**
     * @hide
     */
    final public void invalidate(final int flags) {
        mInvalidateFlags |= flags;

        if (mParent != null) {
            mParent.invalidate(mInvalidateFlags);
        }
    }

    /**
     * @hide
     */
    final protected void validate(final int flags) {
        mInvalidateFlags &= ~flags;
    }

    /**
     * Toggles the heart-beat. If set to false, the update() does NOT get called. This can be used for optimization.
     * 
     * @see com.funzio.pure2D.IDisplayObject#setAlive(boolean)
     * @see #update(int)
     */
    @Override
    public void setAlive(final boolean value) {
        mAlive = value;
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.IDisplayObject#getAlive()
     */
    @Override
    final public boolean isAlive() {
        return mAlive;
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.IDisplayObject#setVisible(boolean)
     */
    @Override
    public void setVisible(final boolean value) {
        mVisible = value;
        invalidate(InvalidateFlags.VISIBILITY);
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.IDisplayObject#getVisible()
     */
    @Override
    final public boolean isVisible() {
        return mVisible;
    }

    /*
     * (non-Javadoc)
     * @see com.funzio.pure2D.DisplayObject#shouldDraw()
     */
    @Override
    public boolean shouldDraw() {
        return mVisible && mAlpha > 0;
    }

    /**
     * @return the position
     */
    final public PointF getPosition() {
        return mPosition;
    }

    /**
     * @param position the position to set
     */
    final public void setPosition(final PointF position) {
        setPosition(position.x, position.y);
    }

    /**
     * @param position the position to set
     */
    public void setPosition(final float x, final float y) {
        mPosition.x = x;
        mPosition.y = y;
        invalidate(InvalidateFlags.POSITION);
    }

    public void setX(final float x) {
        mPosition.x = x;
        invalidate(InvalidateFlags.POSITION);
    }

    final public float getX() {
        return mPosition.x;
    }

    public void setY(final float y) {
        mPosition.y = y;
        invalidate(InvalidateFlags.POSITION);
    }

    final public float getY() {
        return mPosition.y;
    }

    /**
     * Set the Z-depth
     * 
     * @see #setAlphaTestEnabled(boolean)
     */
    public void setZ(final float z) {
        mZ = z;
        invalidate(InvalidateFlags.POSITION);
    }

    final public float getZ() {
        return mZ;
    }

    public void moveTo(final float x, final float y) {
        mPosition.x = x;
        mPosition.y = y;
        invalidate(InvalidateFlags.POSITION);
    }

    public void moveBy(final float x, final float y) {
        mPosition.x += x;
        mPosition.y += y;
        invalidate(InvalidateFlags.POSITION);
    }

    /**
     * @return the origin
     */
    final public PointF getOrigin() {
        return mOrigin;
    }

    /**
     * @param origin the origin to set. Origin is the local point and (0,0) by default. Origin is used to define offset of this object and also the center of rotation and scaling.
     */
    public void setOrigin(final PointF origin) {
        mOrigin.x = origin.x;
        mOrigin.y = origin.y;
        mHasOrigin = mOrigin.x != 0 || mOrigin.y != 0;
        invalidate(InvalidateFlags.ORIGIN);
    }

    public void setOrigin(final float x, final float y) {
        mOrigin.x = x;
        mOrigin.y = y;
        mHasOrigin = mOrigin.x != 0 || mOrigin.y != 0;
        invalidate(InvalidateFlags.ORIGIN);
    }

    public void setOriginAtCenter() {
        mOrigin.x = mSize.x / 2;
        mOrigin.y = mSize.y / 2;
        mHasOrigin = mOrigin.x != 0 || mOrigin.y != 0;
        invalidate(InvalidateFlags.ORIGIN);
    }

    /**
     * @return the size
     */
    final public PointF getSize() {
        return mSize;
    }

    final public float getWidth() {
        return mSize.x;
    }

    final public float getHeight() {
        return mSize.y;
    }

    /**
     * @param size the size to set
     */
    final public void setSize(final PointF size) {
        setSize(size.x, size.y);
    }

    /**
     * @param size the size to set
     */
    public void setSize(final float w, final float h) {
        mSize.x = w;
        mSize.y = h;
        invalidate(InvalidateFlags.SIZE);
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(final float sx, final float sy) {
        mScale.x = sx;
        mScale.y = sy;
        invalidate(InvalidateFlags.SCALE);
    }

    /**
     * @param scale the scale to set
     */
    public void setScale(final float scale) {
        mScale.x = mScale.y = scale;
        invalidate(InvalidateFlags.SCALE);
    }

    final public PointF getScale() {
        return mScale;
    }

    public void setRotation(final float degree) {
        mRotation = degree;
        invalidate(InvalidateFlags.ROTATION);
    }

    @Deprecated
    public void rotateBy(final float degreeDelta) {
        mRotation += degreeDelta;
        invalidate(InvalidateFlags.ROTATION);
    }

    public void rotate(final float degreeDelta) {
        mRotation += degreeDelta;
        invalidate(InvalidateFlags.ROTATION);
    }

    final public float getRotation() {
        return mRotation;
    }

    public void setRotationX(final float degree) {
        mRotationX = degree;
        invalidate(InvalidateFlags.ROTATION);
    }

    public void rotateX(final float degreeDelta) {
        mRotationX += degreeDelta;
        invalidate(InvalidateFlags.ROTATION);
    }

    final public float getRotationX() {
        return mRotationX;
    }

    public void setRotationY(final float degree) {
        mRotationY = degree;
        invalidate(InvalidateFlags.ROTATION);
    }

    public void rotateY(final float degreeDelta) {
        mRotationY += degreeDelta;
        invalidate(InvalidateFlags.ROTATION);
    }

    final public float getRotationY() {
        return mRotationY;
    }

    // public void setRotationVector(final float x, final float y, final float z) {
    // mRotationVectorX = x;
    // mRotationVectorY = y;
    // mRotationVectorZ = z;
    // }

    // public float[] getExtraTransformation() {
    // return mExtraTransformation;
    // }

    // /**
    // * Apply extra transformation
    // *
    // * @param transformation
    // * @see com.funzio.pure2D.geom.Matrix, Matrix
    // */
    // public void setExtraTransformation(final float[] transformation) {
    // mExtraTransformation = transformation;
    // invalidate();
    // }

    /**
     * @return the color
     */
    final public GLColor getColor() {
        return mColor;
    }

    /**
     * @param color the color to set
     */
    public void setColor(final GLColor color) {
        mColor = color;
        invalidate(InvalidateFlags.COLOR);
    }

    /**
     * @return the final color which takes parent's color and alpha into account
     */
    final protected GLColor getSumColor() {
        if (mSumColor == null) {
            // init the mSumColor
            mSumColor = (mColor == null) ? new GLColor(1f, 1f, 1f, mAlpha) : new GLColor(mColor.r, mColor.g, mColor.b, mColor.a * mAlpha);
        } else {
            // recycle the mSumColor object to prevent GC
            if (mColor == null) {
                mSumColor.setValues(1f, 1f, 1f, mAlpha);
            } else {
                mSumColor.setValues(mColor.r, mColor.g, mColor.b, mColor.a * mAlpha);
            }
        }
        // multiply by parent's attributes
        if (mParent != null && mParent instanceof _BaseDisplayObject_Perspective) {
            final _BaseDisplayObject_Perspective parent = (_BaseDisplayObject_Perspective) mParent;
            final GLColor parentColor = parent.getSumColor();
            if (parentColor != null) {
                mSumColor.multiply(parentColor);
            }
        }

        return mSumColor;
    }

    /**
     * This looks better in Premultiplied-alpha mode
     * 
     * @return
     */
    // final protected GLColor getSumColor() {
    // if (mSumColor == null) {
    // // init the mSumColor
    // mSumColor = (mColor == null) ? new GLColor(1f, 1f, 1f, 1f) : new GLColor(mColor);
    // mSumColor.multiply(mAlpha);
    // } else {
    // // recycle the mSumColor object to prevent GC
    // if (mColor == null) {
    // mSumColor.setValues(mAlpha, mAlpha, mAlpha, mAlpha);
    // } else {
    // mSumColor.setValues(mColor.r * mAlpha, mColor.g * mAlpha, mColor.b * mAlpha, mColor.a * mAlpha);
    // }
    // }
    // // multiply by parent's attributes
    // if (mParent != null && mParent instanceof BaseDisplayObject) {
    // final BaseDisplayObject parent = (BaseDisplayObject) mParent;
    // final GLColor color = parent.getSumColor();
    // if (color != null) {
    // mSumColor.multiply(color);
    // }
    // }
    //
    // return mSumColor;
    // }

    /**
     * @return the alpha
     */
    final public float getAlpha() {
        return mAlpha;
    }

    /**
     * @param alpha the alpha to set
     */
    public void setAlpha(final float alpha) {
        mAlpha = alpha;
        invalidate(InvalidateFlags.ALPHA);
    }

    /**
     * @return the fps
     */
    final public int getFps() {
        return mFps;
    }

    /**
     * @param fps the fps to set
     */
    public void setFps(final int fps) {
        mFps = fps;

        if (mFps > 0) {
            mFrameDuration = 1000f / mFps;
        } else {
            mFrameDuration = Scene.DEFAULT_MSPF;
        }
    }

    public boolean addManipulator(final Manipulator manipulator) {
        if (mManipulators == null) {
            // init
            mManipulators = new ArrayList<Manipulator>();
        }

        if (mManipulators.add(manipulator)) {
            manipulator.setTarget(this);
            mNumManipulators++;
            return true;
        }

        return false;
    }

    public boolean removeManipulator(final Manipulator manipulator) {
        // null check
        if (mManipulators != null && mManipulators.remove(manipulator)) {
            manipulator.setTarget(null);
            mNumManipulators--;
            return true;
        }

        return false;
    }

    public int removeAllManipulators() {
        // null check
        if (mManipulators == null) {
            return 0;
        }

        final int n = mNumManipulators;
        mManipulators.clear();
        mNumManipulators = 0;

        return n;
    }

    /**
     * @return the blendFunc
     */
    public BlendFunc getBlendFunc() {
        return mBlendFunc;
    }

    /**
     * @param blendFunc the blendFunc to set
     */
    public void setBlendFunc(final BlendFunc blendFunc) {
        mBlendFunc = blendFunc;
        invalidate(InvalidateFlags.BLEND);
    }

    final public boolean isAlphaTestEnabled() {
        return mAlphaTestEnabled;
    }

    /**
     * Enable Alpha Test. This can be useful when doing depth sorting but also a Performance-Killer. Make sure you know what you're doing!
     * 
     * @param alphaTestEnabled
     * @see #setZ(float)
     */
    public void setAlphaTestEnabled(final boolean alphaTestEnabled) {
        mAlphaTestEnabled = alphaTestEnabled;

        invalidate(InvalidateFlags.ALPHA);
    }

    final public Maskable getMask() {
        return mMask;
    }

    public void setMask(final Maskable mask) {
        mMask = mask;
    }

    private Scene findScene() {
        if (mParent instanceof Scene) {
            return (Scene) mParent;
        } else if (mParent instanceof DisplayObject) {
            return ((DisplayObject) mParent).getScene();
        }

        return null;
    }

    final public Scene getScene() {
        if (mScene == null) {
            mScene = findScene();
        }

        return mScene;
    }

    final public Container getParent() {
        return mParent;
    }

    final public boolean queueEvent(final Runnable r) {
        if (getScene() != null) {
            return mScene.queueEvent(r);
        } else if (Pure2D.ADAPTER != null) {
            // use the adapter
            if (Pure2D.ADAPTER.getSurface() != null) {
                Pure2D.ADAPTER.getSurface().queueEvent(r);
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    public boolean removeFromParent() {
        if (mParent != null) {
            return mParent.removeChild(this);
        }
        return false;
    }

    /**
     * Converts a local point to a global point
     * 
     * @param local
     * @return
     */
    final public PointF localToGlobal(final PointF local) {
        // final PointF temp = new PointF(pt.x + mPosition.x - mOrigin.x, pt.y + mPosition.y - mOrigin.y);
        final PointF result = new PointF((local == null ? 0 : local.x) + mPosition.x, (local == null ? 0 : local.y) + mPosition.y);

        if (mParent != null && !(mParent instanceof Scene)) {
            mParent.localToGlobal(result, result);

            if (mParent instanceof DisplayObject) {
                // apply parent's origin
                final PointF parentOrigin = ((DisplayObject) mParent).getOrigin();
                result.x -= parentOrigin.x;
                result.y -= parentOrigin.y;
            }
        }

        return result;
    }

    /**
     * Converts a local point to a global point, without allocating new PointF
     * 
     * @param local
     * @param result
     */
    final public void localToGlobal(final PointF local, final PointF result) {
        result.x = (local == null ? 0 : local.x) + mPosition.x;
        result.y = (local == null ? 0 : local.y) + mPosition.y;
        if (mParent != null && !(mParent instanceof Scene)) {
            mParent.localToGlobal(result, result);

            if (mParent instanceof DisplayObject) {
                // apply parent's origin
                final PointF parentOrigin = ((DisplayObject) mParent).getOrigin();
                result.x -= parentOrigin.x;
                result.y -= parentOrigin.y;
            }
        }
    }

    /**
     * Converts a global point to a local point
     * 
     * @param global
     * @return
     */
    final public PointF globalToLocal(final PointF global) {
        final PointF local;
        if (mParent != null && !(mParent instanceof Scene)) {
            local = mParent.globalToLocal(global);

            if (mParent instanceof DisplayObject) {
                // apply parent's origin
                final PointF parentOrigin = ((DisplayObject) mParent).getOrigin();
                local.x += parentOrigin.x;
                local.y += parentOrigin.y;
            }
        } else {
            local = new PointF(global.x, global.y);
        }

        local.x -= mPosition.x;
        local.y -= mPosition.y;
        return local;
    }

    /**
     * Converts a global point to a local point, without allocating new PointF
     * 
     * @param global
     * @param result
     */
    final public void globalToLocal(final PointF global, final PointF result) {
        if (mParent != null && !(mParent instanceof Scene)) {
            mParent.globalToLocal(global, result);

            if (mParent instanceof DisplayObject) {
                // apply parent's origin
                final PointF parentOrigin = ((DisplayObject) mParent).getOrigin();
                result.x += parentOrigin.x;
                result.y += parentOrigin.y;
            }
        } else {
            result.x = global.x;
            result.y = global.y;
        }

        result.x -= mPosition.x;
        result.y -= mPosition.y;
    }

    /**
     * Find the global bounds of this object that takes position, scale, rotation... into account. Used mainly for Camera clipping.
     */
    public RectF updateBounds() {
        // init
        final Matrix parentMatrix = (mParent == null) ? null : mParent.getMatrix();
        boolean changed = false;

        if (mTransformMatrix != null) {
            mMatrix.setTranslate(-mOrigin.x, -mOrigin.y);
            mMatrix.postConcat(mTransformMatrix);
            mMatrix.postTranslate(mOrigin.x, mOrigin.y);
            // flag
            changed = true;
        }

        // rotation first
        // if (mRotation != 0) {
        // if (changed) {
        // mMatrix.postRotate(mRotation, mOrigin.x, mOrigin.y);
        // } else {
        // mMatrix.setRotate(mRotation, mOrigin.x, mOrigin.y);
        // }
        // // flag
        // changed = true;
        // }

        // scale next
        if (mScale.x != 1 || mScale.y != 1) {
            if (changed) {
                mMatrix.postScale(mScale.x, mScale.y, mOrigin.x, mOrigin.y);
            } else {
                mMatrix.setScale(mScale.x, mScale.y, mOrigin.x, mOrigin.y);
                // flag
                changed = true;
            }
        }

        // clear flags: bounds
        validate(InvalidateFlags.BOUNDS);

        // translate later
        if ((mPosition.x - mOrigin.x) != 0 || (mPosition.y - mOrigin.y) != 0) {
            if (changed) {
                mMatrix.postTranslate(mPosition.x - mOrigin.x, mPosition.y - mOrigin.y);
            } else {
                mMatrix.setTranslate(mPosition.x - mOrigin.x, mPosition.y - mOrigin.y);
                // flag
                changed = true;

                if (parentMatrix == null) {
                    // easy case: only translation needs to be applied. No need to use matrix!
                    mBounds.left = mPosition.x - mOrigin.x;
                    mBounds.top = mPosition.y - mOrigin.y;
                    mBounds.right = mBounds.left + mSize.x - 1;
                    mBounds.bottom = mBounds.top + mSize.y - 1;
                    // done, no need to go further!
                    return mBounds;
                }
            }
        } else {
            if (!changed) {
                // reset to identity
                mMatrix.reset();
            }
        }

        // prepare to map to the matrix
        mBounds.left = 0;
        mBounds.top = 0;
        mBounds.right = mSize.x - 1;
        mBounds.bottom = mSize.y - 1;

        // find the bounds
        if (changed || parentMatrix != null) {
            // apply the parent's matrix
            if (parentMatrix != null) {
                mMatrix.postConcat(parentMatrix);
            }

            // apply to the bounds
            mMatrix.mapRect(mBounds);
        }

        return mBounds;
    }

    final public Matrix getMatrix() {
        return mMatrix;
    }

    /**
     * Get the Global Bounds of this object that takes translation, rotation and scale factors into account.
     * 
     * @return
     */
    final public RectF getBounds() {
        return mBounds;
    }

    /**
     * @return the autoUpdateBounds
     */
    final public boolean isAutoUpdateBounds() {
        return mAutoUpdateBounds;
    }

    /**
     * This needs to be set to true if using Camera clipping.
     * 
     * @param autoUpdateBounds the autoUpdateBounds to set
     */
    public void setAutoUpdateBounds(final boolean autoUpdateBounds) {
        mAutoUpdateBounds = autoUpdateBounds;
    }

    /**
     * @return
     * @see Pure2D.DEBUG_FLAG_LOCAL_SHAPE, Pure2D.DEBUG_FLAG_GLOBAL_BOUNDS
     */
    public int getDebugFlags() {
        return mDebugFlags;
    }

    /**
     * @param flags
     * @see Pure2D.DEBUG_FLAG_SHAPE, Pure2D.DEBUG_FLAG_BOUNDS
     */
    public void setDebugFlags(final int flags) {
        mDebugFlags = flags;
        invalidate(InvalidateFlags.VISUAL);
    }

    // /**
    // * This is called before this object is added to a Container
    // */
    // public void onPreAdded(final Container container) {
    // // TODO nothing yet
    // }

    /**
     * This is called after this object is added to a Container
     */
    public void onAdded(final Container container) {
        mParent = container;
        mScene = findScene();

        // flag the bounds are changed now
        invalidate(InvalidateFlags.BOUNDS);
    }

    // /**
    // * This is called before this object is remove from a Container
    // */
    // public void onPreRemoved() {
    // // TODO nothing yet
    // }

    /**
     * This is called after this object is removed from a Container
     */
    public void onRemoved() {
        mParent = null;
        mScene = null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '@' + Integer.toHexString(hashCode());
    }

    /**
     * for debugging
     */
    public String getTrace(final String prefix) {
        return prefix + toString();
    }
}