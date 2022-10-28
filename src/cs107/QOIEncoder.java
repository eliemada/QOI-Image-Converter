package cs107;

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
        return ArrayUtils.concat(index);
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
    // ============================== GLOBAL ENCODING METHODS  ==========================
    // ==================================================================================

    /**
     * Encode the given image using the "Quite Ok Image" Protocol
     * (See handout for more information about the "Quite Ok Image" protocol)
     * @param image (byte[][]) - Formatted image to encode
     * @return (byte[]) - "Quite Ok Image" representation of the image
     */
    public static byte[] encodeData(byte[][] image){
        return Helper.fail("Not Implemented");
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