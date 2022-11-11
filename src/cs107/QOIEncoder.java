package cs107;

import java.util.ArrayList;

/**
 * "Quite Ok Image" Encoder
 *
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.0
 * @apiNote Second task of the 2022 Mini Project
 * @since 1.0
 */
public final class QOIEncoder {

    /**
     * Here follow constants and indexes used in the encoding methods.
     * First are typical indexes of the LUMA difference arrays, followed by offset and limit values
     * for handling them and RGB difference arrays.
     * The constants are used by {@link #qoiOpLuma(byte[])}, {@link #isValidLumaDiff(byte[])},
     * {@link #qoiOpDiff(byte[])} and {@link #isValidRGBdiff(byte[])}
     */
    private final static byte DRi             = 0;

    // ==================================================================================
    // ============================ QUITE OK IMAGE HEADER ===============================
    // ==================================================================================
    private final static byte DGi             = 1;

    // ==================================================================================
    // ============================ ATOMIC ENCODING METHODS =============================
    // ==================================================================================
    private final static byte DBi             = 2;
    private final static byte OFF_LIM_DG_HI   = 32;
    private final static byte LIM_DG_LO       = -33;
    private final static byte OFF_LIM_DRDB_HI = 8;
    private final static byte LIM_DRDB_LO     = -9;
    private final static byte LIM_DIFF_LO     = -2;
    private final static byte LIM_DIFF_HI     = 1;
    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private QOIEncoder() {
    }

    /**
     * Generate a "Quite Ok Image" header using the following parameters
     *
     * @param image (Helper.Image) - Image to use
     * @return (byte[]) - Corresponding "Quite Ok Image" Header
     * @throws AssertionError if the colorspace or the number of channels is corrupted or if the image is null.
     *                        (See the "Quite Ok Image" Specification or the handouts of the project for more information)
     * @author Elie BRUNO (elie.bruno@epfl.ch)
     */
    public static byte[] qoiHeader(Helper.Image image) {
        assert image != null : "image is null";
        assert image.channels() == QOISpecification.RGB ||
               image.channels() == QOISpecification.RGBA : "The image channels are corrupted";
        assert image.color_space() == QOISpecification.sRGB ||
               image.color_space() == QOISpecification.ALL : "The image color space is corrupted";

        return ArrayUtils.concat(
                QOISpecification.QOI_MAGIC,
                ArrayUtils.fromInt(image.data()[0].length),
                ArrayUtils.fromInt(image.data().length),
                ArrayUtils.concat(image.channels(), image.color_space()));
    }

