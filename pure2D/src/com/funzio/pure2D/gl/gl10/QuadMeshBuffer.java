/**
 * 
 */
package com.funzio.pure2D.gl.gl10;

import javax.microedition.khronos.opengles.GL10;

/**
 * @author long
 */
public class QuadMeshBuffer extends VertexBuffer {
    public static final int NUM_VERTICES_PER_CELL = 4;
    public static final int NUM_INDICES_PER_CELL = 6;

    protected float[] mVertices;
    protected short[] mIndices;
    protected int mNumCells = 0;
    protected boolean mInvalidated = false;

    public QuadMeshBuffer(final int numCells) {
        super(GL10.GL_TRIANGLES, numCells * NUM_VERTICES_PER_CELL);

        setNumCells(numCells);
    }

    public void setNumCells(final int numCells) {
        if (numCells > mNumCells) {
            mVertices = new float[numCells * NUM_VERTICES_PER_CELL * mVertexPointerSize];
            mIndices = new short[numCells * NUM_INDICES_PER_CELL];

            // indices is always fixed
            int start = 0;
            short vertexStart = 0;
            for (int i = 0; i < numCells; i++) {
                // first triangle
                mIndices[start] = vertexStart;
                mIndices[start + 1] = (short) (vertexStart + 1);
                mIndices[start + 2] = (short) (vertexStart + 2);
                // second triangle
                mIndices[start + 3] = (short) (vertexStart + 2);
                mIndices[start + 4] = (short) (vertexStart + 1);
                mIndices[start + 5] = (short) (vertexStart + 3);
                start += NUM_INDICES_PER_CELL;
                vertexStart += NUM_VERTICES_PER_CELL;
            }
            setIndices(mIndices);

            mInvalidated = true;
        }

        mNumCells = numCells;
        mVerticesNum = numCells * NUM_VERTICES_PER_CELL;
    }

    public int getNumCells() {
        return mNumCells;
    }

    /**
     * Sets a Rect at a specified index but doesn't apply untill applyValues() gets called
     * 
     * @param index
     * @param x
     * @param y
     * @param width
     * @param height
     * @see #validate()
     */
    public void setRectAt(final int index, final float x, final float y, final float width, final float height) {

        final int start = index * NUM_VERTICES_PER_CELL * mVertexPointerSize;
        mVertices[start] = x;
        mVertices[start + 1] = y + height;
        mVertices[start + 2] = x;
        mVertices[start + 3] = y;
        mVertices[start + 4] = x + width;
        mVertices[start + 5] = y + height;
        mVertices[start + 6] = x + width;
        mVertices[start + 7] = y;

        mInvalidated = true;
    }

    /**
     * Sets a Rect at a specified index but doesn't apply untill applyValues() gets called
     * 
     * @param index
     * @param x
     * @param y
     * @param width
     * @param height
     * @see #validate()
     */
    public void setRectFlipVerticalAt(final int index, final float x, final float y, final float width, final float height) {

        final int start = index * NUM_VERTICES_PER_CELL * mVertexPointerSize;
        mVertices[start + 2] = x;
        mVertices[start + 3] = y + height;
        mVertices[start + 0] = x;
        mVertices[start + 1] = y;
        mVertices[start + 6] = x + width;
        mVertices[start + 7] = y + height;
        mVertices[start + 4] = x + width;
        mVertices[start + 5] = y;

        mInvalidated = true;
    }
    
    public void setValuesAt(final int index, float[] values) {

        final int start = index * NUM_VERTICES_PER_CELL * mVertexPointerSize;
        mVertices[start] = values[0];
        mVertices[start + 1] = values[1];
        mVertices[start + 2] = values[2];
        mVertices[start + 3] = values[3];
        mVertices[start + 4] = values[4];
        mVertices[start + 5] = values[5];
        mVertices[start + 6] = values[6];
        mVertices[start + 7] = values[7];

        mInvalidated = true;
    }

    /**
     * Applies the values set by {@link #setRectAt(int, float...)}
     */
    public void validate() {
        if (mInvalidated) {
            setValues(mVertices);

            // unflag
            mInvalidated = false;
        }
    }

}
