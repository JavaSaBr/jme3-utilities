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
package jme3utilities.nifty.dialog;

import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;

/**
 * Controller for a text-entry dialog box used to input a double-precision
 * floating-point value.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DoubleDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DoubleDialog.class.getName());
    /**
     * pattern for matching the word "null"
     */
    final private static Pattern nullPattern
            = Pattern.compile("\\s*null\\s*");
    // *************************************************************************
    // fields

    /**
     * if true, "null" is an allowed value, otherwise it is disallowed
     */
    final private boolean allowNull;
    /**
     * maximum value to commit
     */
    final private double maxValue;
    /**
     * minimum value to commit
     */
    final private double minValue;
    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit description (not null, not empty)
     * @param min minimum value (&lt;max)
     * @param max minimum value (&gt;min)
     * @param allowNull if true, "null" will be an allowed value
     */
    public DoubleDialog(String description, double min, double max,
            boolean allowNull) {
        Validate.nonEmpty(description, "description");
        assert min < max : max;

        commitDescription = description;
        minValue = min;
        maxValue = max;
        this.allowNull = allowNull;
    }
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = getText(dialogElement);
        boolean result;
        try {
            double inputValue = Double.parseDouble(text);
            if (inputValue < minValue || inputValue > maxValue) {
                result = false;
            } else if (Double.isNaN(inputValue)) {
                result = false;
            } else {
                result = true;
            }
        } catch (NumberFormatException e) {
            result = false;
        }
        String lcText = text.toLowerCase(Locale.ROOT);
        if (!result && allowNull && matchesNull(lcText)) {
            result = true;
        }

        return result;
    }

    /**
     * Callback to update the dialog box prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param dialogElement (not null)
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(Element dialogElement, float elapsedTime) {
        Validate.nonNull(dialogElement, "dialog element");

        String commitLabel = "";
        String feedbackMessage = "";

        String text = getText(dialogElement);
        try {
            double inputValue = Double.parseDouble(text);
            if (inputValue < minValue) {
                String minText = Double.toString(minValue);
                feedbackMessage = String.format("must not be < %s", minText);
            } else if (inputValue > maxValue) {
                String maxText = Double.toString(maxValue);
                feedbackMessage = String.format("must not be > %s", maxText);
            } else if (Double.isNaN(inputValue)) {
                feedbackMessage = notANumber();
            } else {
                commitLabel = commitDescription;
            }
        } catch (NumberFormatException e) {
            feedbackMessage = notANumber();
        }
        String lcText = text.toLowerCase(Locale.ROOT);
        if (allowNull && matchesNull(lcText)) {
            feedbackMessage = "";
            commitLabel = commitDescription;
        }

        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);
        commitButton.setText(commitLabel);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackMessage);
    }
    // *************************************************************************
    // private methods

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    private String getText(Element dialogElement) {
        TextField textField
                = dialogElement.findNiftyControl("#textfield", TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }

    /**
     * Test whether the specified string matches nullPattern.
     *
     * @param lcText text string (not null, assumed to be in lower case)
     * @return true for match, otherwise false
     */
    private boolean matchesNull(String lcText) {
        assert lcText != null;

        Matcher matcher = nullPattern.matcher(lcText);
        boolean result = matcher.matches();

        return result;
    }

    /**
     * Generate a feedback message when the text does not represent a number.
     *
     * @return message text (not null, not empty)
     */
    private String notANumber() {
        if (allowNull) {
            return "must be a number or null";
        } else {
            return "must be a number";
        }
    }
}