    /**
     * Encode the given pixel using the QOI_OP_RGB block
     *
     * @param pixel (byte[]) - The Pixel to encode
     * @return (byte[]) - Encoding of the pixel using the QOI_OP_RGB block
     * @throws AssertionError if the pixel's length is not 4
     * @author Sebastian Kugler (362022)
     */
    public static byte[] qoiOpRGB(byte[] pixel) {
        assert pixel.length == 4 : "The length of the input pixel array is not 4";

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGB_TAG,
                pixel[QOISpecification.r], pixel[QOISpecification.g], pixel[QOISpecification.b]);
    }

    /**
     * Encode the given pixel using the QOI_OP_RGBA block
     *
     * @param pixel (byte[]) - The pixel to encode
     * @return (byte[]) Encoding of the pixel using the QOI_OP_RGBA block
     * @throws AssertionError if the pixel's length is not 4
     * @author Sebastian Kugler (362022)
     */
    public static byte[] qoiOpRGBA(byte[] pixel) {
        assert pixel.length == 4 : "The length of the input pixel array is not 4";

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGBA_TAG,
                pixel[QOISpecification.r], pixel[QOISpecification.g],
                pixel[QOISpecification.b], pixel[QOISpecification.a]);
    }

    /**
     * Encode the index using the QOI_OP_INDEX block
     *
     * @param index (byte) - Index of the pixel
     * @return (byte[]) - Encoding of the index using the QOI_OP_INDEX block
     * @throws AssertionError if the index is outside the range of all possible indices
     * @author Sebastian Kugler (362022)
     */
    public static byte[] qoiOpIndex(byte index) {
        assert (index < 64) && (index >= 0) :
                "The input hash table index is outside of the allowed range.";

        // 63 is 0b00_11_11_11 and index >=0 : simple addition is enough here
        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_INDEX_TAG + index));
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_DIFF block
     *
     * @param diff (byte[]) - The difference between 2 pixels
     * @return (byte[]) - Encoding of the given difference in a QOI_OP_DIFF block
     * @throws AssertionError if diff doesn't respect the constraints or diff's length is not 3
     *                        (See the handout for the constraints)
     * @author Sebastian Kugler (362022)
     */
    public static byte[] qoiOpDiff(byte[] diff) {
        assert diff != null && diff.length == 3 : "The length of the input diff is not 3";

        byte binEncodedOutput = QOISpecification.QOI_OP_DIFF_TAG;

        for (int i = 0; i < 3; i++) {
            assert diff[i] >= LIM_DIFF_LO && diff[i] <= LIM_DIFF_HI :
                    "A difference value is outside of this block's allowed range.";
            // offset each diff value as per spec (diff array is mutable, only used here)
            diff[i] += 2;
            // shift the diff value to the correct position in the byte block
            binEncodedOutput += (byte) (diff[i] << (4 - (i * 2)));
        }

        return ArrayUtils.wrap(binEncodedOutput);
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_LUMA block
     *
     * @param diff (byte[]) - The difference between 2 pixels
     * @return (byte[]) - Encoding of the given difference in a QOI_OP_LUMA block
     * @throws AssertionError if diff doesn't respect the constraints
     *                        or diff's length is not 3
     *                        (See the handout for the constraints)
     * @author Sebastian Kugler (362022)
     */
    public static byte[] qoiOpLuma(byte[] diff) {
        assert diff != null && diff.length == 3 : "The length of the input diff is not 3";

        byte[] binEncodedOutput = new byte[]{QOISpecification.QOI_OP_LUMA_TAG, 0};
        byte[] lumaDiff         = calcLumaDiff(diff);

        assert isValidLumaDiff(lumaDiff) : "The RGB difference is out of this block's accepted range.";

        binEncodedOutput[0] += (byte) (lumaDiff[DGi] + OFF_LIM_DG_HI);
        binEncodedOutput[1] = (byte) ((lumaDiff[DRi] + OFF_LIM_DRDB_HI) << 4);
        binEncodedOutput[1] += lumaDiff[DBi] + OFF_LIM_DRDB_HI;

        return binEncodedOutput;
    }

    /**
     * Encode the number of similar pixels using the QOI_OP_RUN block
     *
     * @param count (byte) - Number of similar pixels
     * @return (byte[]) - Encoding of count in a QOI_OP_RUN block
     * @throws AssertionError if count is not between 0 (exclusive) and 63 (exclusive)
     * @author Sebastian Kugler (362022)
     */
    public static byte[] qoiOpRun(byte count) {
        final byte COUNT_OFFSET = -1;
        assert (count > 0) && (count < 63) : "The input run count is outside of the allowed range.";

        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_RUN_TAG + (count + COUNT_OFFSET)));
    }

    // ==================================================================================
    // =================================== Helper methods ===============================
    // ==================================================================================

    /**
     * Calculate the difference between 2 pixels' respective R, G and B channels
     *
     * @param pixel1 (byte[]) - First pixel (in RGB(A) format)
     * @param pixel2 (byte[]) - Second pixel (in RGB(A) format)
     * @return (byte[]) - Difference of the 2 pixels' respective R, G and B channels
     * @author Sebastian Kugler (362022)
     */
    public static byte[] calcRGBdiff(byte[] pixel1, byte[] pixel2) {
        byte[] pixelDiff = new byte[3];

        for (int i = 0; i < 3; i++)
             pixelDiff[i] = (byte) (pixel1[i] - pixel2[i]);

        return pixelDiff;
    }

    /**
     * Calculate the LUMA difference from a given RGB difference
     *
     * @param pixelDiff (byte[]) - Difference between pixels' respective R, G and B channels
     *                  as calculated by {@link QOIEncoder#calcRGBdiff(byte[], byte[])}
     * @return (byte[]) - LUMA difference without offset in DR-dg, DG and DB-dg channels
     * @author Sebastian Kugler (362022)
     */
    public static byte[] calcLumaDiff(byte[] pixelDiff) {
        return new byte[]{
                (byte) (pixelDiff[QOISpecification.r] - pixelDiff[QOISpecification.g]),
                pixelDiff[QOISpecification.g],
                (byte) (pixelDiff[QOISpecification.b] - pixelDiff[QOISpecification.g])};
    }

    /**
     * Check the QOI_OP_DIFF block constraints for a given RGB difference
     *
     * @param pixelDiff (byte[]) - Difference between pixels' respective R, G and B channels
     *                  as calculated by {@link QOIEncoder#calcRGBdiff(byte[], byte[])}
     * @return (boolean) - True if the difference is within the
     * QOI_OP_DIFF block constraints, false otherwise
     * @author Sebastian Kugler (362022)
     */
    public static boolean isValidRGBdiff(byte[] pixelDiff) {
        for (byte diff : pixelDiff)
            if (diff < LIM_DIFF_LO || diff > LIM_DIFF_HI) {
                return false;
            }
        return true;
    }

    /**
     * Check the QOI_OP_LUMA block constraints for a given LUMA difference.
     *
     * @param lumaDiff (byte[]) - Luma Difference of a pixel pair
     *                 as calculated by {@link QOIEncoder#calcLumaDiff(byte[])}
     * @return (boolean) - True if the QOI_OP_LUMA block constraints are met, false otherwise.
     * @author Sebastian Kugler (362022)
     */
    public static boolean isValidLumaDiff(byte[] lumaDiff) {

        return (
                lumaDiff[DRi] > LIM_DRDB_LO && lumaDiff[DRi] < OFF_LIM_DRDB_HI &&
                lumaDiff[DGi] > LIM_DG_LO && lumaDiff[DGi] < OFF_LIM_DG_HI &&
                lumaDiff[DBi] > LIM_DRDB_LO && lumaDiff[DBi] < OFF_LIM_DRDB_HI
        );
    }

    // ==================================================================================
    // ============================== GLOBAL ENCODING METHODS  ==========================
    // ==================================================================================

    /**
     * Encode the given image using the "Quite Ok Image" Protocol
     * (See handout for more information about the "Quite Ok Image" protocol)
     *
     * @param image (byte[][]) - Formatted image to encode
     * @return (byte[]) - "Quite Ok Image" representation of the image
     * @author Sebastian Kugler (362022)
     */
    public static byte[] encodeData(byte[][] image) {


        // Initialization
        byte[]            prevPixel     = QOISpecification.START_PIXEL;
        byte[][]          hashTable     = new byte[64][4];
        int               runCounter    = 0;
        ArrayList<byte[]> encodedPixels = new ArrayList<>();

        // Statistics:
        // Indexes: 0-QOI_OP_RUN,   1-QIO_OP_INDEX, 2-QOI_OP_DIFF,
        //          3-QOI_OP_LUMA,  4-QOI_OP_RGB,   5-QOI_OP_RGBA)
        int[] stats = new int[6];

        // Pixel Processing
        for (int i = 0; i < image.length; i++) {

            // ---QOI_OP_RUN---
            if (ArrayUtils.equals(image[i], prevPixel)) {
                runCounter++;

                if (runCounter == 62 || i == (image.length - 1)) {
                    encodedPixels.add(qoiOpRun((byte) runCounter));
                    stats[0] += runCounter;
                                runCounter = 0;
                }
            }
            else {
                if (runCounter > 0) {
                    encodedPixels.add(qoiOpRun((byte) runCounter));
                    stats[0] += runCounter;
                                runCounter = 0;
                }

                // ---QOI_OP_INDEX---
                if (ArrayUtils.equals(image[i], hashTable[QOISpecification.hash(image[i])])) {
                    encodedPixels.add(qoiOpIndex(QOISpecification.hash(image[i])));
                    stats[1]++;
                }
                else {
                    hashTable[QOISpecification.hash(image[i])] = image[i];

                    if (image[i][QOISpecification.a] == prevPixel[QOISpecification.a]) {
                        byte[] pixelDiff = calcRGBdiff(image[i], prevPixel);

                        // ---QOI_OP_DIFF---
                        if (isValidRGBdiff(pixelDiff)) {
                            encodedPixels.add(qoiOpDiff(pixelDiff));
                            stats[2]++;
                        }

                        // ---QOI_OP_LUMA---
                        else if (isValidLumaDiff(calcLumaDiff(pixelDiff))) {
                            encodedPixels.add(qoiOpLuma(pixelDiff));
                            stats[3]++;
                        }

                        // ---QOI_OP_RGB---
                        else {
                            encodedPixels.add(qoiOpRGB(image[i]));
                            stats[4]++;
                        }

                        // ---QOI_OP_RGBA---
                    }
                    else {
                        encodedPixels.add(qoiOpRGBA(image[i]));
                        stats[5]++;
                    }

                }
            }
            prevPixel = image[i];
        }

        // Statistics
        System.out.println("====== Encoding Statistics ======");
        System.out.println("    Method    |   Pixels encoded");
        System.out.println("QOI_OP_RUN    |   " + stats[0]);
        System.out.println("QOI_OP_INDEX  |   " + stats[1]);
        System.out.println("QOI_OP_DIFF   |   " + stats[2]);
        System.out.println("QOI_OP_LUMA   |   " + stats[3]);
        System.out.println("QOI_OP_RGB    |   " + stats[4]);
        System.out.println("QOI_OP_RGBA   |   " + stats[5]);
        System.out.println("=================================");

        // flatten the 2D array to 1D
        return ArrayUtils.concat(encodedPixels.toArray(new byte[0][0]));
    }

    /**
     * Creates the representation in memory of the "Quite Ok Image" file.
     *
     * @param image (Helper.Image) - Image to encode
     * @return (byte[]) - Binary representation of the "Quite Ok File" of the image
     * @throws AssertionError if the image is null
     * @author Elie BRUNO (elie.bruno@epfl.ch)
     * @apiNote THE FILE IS NOT CREATED YET, THIS IS JUST ITS REPRESENTATION.
     * TO CREATE THE FILE, YOU'LL NEED TO CALL Helper::write
     */
    public static byte[] qoiFile(Helper.Image image) {
        assert image != null : "The image is null.";
        return ArrayUtils.concat(qoiHeader(image),
                encodeData(ArrayUtils.imageToChannels(image.data())),
                QOISpecification.QOI_EOF);
    }
}