package cs107;

import static cs107.Helper.Image;

/**
 * "Quite Ok Image" Decoder
 * @apiNote Third task of the 2022 Mini Project
 * @author Hamza REMMAL (hamza.remmal@epfl.ch)
 * @version 1.0
 * @since 1.0
 */
public final class QOIDecoder {

    /**
     * DO NOT CHANGE THIS, MORE ON THAT IN WEEK 7.
     */
    private QOIDecoder(){}

    // ==================================================================================
    // =========================== QUITE OK IMAGE HEADER ================================
    // ==================================================================================

    /**
     * Extract useful information from the "Quite Ok Image" header
     * @param header (byte[]) - A "Quite Ok Image" header
     * @return (int[]) - Array such as its content is {width, height, channels, color space}
     * @throws AssertionError See handouts section 6.1
     */
    public static int[] decodeHeader(byte[] header){
        assert header != null : "Header is null";
        assert (header.length == QOISpecification.HEADER_SIZE) :"The length of the header is not equal to "
                                                                 + "the constant HEADER_SIZE" ;
        assert !(ArrayUtils.equals(ArrayUtils.extract(header, 4, 4), QOISpecification.QOI_MAGIC))
                :"The first four bytes are not equal to the constant HEADER_SIZE" ;
        assert (header[header.length-2]==QOISpecification.RGB||
                header[header.length-2]==QOISpecification.RGBA) : "The number of channels is not equal "
                                                                  + "to RGB or RGBA";
        assert (header[header.length-1]==QOISpecification.ALL||
                header[header.length-1]==QOISpecification.sRGB) : "the color space is not equal "
                                                                  + "to ALL or sRGB";



        int[] output = new int[4];
        output[0] = ArrayUtils.toInt(ArrayUtils.extract(header,4,4));
        output[1] = ArrayUtils.toInt(ArrayUtils.extract(header,8,4));
        output[2] = header[header.length-2];
        output[3] = header[header.length-1];
        return output;

    }
    // ==================================================================================
    // =========================== ATOMIC DECODING METHODS ==============================
    // ==================================================================================

    /**
     * Store the pixel in the buffer and return the number of consumed bytes
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param input (byte[]) - Stream of bytes to read from
     * @param alpha (byte) - Alpha component of the pixel
     * @param position (int) - Index in the buffer
     * @param idx (int) - Index in the input
     * @return (int) - The number of consumed bytes
     * @throws AssertionError See handouts section 6.2.1
     */
    public static int decodeQoiOpRGB(byte[][] buffer, byte[] input, byte alpha, int position, int idx){
        assert !(input == null && buffer ==null) : "The input and the buffer are null";
        assert !(buffer == null) : "The buffer is null";
        assert !(input == null) : "The input is null";
        assert (position >= 0 && position < buffer.length) :
                "The variable position doesn't point towards a valid location of buffer";
        assert (idx+2 < input.length): "input does not contain enough data to recover the pixel";
        byte[]   extracted = ArrayUtils.extract(input,idx,3);
        byte[]   editedBuffer = new byte[4];
        System.arraycopy(extracted, 0, editedBuffer, 0, extracted.length);
        editedBuffer[3]=alpha;
        buffer[position] = editedBuffer;
        return 3;
    }

    /**
     * Store the pixel in the buffer and return the number of consumed bytes
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param input (byte[]) - Stream of bytes to read from
     * @param position (int) - Index in the buffer
     * @param idx (int) - Index in the input
     * @return (int) - The number of consumed bytes
     * @throws AssertionError See handouts section 6.2.2
     */
    public static int decodeQoiOpRGBA(byte[][] buffer, byte[] input, int position, int idx){
        assert !(input == null && buffer ==null) : "The input and the buffer are null";
        assert !(buffer == null) : "The buffer is null";
        assert !(input == null) : "The input is null";
        assert (position>0 || position < buffer.length) : "The variable position doesn't point towards a "
                                                             + "valid location of buffer";
        assert (idx+3 < input.length): "input does not contain enough data to recover the pixel";

        byte[] output = ArrayUtils.extract(input,idx,4);
        buffer[position] = output;
        return 4;
    }

    /**
     * Create a new pixel following the "QOI_OP_DIFF" schema.
     * @param previousPixel (byte[]) - The previous pixel
     * @param chunk (byte) - A "QOI_OP_DIFF" data chunk
     * @return (byte[]) - The newly created pixel
     * @throws AssertionError See handouts section 6.2.4
     */
    public static byte[] decodeQoiOpDiff(byte[] previousPixel, byte chunk){
        assert !(previousPixel == null): "The previous pixel is equal to null";
        assert (previousPixel.length == 4): "The previous length is not equal to 4";
        assert ((chunk & 0b11_00_00_00) == QOISpecification.QOI_OP_DIFF_TAG) : "The tag of chunk is not "
                                                                               + "equal to QOI_OP_DIFF_TAG";
        int dr, dg, db;
        dr = ((chunk & 0b11_00_00) >>> 4) -2;
        dg = ((chunk & 0b11_00 )>>> 2) -2;
        db = (chunk & 0b11) -2;

        return new byte[]{(byte) (previousPixel[0] + dr),(byte) (previousPixel[1] + dg),
                (byte) (previousPixel[2] + db),(byte) (previousPixel[3])};
    }
    /**
     * Create a new pixel following the "QOI_OP_LUMA" schema
     * @param previousPixel (byte[]) - The previous pixel
     * @param data (byte[]) - A "QOI_OP_LUMA" data chunk
     * @return (byte[]) - The newly created pixel
     * @throws AssertionError See handouts section 6.2.5
     */
    public static byte[] decodeQoiOpLuma(byte[] previousPixel, byte[] data){
        assert !(previousPixel == null): "The previous pixel is equal to null";
        assert (previousPixel.length == 4): "The previous length is not equal to 4";
        assert (((byte) (data[0] & 0b11_00_00_00)) == QOISpecification.QOI_OP_LUMA_TAG) :
                "The tag of chunk is not equal to QOI_OP_LUMA_TAG";

        int dr, dg, db;
        dg = ((data[0] &0b11_11_11)-32);
        dr =  (((data[1]&0b11_11_11_11)>>>4)-8)+dg;
        db =  ((data[1]&0b11_11))-8+dg;
        return new byte[]{(byte) (previousPixel[0] + dr),(byte) (previousPixel[1] + dg),
                (byte) (previousPixel[2] + db),(byte) (previousPixel[3])};

    }

