package squidpony.squidgrid.gui.gdx;

import com.badlogic.gdx.graphics.Color;

/**
 * A group of nested classes under the empty interface Filters, these all are meant to perform different changes
 * to colors before they are created (they should be passed to SquidColorCenter's constructor, which can use them).
 * Created by Tommy Ettinger on 10/31/2015.
 */
public interface Filters {

    /**
     * A Filter that does nothing to the colors it is given but pass them along unchanged.
     */
    class IdentityFilter extends Filter<Color>
    {
        public IdentityFilter()
        {
            state = new float[0];
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            return new Color(r, g, b, a);
        }
    }

    /**
     * A Filter that converts all colors passed to it to grayscale, like a black and white film.
     */
    class GrayscaleFilter extends Filter<Color>
    {
        public GrayscaleFilter()
        {
            state = new float[0];
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            float v = (r + g + b) / 3f;
            return new Color(v, v, v, a);
        }
    }

    /**
     * A Filter that tracks the highest brightness for any component it was assigned and stores it in state as the first
     * and only element.
     */
    class MaxValueFilter extends Filter<Color>
    {
        public MaxValueFilter()
        {
            state = new float[]{0};
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            state[0] = Math.max(state[0], Math.max(r, Math.max(g, b)));
            return new Color(r, g, b, a);
        }

    }

    /**
     * A Filter that performs a brightness adjustment to make dark areas lighter and light areas not much less bright.
     */
    class GammaCorrectFilter extends Filter<Color> {
        /**
         * Sets up a GammaCorrectFilter with the desired gamma adjustment.
         *
         * @param gamma    should be 1.0 or less, and must be greater than 0. Typical values are between 0.4 to 0.8.
         * @param maxValue the maximum brightness in the colors this will be passed; use MaxValueFilter for this
         */
        public GammaCorrectFilter(float gamma, float maxValue) {
            state = new float[]{gamma, 1f / (float) Math.pow(maxValue, gamma)};
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            return new Color(state[1] * (float) Math.pow(r, state[0]),
                    state[1] * (float) Math.pow(g, state[0]),
                    state[1] * (float) Math.pow(b, state[0]),
                    a);
        }
    }
    /**
     * A Filter that is constructed with a color and linear-interpolates any color it is told to alter toward the color
     * it was constructed with.
     */
    class LerpFilter extends Filter<Color> {
        /**
         * Sets up a LerpFilter with the desired color to linearly interpolate towards.
         *
         * @param r the red component to lerp towards
         * @param g the green component to lerp towards
         * @param b the blue component to lerp towards
         * @param a the opacity component to lerp towards
         * @param amount the amount to lerp by, should be between 0.0 and 1.0
         */
        public LerpFilter(float r, float g, float b, float a, float amount) {
            state = new float[]{r, g, b, a, amount};
        }
        /**
         * Sets up a LerpFilter with the desired color to linearly interpolate towards.
         *
         * @param color the Color to lerp towards
         * @param amount the amount to lerp by, should be between 0.0 and 1.0
         */
        public LerpFilter(Color color, float amount) {
            state = new float[]{color.r, color.g, color.b, color.a, amount};
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            return new Color(r, g, b, a).lerp(state[0], state[1], state[2], state[3], state[4]);
        }
    }
    /**
     * A Filter that is constructed with a group of colors and linear-interpolates any color it is told to alter toward
     * the color it was constructed with that has the closest hue.
     */
    class MultiLerpFilter extends Filter<Color> {
        private SquidColorCenter globalSCC;
        /**
         * Sets up a MultiLerpFilter with the desired colors to linearly interpolate towards; the lengths of each given
         * array should be identical.
         *
         * @param r the red components to lerp towards
         * @param g the green components to lerp towards
         * @param b the blue components to lerp towards
         * @param a the opacity components to lerp towards
         * @param amount the amounts to lerp by, should each be between 0.0 and 1.0
         */
        public MultiLerpFilter(float[] r, float[] g, float[] b, float[] a, float[] amount) {
            state = new float[Math.min(r.length, Math.min(g.length, Math.min(b.length,
                    Math.min(a.length, amount.length)))) * 6];
            globalSCC = DefaultResources.getSCC();
            for (int i = 0; i < state.length / 6; i++) {
                state[i * 6] = r[i];
                state[i * 6 + 1] = g[i];
                state[i * 6 + 2] = b[i];
                state[i * 6 + 3] = a[i];
                state[i * 6 + 4] = amount[i];
                state[i * 6 + 5] = globalSCC.getHue(r[i], g[i], b[i]);
            }
        }/**
         * Sets up a MultiLerpFilter with the desired colors to linearly interpolate towards and their amounts.
         *
         * @param colors the Colors to lerp towards
         * @param amount the amounts to lerp by, should each be between 0.0 and 1.0
         */
        public MultiLerpFilter(Color[] colors, float[] amount) {
            state = new float[Math.min(colors.length, amount.length) * 6];
            globalSCC = DefaultResources.getSCC();
            for (int i = 0; i < state.length / 6; i++) {
                state[i * 6] = colors[i].r;
                state[i * 6 + 1] = colors[i].g;
                state[i * 6 + 2] = colors[i].b;
                state[i * 6 + 3] = colors[i].a;
                state[i * 6 + 4] = amount[i];
                state[i * 6 + 5] = globalSCC.getHue(colors[i]);
            }
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            float givenH = globalSCC.getHue(r, g, b), givenS = globalSCC.getSaturation(r, g, b),
                    minDiff = 999.0f, temp;
            if(givenS < 0.05)
                return new Color(r, g, b, a);
            int choice = 0;
            for (int i = 5; i < state.length; i += 6) {
                temp = state[i] - givenH;
                temp = (temp >= 0.5f) ? Math.abs(temp - 1f) % 1f : Math.abs(temp);
                if(temp < minDiff) {
                    minDiff = temp;
                    choice = i / 6; // rounds down
                }
            }
            return new Color(r, g, b, a).lerp(state[choice * 6], state[choice * 6 + 1], state[choice * 6 + 2],
                    state[choice * 6 + 3], state[choice * 6 + 4]);
        }
    }

