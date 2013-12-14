/*
 Copyright (c) 2013, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.physics;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A simplified physics control for a non-solid object.
 *
 * Implements key methods in order to simplify the development of subclasses.
 *
 * Assumes that the object will live in a single physics space and listen to
 * overlap events.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class SimpleGhostControl
        extends GhostControl
        implements PhysicsTickListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this control
     */
    final private static Logger logger =
            Logger.getLogger(SimpleGhostControl.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a control.
     *
     * @param enabled true for an enabled object, false for a disabled one
     * @param initialShape collision shape for the object (not null)
     * @param initialSpace which physics space will contain the object (not
     * null)
     */
    public SimpleGhostControl(boolean enabled, CollisionShape initialShape,
            PhysicsSpace initialSpace) {
        super(initialShape);
        assert initialSpace != null;
        space = initialSpace;
        setEnabled(enabled);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Alter the object's collision shape. Assumes that the object has already
     * been added to physics space.
     */
    public void changeShape(CollisionShape shape) {
        assert shape != null;
        assert space != null;
        /*
         * The remove() method will null out the "space" field, so save a copy.
         */
        PhysicsSpace physicsSpace = space;
        /*
         * In order to resize a physical object, we must
         * remove the control from physics space and then re-add it.
         */
        physicsSpace.remove(this);
        super.setCollisionShape(shape);
        physicsSpace.add(this);
    }
    // *************************************************************************
    // GhostControl methods

    /**
     * Clone this control for a different spatial.
     *
     * @param spatial which spatial to clone for (not null)
     * @return a new control
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * De-serialize this control when loading from a .jm3o file.
     *
     * @param importer (not null)
     * @throws IOException TODO when?
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Enable or disable the control.
     *
     * @param newState true to enable or false to disable
     */
    @Override
    final public void setEnabled(boolean newState) {
        enabled = newState;
        if (enabled && !added) {
            onAdd();
            added = true;
        } else if (!enabled && added) {
            onRemove();
            added = false;
        }
    }

    /**
     * Serialize this control when saving to a .jm3o file.
     *
     * @param exporter (not null)
     * @throws IOException TODO when?
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback invoked after each physics tick.
     *
     * Does nothing. Meant to be overridden.
     *
     * @param space
     * @param tpf
     */
    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
        /* do nothing */
    }

    /**
     * Callback invoked before each physics tick.
     *
     * Does nothing. Meant to be overridden.
     *
     * @param space
     * @param tpf
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        /* do nothing */
    }
    // *************************************************************************
    // new protected methods

    /**
     * (Re-)Initialize this control each time it is added.
     *
     * Meant to be overridden.
     */
    protected void onAdd() {
        if (spatial != null) {
            setPhysicsLocation(spatial.getWorldTranslation());
            setPhysicsRotation(spatial.getWorldRotation());
        }
        space.addCollisionObject(this);
        space.addTickListener(this);
    }

    /**
     * De-initialize this control each time it is removed.
     *
     * Meant to be overridden.
     */
    protected void onRemove() {
        space.removeCollisionObject(this);
        space.removeTickListener(this);
    }
}