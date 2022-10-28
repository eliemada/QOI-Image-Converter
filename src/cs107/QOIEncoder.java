package cs107;

import java.util.ArrayList;

/**
 * "Quite Ok Image" Encoder
 * @apiNote Second task of the 2022 Mini Project
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.0
 * @since 1.0
 */
public final class QOIEncoder {

    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private QOIEncoder(){}

    // ==================================================================================
    // ============================ QUITE OK IMAGE HEADER ===============================
    // ==================================================================================

    /**
     * Generate a "Quite Ok Image" header using the following parameters
     * @param image (Helper.Image) - Image to use
     * @throws AssertionError if the colorspace or the number of channels is corrupted or if the image is null.
     *  (See the "Quite Ok Image" Specification or the handouts of the project for more information)
     * @return (byte[]) - Corresponding "Quite Ok Image" Header
     */
    public static byte[] qoiHeader(Helper.Image image){
        assert image != null : "image is null";
        assert image.channels() == QOISpecification.RGB || image.channels() == QOISpecification.RGBA : "The image channels are corrupted";
        assert image.color_space() == QOISpecification.sRGB || image.color_space() == QOISpecification.ALL: "The image color space is corrupted";
        byte[] header = ArrayUtils.concat(
                QOISpecification.QOI_MAGIC,
                ArrayUtils.fromInt(image.data()[0].length),
                ArrayUtils.fromInt(image.data().length),
                ArrayUtils.concat(image.channels(), image.color_space()));
        assert header.length == 14;
        return header;
    }

    // ==================================================================================
    // ============================ ATOMIC ENCODING METHODS =============================
    // ==================================================================================