    /**
     * A Filter that is constructed with a color and makes any color it is told to alter have the same hue as the given
     * color, have saturation that is somewhere between the given color's and the altered colors, and chiefly is
     * distinguishable from other colors by value. Useful for sepia effects, which can be created satisfactorily with
     * {@code new Filters.ColorizeFilter(SColor.CLOVE_BROWN, 0.6f, 0.0f)}.
     */
    class ColorizeFilter extends Filter<Color> {
        private SquidColorCenter globalSCC;
        /**
         * Sets up a ColorizeFilter with the desired color to colorize towards.
         *
         * @param r the red component to colorize towards
         * @param g the green component to colorize towards
         * @param b the blue component to colorize towards
         */
        public ColorizeFilter(float r, float g, float b) {
            globalSCC = DefaultResources.getSCC();

            state = new float[]{globalSCC.getHue(r, g, b), globalSCC.getSaturation(r, g, b), 1f, 0f};
        }
        /**
         * Sets up a ColorizeFilter with the desired color to colorize towards.
         *
         * @param color the Color to colorize towards
         */
        public ColorizeFilter(Color color) {
            globalSCC = DefaultResources.getSCC();
            state = new float[]{globalSCC.getHue(color), globalSCC.getSaturation(color), 1f, 0f};
        }
        /**
         * Sets up a ColorizeFilter with the desired color to colorize towards.
         *
         * @param r the red component to colorize towards
         * @param g the green component to colorize towards
         * @param b the blue component to colorize towards
         * @param saturationMultiplier a multiplier to apply to the final color's saturation; may be greater than 1
         * @param valueModifier a modifier that affects the final brightness value of any color this alters;
         *                      typically very small, such as in the -0.2f to 0.2f range
         */
        public ColorizeFilter(float r, float g, float b, float saturationMultiplier, float valueModifier) {
            globalSCC = DefaultResources.getSCC();

            state = new float[]{
                    globalSCC.getHue(r, g, b),
                    globalSCC.getSaturation(r, g, b),
                    saturationMultiplier,
                    valueModifier};
        }
        /**
         * Sets up a ColorizeFilter with the desired color to colorize towards.
         *
         * @param color the Color to colorize towards
         * @param saturationMultiplier a multiplier to apply to the final color's saturation; may be greater than 1
         * @param valueModifier a modifier that affects the final brightness value of any color this alters;
         *                      typically very small, such as in the -0.2f to 0.2f range
         */
        public ColorizeFilter(Color color, float saturationMultiplier, float valueModifier) {
            globalSCC = DefaultResources.getSCC();
            state = new float[]{
                    globalSCC.getHue(color),
                    globalSCC.getSaturation(color),
                    saturationMultiplier,
                    valueModifier};
        }

        @Override
        public Color alter(float r, float g, float b, float a) {
            return globalSCC.getHSV(
                    state[0],
                    Math.max(0f, Math.min((globalSCC.getSaturation(r, g, b) + state[1]) * 0.5f * state[2], 1f)),
                    globalSCC.getValue(r, g, b) * (1f - state[3]) + state[3],
                    a);
        }
    }
    /**
     * A Filter that makes the colors requested from it highly saturated, with the original value and a timer that
     * increments very slowly altering hue, and the original hue altering value. It should look like a hallucination.
     *
     * A short (poorly recorded) video can be seen here http://i.imgur.com/SEw2LXe.gifv ; performance should be smoother
     * during actual gameplay.
     */
    class HallucinateFilter extends Filter<Color> {
        private SquidColorCenter globalSCC;
        /**
         * Sets up a HallucinateFilter with the timer at 0..
         */
        public HallucinateFilter() {
            globalSCC = DefaultResources.getSCC();

            state = new float[]{0f};
        }
        @Override
        public Color alter(float r, float g, float b, float a) {
            state[0] += 0.00003f;
            if(state[0] >= 1.0f)
                state[0] = 0f;
            float h = globalSCC.getHue(r, g, b),
                    s = globalSCC.getSaturation(r, g, b),
                    v = globalSCC.getValue(r, g, b);
            return globalSCC.getHSV(
                    (v * 4f + h + state[0]) % 1.0f,
                    Math.max(0f, Math.min((h + v) * 0.65f + state[0] * 0.4f, 1f)),
                    (h + v + s) * 0.35f + 0.7f,
                    a);
        }
    }
}
