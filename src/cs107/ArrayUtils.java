package cs107;

import java.util.ArrayList;
// import java.util.Arrays;

/**
 * Utility class to manipulate arrays.
 * @apiNote First Task of the 2022 Mini Project
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.2
 * @since 1.0
 */
public final class ArrayUtils {

    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private ArrayUtils(){}

    // ==================================================================================
    // =========================== ARRAY EQUALITY METHODS ===============================
    // ==================================================================================

    /**
     * Check if the content of both arrays is the same
     * @param a1 (byte[]) - First array
     * @param a2 (byte[]) - Second array
     * @return (boolean) - true if both arrays have the same content (or both null), false otherwise
     * @throws AssertionError if one of the parameters is null
     */
    public static boolean equals (byte[] a1, byte[] a2) {

        assert (a1 != null) == (a2 != null) : "Only one of the arrays is null";

        // If both are null, then they are equal (only check one array because of the previous assertion)
        if (a1 == null) return true;

        if (a1.length != a2.length) return false;

        for (int i = 0; i < a1.length; i++)
            if (a1[i] != a2[i]) return false;

        return true;
    }

    /**
     * Check if the content of both arrays is the same
     * @param a1 (byte[][]) - First array
     * @param a2 (byte[][]) - Second array
     * @return (boolean) - true if both arrays have the same content (or both null), false otherwise
     * @throws AssertionError if one of the parameters is null
     * @author Elie BRUNO (elie.bruno@epfl.ch )
     *
     * <ul>
     *     <li>
     *     First we are going to check if one of our table is null but not both.
     *     </li><li>
     *     Then we check if both of them are null, if so we return true as they have the same size
     *</li>
     * <li>
     *     To finish we loop through every lines of our table and use the {@code equals(byte[] a1, byte[]
     *     a2)} function in every line to check if the arrays are equal.     <ul>
     *         <li>If they are we {@code return} true</li>
     *         <li>Otherwise we {@code return} false</li>
     *         </ul>
     * </li>
     * </ul>
     */
    public static boolean equals(byte[][] a1, byte[][] a2){
        assert (a1 != null) == (a2 != null) : "Only one of the arrays is null";
        // If both are null, then they are equal (only check one array because of the previous assertion)
        if (a1 == null) {
            return true;
        }
        if (a1.length!= a2.length){
            return false;
        }
        for (int i = 0; i < a1.length; i++){

            if (!equals(a1[i], a2[i])){
                return false;
            }
        }
        return true;
    }

    // ==================================================================================
    // ============================ ARRAY WRAPPING METHODS ==============================
    // ==================================================================================

    /**
     * Wrap the given value in an array
     * @param value (byte) - value to wrap
     * @return (byte[]) - array with one element (value)
     */
    public static byte[] wrap(byte value){
        byte[] wrappedArray = new byte[1];
        wrappedArray[0] = value;
        return wrappedArray;
    }

    // ==================================================================================
    // ========================== INTEGER MANIPULATION METHODS ==========================
    // ==================================================================================

    /**
     * Create an Integer using the given array. The input needs to be considered
     * as "Big Endian"
     * (See handout for the definition of "Big Endian")
     * @param bytes (byte[]) - Array of 4 bytes
     * @return (int) - Integer representation of the array
     * @throws AssertionError if the input is null or the input's length is different from 4
     */
    public static int toInt(byte[] bytes) {
        assert bytes != null : "The array is null ";
        assert (bytes.length == 4 ) : "The array does not contain 4 elements";
        int output  = 0;
        int shift = 24;
        for (byte aByte : bytes) {
            output += aByte << shift;
            shift -= 8;
        }

        return output;
    }


    /**
     * Separate the Integer (word) to 4 bytes. The Memory layout of this integer is "Big Endian"
     * (See handout for the definition of "Big Endian")
     * @param value (int) - The integer
     * @return (byte[]) - Big Endian representation of the integer
     */
    public static byte[] fromInt(int value){
        // add asserts?
        byte[] output = new byte[4];
        int shift = 24;

        for (int i = 0; i < output.length; i++) {
            output[i] = (byte) (value >> shift);
            shift -= 8;
        }

        return output;
    }

    // ==================================================================================
    // ========================== ARRAY CONCATENATION METHODS ===========================
    // ==================================================================================

    /**
     * Concatenate a given sequence of bytes and stores them in an array
     * @param bytes (byte ...) - Sequence of bytes to store in the array
     * @return (byte[]) - Array representation of the sequence
     * @throws AssertionError if the input is null
     */
    public static byte[] concat(byte ... bytes){
        assert !(bytes == null ) : "The input is null";
        return bytes;
    }

    /**
     * Concatenate a given sequence of arrays into one array
     * @param tabs (byte[] ...) - Sequence of arrays
     * @return (byte[]) - Array representation of the sequence
     * @throws AssertionError if the input is null
     * or one of the inner arrays of input is null.
     */
    public static byte[] concat(byte[] ... tabs){
        assert !(tabs == null ) : "The input is null";
        for (byte[] b : tabs) {
            assert !(b == null) : "An input is null";
        }

        ArrayList<Byte> outputList = new ArrayList<>();

        for (byte[] bArray : tabs)
            for (byte b : bArray) outputList.add(b);

        byte[] output = new byte[outputList.size()];
        for (int i = 0; i < outputList.size(); i++) output[i] = outputList.get(i);

        return output;
    }

    // ==================================================================================
    // =========================== ARRAY EXTRACTION METHODS =============================
    // ==================================================================================

