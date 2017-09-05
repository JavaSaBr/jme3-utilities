/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.wes;

import com.jme3.animation.BoneTrack;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;

/**
 * Tweening techniques for time sequences of JME transforms.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TweenTransforms implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TweenTransforms.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // fields

    /**
     * technique for rotations
     */
    private TweenRotations tweenRotations = TweenRotations.Nlerp;
    /**
     * technique for scales
     */
    private TweenVectors tweenScales = TweenVectors.Lerp;
    /**
     * technique for translations
     */
    private TweenVectors tweenTranslations = TweenVectors.Lerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the bone transform for the specified track and time using these
     * techniques.
     *
     * @param track input (not null, unaffected)
     * @param time animation time input (in seconds)
     * @param duration (in seconds)
     * @param storeResult (modified if not null)
     * @return a transform (either storeResult or a new instance)
     */
    public Transform boneTransform(BoneTrack track, float time,
            float duration, Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }
        float[] times = track.getKeyFrameTimes();
        int lastFrame = times.length - 1;
        assert lastFrame >= 0 : lastFrame;

        Vector3f[] translations = track.getTranslations();
        Quaternion[] rotations = track.getRotations();
        Vector3f[] scales = track.getScales();

        if (time <= 0f || lastFrame == 0) {
            /*
             * Copy the transform of the first frame.
             */
            storeResult.setTranslation(translations[0]);
            storeResult.setRotation(rotations[0]);
            if (scales == null) {
                storeResult.setScale(scaleIdentity);
            } else {
                storeResult.setScale(scales[0]);
            }

        } else {
            /*
             * Interpolate between frames.
             */
            interpolate(time, times, duration, translations, rotations, scales,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Read the technique for rotations.
     *
     * @return enum (not null)
     */
    public TweenRotations getTweenRotations() {
        return tweenRotations;
    }

    /**
     * Read the technique for scales.
     *
     * @return enum (not null)
     */
    public TweenVectors getTweenScales() {
        return tweenScales;
    }

    /**
     * Read the technique for translations.
     *
     * @return enum (not null)
     */
    public TweenVectors getTweenTranslations() {
        return tweenTranslations;
    }

    /**
     * Interpolate between keyframes in a bone track using these techniques.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param boneTrack (not null, unaffected)
     * @param duration animation duration (in seconds, &gt;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform interpolate(float time, BoneTrack boneTrack,
            float duration, Transform storeResult) {
        Validate.inRange(time, "time", 0f, duration);

        float[] times = boneTrack.getKeyFrameTimes();
        Vector3f[] translations = boneTrack.getTranslations();
        Quaternion[] rotations = boneTrack.getRotations();
        Vector3f[] scales = boneTrack.getScales();
        storeResult = interpolate(time, times, duration, translations,
                rotations, scales, storeResult);

        return storeResult;
    }

    /**
     * Interpolate between keyframes in a bone track using these techniques.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param times keyframe times (in seconds, not null, unaffected)
     * @param duration animation duration (in seconds, &gt;0)
     * @param translations (not null, unaffected, same length as times)
     * @param rotations (not null, unaffected, same length as times)
     * @param scales (may be null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform interpolate(float time, float[] times, float duration,
            Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales,
            Transform storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.nonNull(times, "times");
        Validate.nonNull(translations, "translations");
        Validate.nonNull(rotations, "rotations");
        if (storeResult == null) {
            storeResult = new Transform();
        }

        tweenTranslations.interpolate(time, times, duration, translations,
                storeResult.getTranslation());
        tweenRotations.interpolate(time, times, duration, rotations,
                storeResult.getRotation());

        if (scales == null) {
            storeResult.setScale(scaleIdentity);
        } else {
            tweenScales.interpolate(time, times, duration, scales,
                    storeResult.getScale());
        }

        return storeResult;
    }

    /**
     * Copy a bone track, resampling at the specified times using these
     * techniques.
     *
     * @param oldTrack (not null, unaffected)
     * @param newTimes sample times (not null, alias created)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public BoneTrack resample(BoneTrack oldTrack, float[] newTimes,
            float duration) {
        Validate.nonNegative(duration, "duration");

        int boneIndex = oldTrack.getTargetBoneIndex();
        int numSamples = newTimes.length;
        Vector3f[] newTranslations = new Vector3f[numSamples];
        Quaternion[] newRotations = new Quaternion[numSamples];
        Vector3f[] newScales = null;
        Vector3f[] oldScales = oldTrack.getScales();
        if (oldScales != null) {
            newScales = new Vector3f[numSamples];
        }

        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = newTimes[frameIndex];
            Transform boneTransform;
            boneTransform = boneTransform(oldTrack, time, duration, null);
            newTranslations[frameIndex] = boneTransform.getTranslation();
            newRotations[frameIndex] = boneTransform.getRotation();
            if (oldScales != null) {
                newScales[frameIndex] = boneTransform.getScale();
            }
        }

        BoneTrack result = MyAnimation.newBoneTrack(boneIndex, newTimes,
                newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone track, resampling it at the specified rate using these
     * techniques.
     *
     * @param oldTrack (not null, unaffected)
     * @param sampleRate sample rate (in frames per second, &gt;0)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public BoneTrack resampleAtRate(BoneTrack oldTrack, float sampleRate,
            float duration) {
        Validate.positive(sampleRate, "sample rate");
        Validate.nonNegative(duration, "duration");

        int numSamples = 1 + (int) Math.floor(duration * sampleRate);
        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = frameIndex / sampleRate;
            if (time > duration) {
                time = duration;
            }
            newTimes[frameIndex] = time;
        }
        BoneTrack result = resample(oldTrack, newTimes, duration);

        return result;
    }

    /**
     * Copy a bone track, resampling to the specified number of samples using
     * these techniques.
     *
     * @param oldTrack (not null, unaffected)
     * @param numSamples number of samples (&ge;2)
     * @param duration animation duration (in seconds, &gt;0)
     * @return a new instance
     */
    public BoneTrack resampleToNumber(BoneTrack oldTrack, int numSamples,
            float duration) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        Validate.positive(duration, "duration");

        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time;
            if (frameIndex == numSamples - 1) {
                time = duration;
            } else {
                time = (frameIndex * duration) / (numSamples - 1);
            }
            newTimes[frameIndex] = time;
        }
        BoneTrack result = resample(oldTrack, newTimes, duration);

        return result;
    }

    /**
     * Alter the technique for rotations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenRotations(TweenRotations newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenRotations = newTechnique;
    }

    /**
     * Alter the technique for scales.
     *
     * @param newTechnique (not null)
     */
    public void setTweenScales(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenScales = newTechnique;
    }

    /**
     * Alter the technique for translations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenTranslations(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenTranslations = newTechnique;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public TweenTransforms clone() throws CloneNotSupportedException {
        TweenTransforms clone = (TweenTransforms) super.clone();
        return clone;
    }
}