    /**
     * Store the given pixel in the buffer multiple times
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param pixel (byte[]) - The pixel to store
     * @param chunk (byte) - a QOI_OP_RUN data chunk
     * @param position (int) - Index in buffer to start writing from
     * @return (int) - number of written pixels in buffer
     * @throws AssertionError See handouts section 6.2.6
     */
    public static int decodeQoiOpRun(byte[][] buffer, byte[] pixel, byte chunk, int position){
        assert !(buffer==null&&pixel == null) : "The buffer and the pixel are null";
        assert !(buffer==null) : "The buffer is null";
        assert !(pixel==null) : "The pixel is null";
        assert (position >= 0 && position < buffer.length) :
                "The variable position doesn't point towards a valid location of buffer";
        assert (pixel.length==4): "The pixel length is not equal to 4";

        int count = (byte) ((chunk & 0b11_11_11) + 1);
        assert (count + position <= buffer.length):
                "The buffer does not contain enough space to recover the pixels";
        for (int i = 0; i < count; i++){
            buffer[position] = pixel;
            position++;
        }
        return count -1;
    }

    // ==================================================================================
    // ========================= GLOBAL DECODING METHODS ================================
    // ==================================================================================

    /**
     * Decode the given data using the "Quite Ok Image" Protocol
     * @param data (byte[]) - Data to decode
     * @param width (int) - The width of the expected output
     * @param height (int) - The height of the expected output
     * @return (byte[][]) - Decoded "Quite Ok Image"
     * @throws AssertionError See handouts section 6.3
     */
    public static byte[][] decodeData(byte[] data, int width, int height) {
        // Initialization
        byte[] prevPixel = QOISpecification.START_PIXEL;
        byte[][] hashTable = new byte[64][4];
        byte[][] buffer = new byte[width * height][4];
        int idx = 0;
        int position = 0;

        // which asserts are needed here?
        assert data != null : "The data is null";
        assert (width > 0) : "The width is not positive";
        assert (height > 0) : "The height is not positive";

        // Data decoding
        while (position < buffer.length) {

            byte chunk = data[idx];
            // Whole-byte tags

            // ---QOI_OP_RGB---
            if (chunk == QOISpecification.QOI_OP_RGB_TAG) {
                decodeQoiOpRGB(buffer, data, prevPixel[3], position, idx+1);
                idx += 4;
            }

            // ---QOI_OP_RGBA---
            else if (chunk == QOISpecification.QOI_OP_RGBA_TAG) {
                decodeQoiOpRGBA(buffer, data, position, idx+1);

                idx += 5;
            } else {
                // Two-bit tags
                byte twoBitTag = (byte) (chunk & 0b11_00_00_00);

                switch (twoBitTag){
                    case QOISpecification.QOI_OP_INDEX_TAG:
                        buffer[position] = hashTable[(byte) (chunk & 0b00_11_11_11)];
                        idx++;
                        break;
                    case QOISpecification.QOI_OP_DIFF_TAG:
                        buffer[position] = decodeQoiOpDiff(prevPixel, chunk);
                        idx++;
                        break;
                    case QOISpecification.QOI_OP_LUMA_TAG:
                        buffer[position] = decodeQoiOpLuma(prevPixel, ArrayUtils.extract(data, idx, 2));
                        idx += 2;
                        break;
                    case  QOISpecification.QOI_OP_RUN_TAG:
                        position += decodeQoiOpRun(buffer, prevPixel, chunk, position);
                        idx++;
                        break;
                    default:
                        assert false : "The universe is invalid";

                }
            }

            prevPixel = buffer[position];
            hashTable[QOISpecification.hash(buffer[position])] = buffer[position];
            position++;
        }


        return buffer;
    }

    /**
     * Decode a file using the "Quite Ok Image" Protocol
     * @param content (byte[]) - Content of the file to decode
     * @return (Image) - Decoded image
     * @throws AssertionError if content is null
     */
    public static Image decodeQoiFile(byte[] content){

        assert content != null : "The content is null";

        // int[] test = decodeHeader(ArrayUtils.extract(content, 0, QOISpecification.HEADER_SIZE));
        // byte[] comp = new byte[]{0, 0, 0, 0, 0, 0, 0, 1};

        assert ArrayUtils.equals(ArrayUtils.extract(content, content.length-8, 8),QOISpecification.QOI_EOF) : "The magic number is not valid";

        int[] header = decodeHeader(ArrayUtils.extract(content, 0, QOISpecification.HEADER_SIZE));

        byte[] blockStream = ArrayUtils.extract(content, QOISpecification.HEADER_SIZE, content.length-QOISpecification.HEADER_SIZE-QOISpecification.QOI_EOF.length);
        // unsigned?
        int width = header[0];
        int height = header[1];
        byte channels =(byte) header[2];
        byte colorSpace = (byte) header[3];

        return Helper.generateImage(ArrayUtils.channelsToImage(decodeData(blockStream, width, height), height, width), channels, colorSpace);
    }

}