    /**
     * Extract an array from another array
     * @param input (byte[]) - Array to extract from
     * @param start (int) - Index in the input array to start the extract from
     * @param length (int) - The number of bytes to extract
     * @return (byte[]) - The extracted array
     * @throws AssertionError if the input is null or start and length are invalid.
     * start + length should also be smaller than the input's length
     */
    @SuppressWarnings("unused")
    public static byte[] extract (byte[] input, int start, int length){

        assert input != null : "The input array is null";
        assert (0 <= start && start < input.length && length >= 0 && start + length <= input.length) : "The given positions are invalid";

        byte[] output = new byte[length];

        for (int i = 0; i < length; i++) output[i] = input[start + i];

        return output;
    }

    /**
     * Create a partition of the input array.
     * (See handout for more information on how this method works)
     * @author Elie BRUNO (elie.bruno@epfl.ch)
     * @param input (byte[]) - The original array
     * @param sizes (int ...) - Sizes of the partitions
     * @return (byte[][]) - Array of input's partitions.
     * The order of the partition is the same as the order in sizes
     * @throws AssertionError if one of the parameters is null
     * or the sum of the elements in sizes is different from the input's length
     */
    @SuppressWarnings("unused")
    public static byte[][] partition(byte[] input, int ... sizes) {
        assert input != null: "Tab is null";
        assert sizes != null: "Sizes is null";
        int sum = 0;
        for (int value : sizes){
            sum += value;
        }
        assert (sum == input.length) : "The Sum of Integers in sizes is different than tab.length";

        byte[][] partitioned = new byte[sizes.length][1];
        int start = 0;
        for (int i = 0; i < sizes.length; ++i){
            partitioned[i]= extract(input, start,sizes[i]);
            start += sizes[i];
        }
        return partitioned;
    }


    // ==================================================================================
    // ============================== ARRAY FORMATTING METHODS ==========================
    // ==================================================================================


    /**
     * These functions have been created for the modularity of the code.
     * They are used to convert one pixel from an ARGB integer representation to an RGBA byte array and back.
     * @param input The pixel in ARGB integer representation
     * @return The pixel in RGBA byte array representation
     *
     * @author Elie BRUNO (elie.bruno@epfl.ch)
     *
     */
    public static byte[] rgbaFromInt(int input){
        byte[] inputAsByte = fromInt(input);
        return new byte[]{inputAsByte[1], inputAsByte[2], inputAsByte[3], inputAsByte[0]};
    }

    /**
     * This is a reverse function of the above and converts a pixel from RGBA byte array to ARGB integer representation.
     * @param input The pixel in RGBA byte array representation
     * @return The pixel in ARGB integer representation
     *
     * @author Elie BRUNO (elie.bruno@epfl.ch)
     *
     */

    public static int rgbaToInt(byte[] input){
        byte[] permuted = {input[3], input[0], input[1], input[2]};
        return toInt(permuted);
    }

    /**
     * Format a 2-dim integer array
     * where each dimension is a direction in the image to
     * a 2-dim byte array where the first dimension is the pixel
     * and the second dimension is the channel.
     * See handouts for more information on the format.
     * @param input (int[][]) - image data
     * @return (byte [][]) - formatted image data
     * @throws AssertionError if the input is null
     * or one of the inner arrays of input is null
     * @author Elie BRUNO (elie.bruno@epfl.ch)
     */
    @SuppressWarnings("unused")
    public static byte[][] imageToChannels(int[][] input) {
        assert input != null && input.length > 0 : "The input is null or empty";
        assert input[0] != null && input[0].length > 0 : "The first line of the input is null or empty";
        int lengthPrevLine = input[0].length;
        boolean first = true;
        for (int[] line : input) {
            assert line != null : "At least one input line is null ! ";
            if (first) {
                first = false;
                continue;
            }
            assert line.length == lengthPrevLine : "The lines are not the same length ! ";
            //noinspection ReassignedVariable
            lengthPrevLine = line.length;
            }

        byte[][] output = new byte[input.length * input[0].length][4];
        int flatIndex = 0;
        for (int[] line : input) {
            for (int pixel : line) {
                output[flatIndex] = rgbaFromInt(pixel);
                flatIndex++;
            }
        }
        return output;
    }

    /**
     * Format a 2-dim byte array where the first dimension is the pixel
     * and the second is the channel to a 2-dim int array where the first
     * dimension is the height and the second is the width
     * @param input (byte[][]) : linear representation of the image
     * @param height (int) - Height of the resulting image
     * @param width (int) - Width of the resulting image
     * @return (int[][]) - the image data
     * @throws AssertionError if the input is null
     * or one of the inner arrays of input is null
     * or input's length differs from width * height
     * or height is invalid
     * or width is invalid
     */
    @SuppressWarnings("unused")
    public static int[][] channelsToImage(byte[][] input, int height, int width) {
        assert input != null : "The input is null";
        assert input.length == height * width : "The input's length is different from height * width";
        for (byte[] channels : input) {
            assert channels != null : "At least one channel is null ! ";
            assert channels.length == 4: "The input contains pixels that do not have 4 channels";
        }

        int[][] output = new int[height][width];

        int flatIndex = 0;
        for (int i = 0; i<output.length;i++){
            for (int j = 0; j < output[0].length;j++) {
                output[i][j] = rgbaToInt(input[flatIndex]);
                flatIndex++;
            }
        }


        return output;
    }


}