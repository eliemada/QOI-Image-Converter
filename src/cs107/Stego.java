package cs107;
import java.io.File;
import java.util.ArrayList;
import static cs107.QOIEncoder.*;
import static cs107.QOIDecoder.*;

/**
 * Stego: Steganography tool for embedding strings in images.
 * This piggybacks off of the QOI encoder/decoder and is implemented to work
 * in parallel to encoding and decoding QOI images.
 * @author  Sebastian Kugler (362022)
 * @version 1.0
 * @apiNote Extension of the 2022 Mini Project 1
 * @since 1.0
 */
public class Stego {

    /**
     * Input / Output management for the Stego class.
     * Per default, the string to be encoded is read from the specified file
     * while the decoded message is printed to the console.
     * @author Sebastian Kugler (362022)
     */
    public static void main(String[] args){

        // encode and embed:
//        pngToQoi("references/dice.png", "dice.qoi",
//                fileToString("res/messageToEncode.txt"));

        // decode and extract:
//        qoiToPng("res/dice.qoi", "dice.png");
    }

    // ==================================================================================
    // ============================== Encoding and embedding ============================
    // ==================================================================================

    /**
     * Convert a string to prepare it for the embedding in an image.
     * @author Sebastian Kugler (362022)
     * @param message (String) - message to be converted
     * @return (byte[]) - array of bytes with only each least bit containing the data.
     */
    static byte[] toAsciiLeastBit(String message) {
        assert (message != null) && (message.length() > 0) : "Message is null and/or empty";

        byte[] asciiLb = new byte[message.length() * 8];
        int pos = 0;
        byte mask = 0b1;

        for (int i = 0; i < message.length(); i++) {
            byte msgByte = (byte) message.charAt(i);
            int shift = 7;
            for (int j = 0; j < 8; j++) {
                asciiLb[pos] = (byte) ((msgByte >> shift) & mask);
                shift -= 1;
                pos++;
            }
        }

        return asciiLb;
    }

    /**
     * Encode the given pixel using the QOI_OP_RGB block.
     * Overwrite the least significant bit of RGB channels with provided data.
     * @author Sebastian Kugler (362022)
     * @param pixel (byte[]) - The Pixel to encode
     * @param asciiLb (byte[]) - message as array of bytes
     *                provided by {@link #toAsciiLeastBit(String)}
     * @param position (int) - position in the message to start embedding
     * @return (byte[]) - QOI_OP_RGB block with additional data embedded
     */
    static byte[] qoiOpRGB(byte[] pixel, byte[] asciiLb, int position) {
        byte[] alteredPixel = new byte[4];
        for (int i = 0; (i < 3) && (position < asciiLb.length); i++) {
            alteredPixel[i] = (byte) ((pixel[i] & 0b11_11_11_10) | asciiLb[position]);
            position++;
        }
        // always increments position by 3
        return QOIEncoder.qoiOpRGB(alteredPixel);
    }

    /**
     * Encode the given pixel using the QOI_OP_RGBA block.
     * Overwrite the least significant bit of RGBA channels with provided data.
     * @author Sebastian Kugler (362022)
     * @param pixel (byte[]) - The Pixel to encode
     * @param asciiLb (byte[]) - message as array of bytes
     *                provided by {@link #toAsciiLeastBit(String)}
     * @param position (int) - position in the message to start embedding
     * @return (byte[]) - QOI_OP_RGBA block with additional data embedded
     */
    static byte[] qoiOpRGBA(byte[] pixel, byte[] asciiLb, int position) {
        byte[] alteredPixel = new byte[4];
        for (int i = 0; (i < 4) && (position < asciiLb.length); i++) {
            alteredPixel[i] = (byte) ((pixel[i] & 0b11_11_11_10) | asciiLb[position]);
            position++;
        }
        // always increments position by 4
        return QOIEncoder.qoiOpRGBA(alteredPixel);
    }

