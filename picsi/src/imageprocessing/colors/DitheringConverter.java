package imageprocessing.colors;

import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import main.Picsi;

public class DitheringConverter implements IImageProcessor {

    private static final int THRESHOLD = 128;
    private static final int[] DITHER_MATRIX = new int []{ 8, 4, 2, 4, 8, 4, 2, 1, 2, 4, 2, 1 };
    private static final int DITHER_DIMENSION = 5;

    @Override
    public boolean isEnabled(int imageType) {
        return imageType != Picsi.IMAGE_TYPE_GRAY && imageType != Picsi.IMAGE_TYPE_INDEXED;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        return dither(inData, imageType);
    }

    public static ImageData dither(ImageData inData, int imageType) {
        var outputImage = GrayScaleConverter.convert(inData, imageType);

        for (int y = 0; y < outputImage.height; y++) {
            for (int x = 0; x < outputImage.width; x++) {
                int oldGray = 0xFF & outputImage.data[y * outputImage.bytesPerLine + x];
                
                int newGray = oldGray < THRESHOLD ? 0 : 0xFF;
                outputImage.data[y * outputImage.bytesPerLine + x] = (byte)newGray;

                int error = oldGray - newGray;
                distributeError(outputImage, y, x, error);
            }
        }
        return outputImage;
    }

    private static void distributeError(ImageData outputImage, int y, int x, int error) {
        for (int i = 0; i < DITHER_MATRIX.length; i++) {
            int startOffset = i + DITHER_DIMENSION / 2 + 1;
            int neighborX = x + (startOffset % DITHER_DIMENSION) - 2;
            int neighborY = y + (startOffset / DITHER_DIMENSION);

            if (neighborX >= 0 && neighborX < outputImage.width && neighborY >= 0 && neighborY < outputImage.height) {
                int currentGray = 0xFF & outputImage.data[neighborY * outputImage.bytesPerLine + neighborX];
                var grayWithError = currentGray + error * (double)(DITHER_MATRIX[i]) / 42;

                int gray = ImageProcessing.clamp8(grayWithError);
                outputImage.data[neighborY * outputImage.bytesPerLine + neighborX] = (byte)gray;
            }
        }
    }

}