    /**
     * Encode the given pixel using the QOI_OP_RGB schema
     * @param pixel (byte[]) - The Pixel to encode
     * @throws AssertionError if the pixel's length is not 4
     * @return (byte[]) - Encoding of the pixel using the QOI_OP_RGB schema
     */
    public static byte[] qoiOpRGB(byte[] pixel){
        assert pixel.length == 4 : "The length of the input pixel is not 4";

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGB_TAG,
                pixel[QOISpecification.r], pixel[QOISpecification.g], pixel[QOISpecification.b]);
    }

    /**
     * Encode the given pixel using the QOI_OP_RGBA schema
     * @param pixel (byte[]) - The pixel to encode
     * @throws AssertionError if the pixel's length is not 4
     * @return (byte[]) Encoding of the pixel using the QOI_OP_RGBA schema
     */
    public static byte[] qoiOpRGBA(byte[] pixel){
        assert pixel.length == 4 : "The length of the input pixel is not 4";

        return ArrayUtils.concat(
                QOISpecification.QOI_OP_RGBA_TAG,
                pixel[QOISpecification.r], pixel[QOISpecification.g], pixel[QOISpecification.b], pixel[QOISpecification.a]);
    }

    /**
     * Encode the index using the QOI_OP_INDEX schema
     * @param index (byte) - Index of the pixel
     * @throws AssertionError if the index is outside the range of all possible indices
     * @return (byte[]) - Encoding of the index using the QOI_OP_INDEX schema
     */
    public static byte[] qoiOpIndex(byte index){
        assert (index < 64) && (index >= 0) : "The index is outside of the allowed range.";
        // add binary check to make sure output is in format 0b00XXXXXX ?
        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_INDEX_TAG + index));
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_DIFF schema
     * @param diff (byte[]) - The difference between 2 pixels
     * @throws AssertionError if diff doesn't respect the constraints or diff's length is not 3
     * (See the handout for the constraints)
     * @return (byte[]) - Encoding of the given difference
     */
    public static byte[] qoiOpDiff(byte[] diff){
        assert diff != null && diff.length == 3 : "The length of the input diff is not 3";

        byte binEncodedOutput = QOISpecification.QOI_OP_DIFF_TAG;

        for (int i = 0; i < 3; i++){
            assert diff[i] >= -2 && diff[i] <= 1 : "A difference value is invalid.";
            diff[i] += 2;

            binEncodedOutput += (byte) (diff[i] << (4-i*2));
        }

        return ArrayUtils.wrap(binEncodedOutput);
    }

    /**
     * Encode the difference between 2 pixels using the QOI_OP_LUMA schema
     * @param diff (byte[]) - The difference between 2 pixels
     * @throws AssertionError if diff doesn't respect the constraints
     * or diff's length is not 3
     * (See the handout for the constraints)
     * @return (byte[]) - Encoding of the given difference
     */

    // still expects primitive RGB diff, will modularize
    public static byte[] qoiOpLuma(byte[] diff){
        assert diff != null && diff.length == 3 : "The length of the input diff is not 3";

        final byte DRi = 0;
        final byte DGi = 1;
        final byte DBi = 2;

        final byte LOWER_DG = -33;
        final byte UPPER_DG = 32;
        final byte DG_OFFSET = 32;

        final byte LOWER_DRDB = -9;
        final byte UPPER_DRDB = 8;
        final byte DRDB_OFFSET = 8;

        byte[] binEncodedOutput = new byte[]{QOISpecification.QOI_OP_LUMA_TAG, 0};

        assert diff[DGi] > LOWER_DG && diff[DGi] < UPPER_DG : "The difference value of the green channel is out of range.";
        binEncodedOutput[0] += (byte) (diff[DGi] + DG_OFFSET);

        byte drdgDiff = (byte) (diff[DRi] - diff[DGi]);
        assert drdgDiff > LOWER_DRDB && drdgDiff < UPPER_DRDB : "The difference value of the red channel is out of range.";
        binEncodedOutput[1] = (byte) ((diff[DRi] - diff[DGi] + DRDB_OFFSET) << 4);

        byte dbdgDiff = (byte) (diff[DBi] - diff[DGi]);
        assert dbdgDiff > LOWER_DRDB && dbdgDiff < UPPER_DRDB : "The difference value of the blue channel is out of range.";
        binEncodedOutput[1] += diff[DBi] - diff[DGi] + DRDB_OFFSET;

        return binEncodedOutput;
    }

    /**
     * Encode the number of similar pixels using the QOI_OP_RUN schema
     * @param count (byte) - Number of similar pixels
     * @throws AssertionError if count is not between 0 (exclusive) and 63 (exclusive)
     * @return (byte[]) - Encoding of count
     */
    public static byte[] qoiOpRun(byte count){
        final byte COUNT_OFFSET = -1;
        assert count > 0 && count < 63 : "The count is outside of the allowed range.";
        return ArrayUtils.wrap((byte) (QOISpecification.QOI_OP_RUN_TAG + (count + COUNT_OFFSET)));
    }

    // ==================================================================================
    // =========================== Helper methods for encodeData  =======================
    // ==================================================================================

    // this method calculates the simple difference between the respective channels (only RGB) of 2 input pixels
    // wrap-around byte values are expected
    // note: could be improved to return boolean with surcharge problem from the lesson
    public static byte[] calcRGBdiff(byte[] pixel1, byte[] pixel2){
        byte[] pixelDiff = new byte[3];
        for (int i = 0; i < 3; i++)
            pixelDiff[i] = (byte) (pixel1[i] - pixel2[i]);
        return pixelDiff;
    }

    // this calculates the RGB differences in the QoiOpLuma format
    // wrap-around byte values as always
    // returns byte-array DR-DG, DG, DB-DG !!
    public static byte[] calcLumaDiff(byte[] pixel1, byte[] pixel2){
        byte[] pixelDiff = calcRGBdiff(pixel1, pixel2);
        return new byte[]{(byte) (pixelDiff[QOISpecification.r] - pixelDiff[QOISpecification.g]), pixelDiff[QOISpecification.g], (byte) (pixelDiff[QOISpecification.b] - pixelDiff[QOISpecification.g])};
    }

    // this is like a reverse surcharge of calcRGBdiff with the QoiOpDiff constraints (see above for ref. to lesson problem)
    public static boolean isSmallRGBdiff(byte[] pixel1, byte[] pixel2){
        byte[] pixelDiff = calcRGBdiff(pixel1, pixel2);
        for (byte diff : pixelDiff)
            if (diff < -2 || diff > 1)
                return false;
        return true;
    }

    // this is like a reverse surcharge of calcRGBdiff with the QoiOpLuma constraints
    // wrap-around byte values are expected
    public static boolean isLumaDiff(byte[] pixel1, byte[] pixel2){
        byte[] lumaDiff = calcLumaDiff(pixel1, pixel2);
        // c moche, je vais le faire plus clean:
        // reminder: add byte casts somewhere
        // will let QoiOpLuma share this method somehow
        return ((lumaDiff[QOISpecification.r] > -9) && (lumaDiff[QOISpecification.r] < 8) && lumaDiff[QOISpecification.g] > -33) && (lumaDiff[QOISpecification.g] < 32) && (lumaDiff[QOISpecification.b] > -9) && (lumaDiff[QOISpecification.b] < 8);
    }

    // ==================================================================================
    // ============================== GLOBAL ENCODING METHODS  ==========================
    // ==================================================================================

    /**
     * Encode the given image using the "Quite Ok Image" Protocol
     * (See handout for more information about the "Quite Ok Image" protocol)
     * @param image (byte[][]) - Formatted image to encode
     * @return (byte[]) - "Quite Ok Image" representation of the image
     */
    public static byte[] encodeData(byte[][] image){
        byte[] prevPixel = QOISpecification.START_PIXEL;
        byte[][] hashTable = new byte[64][4];
        int runCounter = 0;

        ArrayList<byte[]> encodedPixels = new ArrayList<>();
        for (byte[] pixel : image){
            if (ArrayUtils.equals(pixel, prevPixel)){
                runCounter++;
                // might benefit from normal for-loop with index
                if (runCounter == 62 || ArrayUtils.equals(pixel,image[image.length-1])){
                    encodedPixels.add(qoiOpRun((byte) runCounter));
                    runCounter = 0;
                }
                prevPixel = pixel;
                continue;

            } else if (runCounter > 0){
                encodedPixels.add(qoiOpRun((byte) runCounter));
                runCounter = 0;
            }

            if (ArrayUtils.equals(pixel, hashTable[QOISpecification.hash(pixel)])){
                encodedPixels.add(qoiOpIndex(QOISpecification.hash(pixel)));
                prevPixel = pixel;
                continue;
                } else hashTable[QOISpecification.hash(pixel)] = pixel;

            // improve performance by not doubly computing the diff in positive cases?
            if (pixel[QOISpecification.a] == prevPixel[QOISpecification.a] && isSmallRGBdiff(pixel, prevPixel)){
                encodedPixels.add(qoiOpDiff(calcRGBdiff(pixel, prevPixel)));
                prevPixel = pixel;
                continue;
            }
            if (pixel[QOISpecification.a] == prevPixel[QOISpecification.a] && isLumaDiff(pixel, prevPixel)){
                // old diff method, subject to change
                encodedPixels.add(qoiOpLuma(calcRGBdiff(pixel, prevPixel)));
                prevPixel = pixel;
                continue;
            }

            if (pixel[QOISpecification.a] == prevPixel[QOISpecification.a]){
                encodedPixels.add(qoiOpRGB(pixel));
                prevPixel = pixel;
                continue;
            }
            encodedPixels.add(qoiOpRGBA(pixel));
            prevPixel = pixel;
        }

        // manual array flattening, then concatenation. should be done cleaner
        byte[][] encodedImage = new byte[encodedPixels.size()][];
        for (int i = 0; i < encodedPixels.size(); i++)
            encodedImage[i] = encodedPixels.get(i);
        return ArrayUtils.concat(encodedImage);
    }

    /**
     * Creates the representation in memory of the "Quite Ok Image" file.
     * @apiNote THE FILE IS NOT CREATED YET, THIS IS JUST ITS REPRESENTATION.
     * TO CREATE THE FILE, YOU'LL NEED TO CALL Helper::write
     * @param image (Helper.Image) - Image to encode
     * @return (byte[]) - Binary representation of the "Quite Ok File" of the image
     * @throws AssertionError if the image is null
     */
    public static byte[] qoiFile(Helper.Image image){
        return Helper.fail("Not Implemented");
    }

}