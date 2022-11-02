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
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private QOIEncoder() {
    }

    // ==================================================================================
    // ============================ QUITE OK IMAGE HEADER ===============================
    // ==================================================================================

    /**
     * Generate a "Quite Ok Image" header using the following parameters
     *
     * @param image (Helper.Image) - Image to use
     * @return (byte[]) - Corresponding "Quite Ok Image" Header
     * @throws AssertionError if the colorspace or the number of channels is corrupted or if the image is null.
     *                        (See the "Quite Ok Image" Specification or the handouts of the project for more information)
     */
    public static byte[] qoiHeader(Helper.Image image) {
        assert image != null : "image is null";
        assert image.channels() == QOISpecification.RGB || image.channels() == QOISpecification.RGBA :
                "The image channels are corrupted";
        assert image.color_space() == QOISpecification.sRGB || image.color_space() == QOISpecification.ALL :
                "The image color space is corrupted";

        return ArrayUtils.concat(
                QOISpecification.QOI_MAGIC,
                ArrayUtils.fromInt(image.data()[0].length),
                ArrayUtils.fromInt(image.data().length),
                ArrayUtils.concat(image.channels(), image.color_space()));
    }

    // ==================================================================================
    // ============================ ATOMIC ENCODING METHODS =============================
    // ==================================================================================

    /**
     * Encode the given pixel using the QOI_OP_RGB schema
     *
     * @param pixel (byte[]) - The Pixel to encode
     * @return (byte[]) - Encoding of the pixel using the QOI_OP_RGB schema
     * @throws AssertionError if the pixel's length is not 4
     */
    public static byte[] qoiOpRGB(byte[] pixel) {
        assert pixel.length == 4 : "The length of the input pixel array is not 4";

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGB_TAG,
                pixel[QOISpecification.r], pixel[QOISpecification.g], pixel[QOISpecification.b]);
    }

    /**
     * Encode the given pixel using the QOI_OP_RGBA schema
     *
     * @param pixel (byte[]) - The pixel to encode
     * @return (byte[]) Encoding of the pixel using the QOI_OP_RGBA schema
     * @throws AssertionError if the pixel's length is not 4
     */
    public static byte[] qoiOpRGBA(byte[] pixel) {
        assert pixel.length == 4 : "The length of the input pixel array is not 4";

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGBA_TAG,
                pixel[QOISpecification.r], pixel[QOISpecification.g], pixel[QOISpecification.b], pixel[QOISpecification.a]);
    }

    /**
     * Encode the index using the QOI_OP_INDEX schema
     *
     * @param index (byte) - Index of the pixel
     * @return (byte[]) - Encoding of the index using the QOI_OP_INDEX schema
     * @throws AssertionError if the index is outside the range of all possible indices
     */
    public static byte[] qoiOpIndex(byte index) {
        assert (index < 64) && (index >= 0) : "The input hash table index is outside of the allowed range.";

        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_INDEX_TAG + index));
    }
    /**
     * Encode the difference between 2 pixels using the QOI_OP_DIFF schema
     *
     * @param diff (byte[]) - The difference between 2 pixels
     * @return (byte[]) - Encoding of the given difference
     * @throws AssertionError if diff doesn't respect the constraints or diff's length is not 3
     *                        (See the handout for the constraints)
     */
    public static byte[] qoiOpDiff(byte[] diff) {
        assert diff != null && diff.length == 3 : "The length of the input diff is not 3";

        byte binEncodedOutput = QOISpecification.QOI_OP_DIFF_TAG;

        for (int i = 0; i < 3; i++) {
            assert diff[i] >= -2 && diff[i] <= 1 : "A difference value is outside of this block's allowed range.";
            diff[i] += 2;

            binEncodedOutput += (byte) (diff[i] << (4 - (i * 2)));
        }

        return ArrayUtils.wrap(binEncodedOutput);
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_LUMA schema
     *
     * @param diff (byte[]) - The difference between 2 pixels
     * @return (byte[]) - Encoding of the given difference
     * @throws AssertionError if diff doesn't respect the constraints
     *                        or diff's length is not 3
     *                        (See the handout for the constraints)
     */

    public static byte[] qoiOpLuma(byte[] diff) {
        assert diff != null && diff.length == 3 : "The length of the input diff is not 3";

        final byte DRi         = 0;
        final byte DGi         = 1;
        final byte DBi         = 2;

        final byte DG_OFFSET   = 32;
        final byte DRDB_OFFSET = 8;

        byte[] binEncodedOutput = new byte[]{QOISpecification.QOI_OP_LUMA_TAG, 0};
        byte[] lumaDiff         = calcLumaDiff(diff);

        assert isValidLumaDiff(lumaDiff) : "The RGB difference is out of this block's accepted range.";

        binEncodedOutput[0] += (byte) (lumaDiff[DGi] + DG_OFFSET);
        binEncodedOutput[1] = (byte) ((lumaDiff[DRi] + DRDB_OFFSET) << 4);
        binEncodedOutput[1] += lumaDiff[DBi] + DRDB_OFFSET;

        return binEncodedOutput;
    }

    /**
     * Encode the number of similar pixels using the QOI_OP_RUN schema
     *
     * @param count (byte) - Number of similar pixels
     * @return (byte[]) - Encoding of count
     * @throws AssertionError if count is not between 0 (exclusive) and 63 (exclusive)
     */
    public static byte[] qoiOpRun(byte count) {
        final byte COUNT_OFFSET = -1;
        assert count > 0 && count < 63 : "The input run count is outside of the allowed range.";

        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_RUN_TAG + (count + COUNT_OFFSET)));
    }

    // ==================================================================================
    // =================================== Helper methods ===============================
    // ==================================================================================

    // this method calculates the difference between two raw input pixels' respective R, G and B channels
    // note: could be improved to return boolean with surcharge problem from the lesson
    public static byte[] calcRGBdiff(byte[] pixel1, byte[] pixel2) {
        byte[] pixelDiff = new byte[3];

        for (int i = 0; i < 3; i++)
             pixelDiff[i] = (byte) (pixel1[i] - pixel2[i]);

        return pixelDiff;
    }

    // this calculates the RGB differences in the QoiOpLuma format
    // wrap-around byte values as always
    // returns byte-array DR-DG, DG, DB-DG (consistent with R G B).
    public static byte[] calcLumaDiff(byte[] pixelDiff) {
        return new byte[]{
                (byte) (pixelDiff[QOISpecification.r] - pixelDiff[QOISpecification.g]),
                pixelDiff[QOISpecification.g],
                (byte) (pixelDiff[QOISpecification.b] - pixelDiff[QOISpecification.g])};
    }

    // checks the QoiOpDiff block constraints on diff input
    public static boolean isValidRGBdiff(byte[] pixelDiff) {
        for (byte diff : pixelDiff)
            if (diff < -2 || diff > 1) {
                return false;
            }
        return true;
    }

    // checks the QoiOpLuma block constraints on pixel diff array input
    // the R G B indexes are the same as in the QOI spec, maybe remove the index constants?
    public static boolean isValidLumaDiff(byte[] lumaDiff) {

        final byte DRi         = 0;
        final byte DGi         = 1;
        final byte DBi         = 2;

        final byte LOWER_DG    = -33;
        final byte UPPER_DG    = 32;

        final byte LOWER_DRDB  = -9;
        final byte UPPER_DRDB  = 8;

        return (
                lumaDiff[DRi] > LOWER_DRDB && lumaDiff[DRi] < UPPER_DRDB &&
                lumaDiff[DGi] > LOWER_DG && lumaDiff[DGi] < UPPER_DG &&
                lumaDiff[DBi] > LOWER_DRDB && lumaDiff[DBi] < UPPER_DRDB
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
     */
    public static byte[] encodeData(byte[][] image) {


        // Initialization
        byte[]            prevPixel     = QOISpecification.START_PIXEL;
        byte[][]          hashTable     = new byte[64][4];
        int               runCounter    = 0;
        ArrayList<byte[]> encodedPixels = new ArrayList<>();

        // Pixel Processing
        for (int i = 0; i < image.length; i++) {

            // ---QOI_OP_RUN---
            if (ArrayUtils.equals(image[i], prevPixel)) {
                runCounter++;

                if (runCounter == 62 || i == (image.length - 1)) {
                    encodedPixels.add(qoiOpRun((byte) runCounter));
                    runCounter = 0;
                }
            }
            else {
                if (runCounter > 0) {
                    encodedPixels.add(qoiOpRun((byte) runCounter));
                    runCounter = 0;
                }

                // ---QOI_OP_INDEX---
                if (ArrayUtils.equals(image[i], hashTable[QOISpecification.hash(image[i])])) {
                    encodedPixels.add(qoiOpIndex(QOISpecification.hash(image[i])));
                }
                else {
                    hashTable[QOISpecification.hash(image[i])] = image[i];

                    if (image[i][QOISpecification.a] == prevPixel[QOISpecification.a]) {
                        byte[] pixelDiff = calcRGBdiff(image[i], prevPixel);

                        // ---QOI_OP_DIFF---
                        if(isValidRGBdiff(pixelDiff))
                            encodedPixels.add(qoiOpDiff(pixelDiff));

                        // ---QOI_OP_LUMA---
                        else if (isValidLumaDiff(calcLumaDiff(pixelDiff)))
                            encodedPixels.add(qoiOpLuma(pixelDiff));

                        // ---QOI_OP_RGB---
                        else
                            encodedPixels.add(qoiOpRGB(image[i]));

                        // ---QOI_OP_RGBA---
                    } else
                        encodedPixels.add(qoiOpRGBA(image[i]));

                }
            }
            prevPixel = image[i];
        }

        // flatten the 2D array to 1D
        return ArrayUtils.concat(encodedPixels.toArray(new byte[0][0]));
    }

    /**
     * Creates the representation in memory of the "Quite Ok Image" file.
     *
     * @param image (Helper.Image) - Image to encode
     * @return (byte[]) - Binary representation of the "Quite Ok File" of the image
     * @throws AssertionError if the image is null
     * @apiNote THE FILE IS NOT CREATED YET, THIS IS JUST ITS REPRESENTATION.
     * TO CREATE THE FILE, YOU'LL NEED TO CALL Helper::write
     */
    public static byte[] qoiFile(Helper.Image image) {
        assert image != null:"The image is null.";
        return ArrayUtils.concat(qoiHeader(image),encodeData(ArrayUtils.imageToChannels(image.data())),
                QOISpecification.QOI_EOF);
    }

}