    /**
     * Embed string data in image data while keeping a low visual impact and
     * encoding the given image using the "Quite Ok Image" Protocol.
     * @author Sebastian Kugler (362022)
     * @param image (byte[][]) - Formatted image to encode
     * @param asciiLb (byte[]) - String to embed, in asciiLb format as provided
     *                by {@link #toAsciiLeastBit(String)}
     * @return (byte[]) - "Quite Ok Image" representation of the image
     */
    public static byte[] encodeData(byte[][] image, byte[] asciiLb) {

        // Initialization
        byte[]            prevPixel     = QOISpecification.START_PIXEL;
        byte[][]          hashTable     = new byte[64][4];
        int               runCounter    = 0;
        ArrayList<byte[]> encodedPixels = new ArrayList<>();

        // Statistics:
        // Indexes: 0-QOI_OP_RUN,   1-QIO_OP_INDEX, 2-QOI_OP_DIFF,
        //          3-QOI_OP_LUMA,  4-QOI_OP_RGB,   5-QOI_OP_RGBA)
        int [] stats = new int[6];

        // stego logic
        int messagePos = 0;

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
                // lazy outcomment
                if (ArrayUtils.equals(image[i], hashTable[QOISpecification.hash(image[i])])) {
                    encodedPixels.add(qoiOpIndex(QOISpecification.hash(image[i])));
                    stats[1]++;

                }
                else {
                    if (image[i][QOISpecification.a] == prevPixel[QOISpecification.a]) {
                        byte[] pixelDiff = calcRGBdiff(image[i], prevPixel);

                        // ---QOI_OP_DIFF---
                        if(isValidRGBdiff(pixelDiff)) {
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
                            if (messagePos < asciiLb.length) {
                                byte[] alteredRgbTag = qoiOpRGB (image[i], asciiLb, messagePos);
                                System.arraycopy(alteredRgbTag, 1, image[i], 0, 3);
                                encodedPixels.add(alteredRgbTag);
                                messagePos += 3;
                            } else
                                encodedPixels.add(QOIEncoder.qoiOpRGB(image[i]));
                            stats[4]++;
                        }

                        // ---QOI_OP_RGBA---
                    } else {
                        if (messagePos < asciiLb.length) {
                            byte[] alteredRgbaTag = qoiOpRGBA(image[i], asciiLb, messagePos);
                            System.arraycopy(alteredRgbaTag, 1, image[i], 0, 4);
                            encodedPixels.add(alteredRgbaTag);
                            messagePos += 4;
                        } else
                            encodedPixels.add(QOIEncoder.qoiOpRGBA(image[i]));
                        stats[5]++;
                    }
                    hashTable[QOISpecification.hash(image[i])] = image[i];
                }
            }
            prevPixel = image[i];
        }

        // Statistics, percent rounded down to next int
        int pixelsEncoded = stats[0] + stats[1] + stats[2] + stats[3] + stats[4] + stats[5];
        System.out.println("====== Encoding Statistics ======");
        System.out.println("    Method    |   Pixels encoded");
        System.out.println("                  number  | %");
        System.out.println("QOI_OP_RUN    |   " + stats[0] + "   \t" + stats[0]*100/pixelsEncoded);
        System.out.println("QOI_OP_INDEX  |   " + stats[1] + "   \t" + stats[1]*100/pixelsEncoded);
        System.out.println("QOI_OP_DIFF   |   " + stats[2] + "   \t" + stats[2]*100/pixelsEncoded);
        System.out.println("QOI_OP_LUMA   |   " + stats[3] + "   \t" + stats[3]*100/pixelsEncoded);
        System.out.println("QOI_OP_RGB    |   " + stats[4] + "   \t" + stats[4]*100/pixelsEncoded);
        System.out.println("QOI_OP_RGBA   |   " + stats[5] + "   \t" + stats[5]*100/pixelsEncoded);
        System.out.println("=================================");

        // stego statistics
        if (messagePos >= asciiLb.length)
            System.out.println("Stego: Message successfully e̶n̶c̶o̶d̶e̶d̶! " +
                    asciiLb.length + " bits were encoded and " +
                    ((stats[4]*3 + stats[5]*4) - asciiLb.length) + " free bits left.");
        else
            System.out.println("Message does not fit in the encoded image. There are " +
                    (asciiLb.length - messagePos) + " bits left to hide.");


        // flatten the 2D array to 1D
        return ArrayUtils.concat(encodedPixels.toArray(new byte[0][0]));
    }

    // ==================================================================================
    // ============================== Decoding and extracting ===========================
    // ==================================================================================

    /**
     * Extract last-bit data from a given pixel's RGB(A) channels
     * @author Sebastian Kugler (362022)
     * @param pixel (byte[]) - The pixel in RGB(A) format to extract data from
     * @param asciiChunk (byte[]) - The two byte chunk to store extracted bits
     * @param asciiChunkPos (int) - The position in the asciiChunk to write to
     */
    static void PixelToAsciiLb(byte[] pixel, byte[] asciiChunk, int asciiChunkPos) {

        for (int i = 0; (i < pixel.length) && (asciiChunkPos < 16); i++) {
            if (asciiChunkPos < 8)
                asciiChunk[0] = (byte) ((asciiChunk[0] | (pixel[i] & 0b1) << (7 - asciiChunkPos)));
            else
                asciiChunk[1] = (byte) ((asciiChunk[1] | (pixel[i] & 0b1) << (15 - asciiChunkPos)));
            asciiChunkPos++;
        }
    }

    /**
     * Store the decoded pixel in the buffer and extracted stego data in the {@code asciiChunk}.
     * @author Sebastian Kugler (362022)
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param input (byte[]) - Stream of bytes to read from
     * @param alpha (byte) - Alpha component of the pixel
     * @param position (int) - Index in the buffer
     * @param asciiChunk (byte[]) - Two bytes to store the extracted stego data
     * @param asciiChunkPos (int) - Position in the asciiChunk
     * @param idx (int) - Index in the input
     */
    static void decodeQoiOpRGB(byte[][] buffer, byte[] input, byte alpha, int position, int idx,
                               byte[] asciiChunk, int asciiChunkPos) {
        QOIDecoder.decodeQoiOpRGB(buffer, input, alpha, position, idx);
        byte[] rgb = ArrayUtils.extract(input,idx,3);
        PixelToAsciiLb(rgb, asciiChunk, asciiChunkPos);
    }

    /**
     * Store the decoded pixel in the buffer and extracted stego data in the {@code asciiChunk}.
     * @author Sebastian Kugler (362022)
     * @param buffer (byte[][]) - Buffer where to store the pixel
     * @param input (byte[]) - Stream of bytes to read from
     * @param position (int) - Index in the buffer
     * @param asciiChunk (byte[]) - Two bytes to store the extracted stego data
     * @param asciiChunkPos (int) - Position in the asciiChunk
     * @param idx (int) - Index in the input
     */
    static void decodeQoiOpRGBA(byte[][] buffer, byte[] input, int position, int idx,
                                byte[] asciiChunk, int asciiChunkPos) {

        QOIDecoder.decodeQoiOpRGBA(buffer, input, position, idx);
        byte[] rgba = ArrayUtils.extract(input,idx,4);

        PixelToAsciiLb(rgba, asciiChunk, asciiChunkPos);
    }

    /**
     * Decode the given data using the "Quite Ok Image" Protocol.
     * Extract additional data using the stego scheme and print it to the console.
     * @author Sebastian Kugler (362022)
     * @param data (byte[]) - Data to decode
     * @param width (int) - The width of the expected output
     * @param height (int) - The height of the expected output
     * @return (byte[][]) - Decoded "Quite Ok Image"
     * @throws AssertionError See handouts section 6.3
     */
    public static byte[][] decodeData(byte[] data, int width, int height) {
        // Initialization
        byte[]      prevPixel   =   QOISpecification.START_PIXEL;
        byte[][]    hashTable   =   new byte[64][4];
        byte[][]    buffer      =   new byte[width * height][4];
        int  idx       = 0;
        int  position  = 0;

        // stego logic
        boolean isStego = true;
        byte[] asciiChunk = new byte[2];
        int asciipos = 0;
        StringBuilder messageSoFar = new StringBuilder();
        String message;


        assert data != null : "The data is null";
        assert (width > -1) : "The width is negative";
        assert (height > -1) : "The height is negative";

        // Data decoding
        while (position < buffer.length) {

            byte chunk = data[idx];
            // Whole-byte tags

            // ---QOI_OP_RGB---
            if (chunk == QOISpecification.QOI_OP_RGB_TAG) {
                if (isStego) {
                    decodeQoiOpRGB(buffer, data, prevPixel[3], position, idx + 1, asciiChunk, asciipos);
                    asciipos += 3;
                } else
                    QOIDecoder.decodeQoiOpRGB(buffer, data, prevPixel[3], position, idx + 1);
                idx += 4;
            }

            // ---QOI_OP_RGBA---
            else if (chunk == QOISpecification.QOI_OP_RGBA_TAG) {
                if (isStego) {
                    decodeQoiOpRGBA(buffer, data, position, idx + 1, asciiChunk, asciipos);
                    asciipos += 4;
                }
                else
                    QOIDecoder.decodeQoiOpRGBA(buffer, data, position, idx + 1);
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

            // stego logic
            if (isStego) {
                if (asciipos > 7) {
                    messageSoFar.append((char) (asciiChunk[0]));
                    asciiChunk[0] = asciiChunk[1];
                    asciipos -= 8;
                    asciiChunk[1] = 0;
                }

                if ((messageSoFar.length() > 4) && !messageSoFar.substring(0, 5).equals("stgo%")) {
                    isStego = false;
                }
                if (messageSoFar.toString().endsWith("%stgo")) {
                    message = messageSoFar.substring(5, messageSoFar.toString().length() - 5);
                    System.out.println(message);
                    isStego = false;
                }
            }
        }


        return buffer;
    }

    // ==================================================================================
    // =============================== Boring Helper methods ============================
    // ==================================================================================
    // these are needed to keep this class strictly independent as an extension

    /**
     * Encodes a given file from "QOI" to "PNG"
     * @param inputFile (String) - The path of the file to decode
     * @param outputFile (String) - The path where to store the generated "PNG" Image
     */
    static void qoiToPng(String inputFile, String outputFile) {
        // Read in binary mode the file 'input_file'
        var inputFileContent = Helper.read(inputFile);
        // Decode the file using the 'QOI' decoder
        var computedImage = decodeQoiFile(inputFileContent);
        // Write an image to 'output_file'
        Helper.writeImage(outputFile, computedImage);
    }

    /**
     * Encodes a given file from "PNG" to "QOI" with hidden data using steganography
     * @param inputFile (String) - The path of the file to encode
     * @param outputFile (String) - The path where to store the generated "Quite Ok Image"
     * @param message (String) - The message to hide in the image
     */
    static void pngToQoi(String inputFile, String outputFile, String message){
        // Read a PNG file
        var inputImage = Helper.readImage(inputFile);
        // Encode the Image to QOI
        var outputFileContent = stegoFile(inputImage, message);
        // Write in binary mode the file content to 'output_file'
        Helper.write(outputFile, outputFileContent);
    }

    /**
     * Convert a file to a string.
     * @author Sebastian Kugler (362022)
     * @param path (String) - path of the file to convert
     * @return (String) - String representation of the file
     */
    static String fileToString(String path) {
        StringBuilder message = new StringBuilder();
        try {
            java.util.Scanner sc = new java.util.Scanner(new File(path));
            while (sc.hasNext ()) {
                message.append (sc.nextLine ());
                message.append (System.lineSeparator());
            }
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Message file " + path + " not found.");
        }

        return message.toString();
    }

    /**
     * Creates the representation in memory of the "Quite Ok Image" file
     * while hiding a message in the encoded data.
     * @author Sebastian Kugler (362022)
     * @param image (Helper.Image) - Image to encode
     * @param message (String) - Message to hide
     * @return (byte[]) - Binary representation of the "Quite Ok File" of the image
     * @throws AssertionError if the image is null
     * @apiNote THE FILE IS NOT CREATED YET, THIS IS JUST ITS REPRESENTATION.
     * TO CREATE THE FILE, YOU'LL NEED TO CALL Helper::write
     */
    public static byte[] stegoFile(Helper.Image image,String message) {
        assert image != null:"The image is null.";
        return ArrayUtils.concat(qoiHeader(image),
                encodeData(ArrayUtils.imageToChannels(image.data()),
                        toAsciiLeastBit("stgo%" + message + "%stgo")),
                QOISpecification.QOI_EOF);
    }

    /**
     * Decode a file using the "Quite Ok Image" Protocol
     * @param content (byte[]) - Content of the file to decode
     * @return (Image) - Decoded image
     * @throws AssertionError if content is null
     */
    public static Helper.Image decodeQoiFile(byte[] content){

        assert content != null : "The content is null";
        assert ArrayUtils.equals(ArrayUtils.extract(
                content, content.length-8, 8),QOISpecification.QOI_EOF) :
                "The magic number is not valid";

        int[] header = decodeHeader(ArrayUtils.extract(content, 0, QOISpecification.HEADER_SIZE));

        byte[] blockStream = ArrayUtils.extract(content, QOISpecification.HEADER_SIZE,
                content.length - QOISpecification.HEADER_SIZE - QOISpecification.QOI_EOF.length);

        int width = header[0];
        int height = header[1];
        byte channels =(byte) header[2];
        byte colorSpace = (byte) header[3];


        return Helper.generateImage(
                ArrayUtils.channelsToImage(
                        decodeData(blockStream, width, height), height, width), channels, colorSpace);
    }


}
