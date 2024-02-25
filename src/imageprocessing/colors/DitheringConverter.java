package imageprocessing.colors;

import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;

public class DitheringConverter implements IImageProcessor {

    private static final int THRESHOLD = 127;

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        return dither(inData, imageType);
    }


    public static ImageData dither(ImageData inData, int imageType) {
        var outputImage = GrayScaleConverter.convert(inData, imageType);

        var ditherMatrix = new int[] { 8, 4, 2, 4, 8, 4, 2, 1, 2, 4, 2, 1 };

        for (int y = 0; y < inData.height; y++) {
            for (int x = 0; x < inData.width; x++) {
                int oldGray = 0xFF & outputImage.data[y * outputImage.bytesPerLine + x];
                
                int newGray = oldGray < 128 ? 0 : 255;
                outputImage.data[y * outputImage.bytesPerLine + x] = (byte)newGray;

                int error = oldGray - newGray;

                for (int offset = 0; offset < ditherMatrix.length; offset++) {
                    int offsetTest = offset + 3;
                    int neighborX = x + (offsetTest % 5) - 2;
                    int neighborY = y + (offsetTest / 5);

                    if (neighborX >= 0 && neighborX < inData.width && neighborY >= 0 && neighborY < inData.height) {
                        int gray = 0xFF & inData.data[neighborY * inData.bytesPerLine + neighborX];
                        var newGray1 = gray + error * (double)(ditherMatrix[offset]) / 42;
    
                        if (newGray1 < 0) {
                            newGray1 = 0;
                        } else if (newGray1 > 255) {
                            newGray1 = 255;
                        }
    
                        int gray1 = ImageProcessing.clamp8(newGray1);
                        inData.data[neighborY * inData.bytesPerLine + neighborX] = (byte)gray1;
                    }
                }
            }
        }

        return outputImage;
    }

    private static void distributeError(ImageData inData, int x, int y, int width, int height, int error, int[][] matrix) {
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            for (int xOffset = -2; xOffset <= 2; xOffset++) {
                int neighborX = x + xOffset;
                int neighborY = y + yOffset;

                if (neighborX >= 0 && neighborX < width && neighborY >= 0 && neighborY < height) {
                    int gray = 0xFF & inData.data[neighborY * inData.bytesPerLine + neighborX];
                    var newGray = gray + error * (double)(matrix[yOffset][xOffset + 2]) / 42;

                    if (newGray < 0) {
                        newGray = 0;
                    } else if (newGray > 255) {
                        newGray = 255;
                    }

                    int gray1 = ImageProcessing.clamp8(newGray);
                    inData.data[neighborY * inData.bytesPerLine + neighborX] = (byte)gray1;
                    // inData.setPixel(neighborX, neighborY, gray1);
                }
            }
        }
    }

    private static void distributeError2(ImageData inData, int x, int y, int width, int height, int error, int[][] matrix) {
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            for (int xOffset = -2; xOffset <= 2; xOffset++) {
                int neighborX = x + xOffset;
                int neighborY = y + yOffset;

                if (neighborX >= 0 && neighborX < width && neighborY >= 0 && neighborY < height) {
                    var gray = inData.getPixel(neighborX, neighborY);
                    var newGray = gray + error * (double)(matrix[yOffset][xOffset + 2]) / 42;

                    if (newGray < 0) {
                        newGray = 0;
                    } else if (newGray > 255) {
                        newGray = 255;
                    }

                    int gray1 = ImageProcessing.clamp8(newGray);
                    inData.setPixel(neighborX, neighborY, gray1);
                }
            }
        }
    }


    public static ImageData dither1(ImageData inData, int imageType) {
        var matrix = new double[] {
                8.0 / 42, 4.0 / 42, 2.0 / 42,
                4.0 / 42, 8.0 / 42, 4.0 / 42,
                2.0 / 42, 4.0 / 42, 2.0 / 42
        };
        var matrixDimension = 3;
        var matrixStartIndex = matrixDimension / 2;
    
        var grayScaleImage = GrayScaleConverter.convert(inData, imageType);
    
        for (int y = 0; y < grayScaleImage.height; y++) {
            for (int x = 0; x < grayScaleImage.width; x++) {
    
                var pixelValue = grayScaleImage.getPixel(x, y);
                if (pixelValue < 0) {
                    pixelValue = 0;
                } else if (pixelValue > 255) {
                    pixelValue = 255;
                }
    
                var newPixel = pixelValue < 128 ? 0 : 255;
                grayScaleImage.setPixel(x, y, newPixel);
                var error = pixelValue - newPixel;
    
                for (int row = -matrixStartIndex; row <= matrixStartIndex; row++) {
                    for (int col = -matrixStartIndex; col <= matrixStartIndex; col++) {
                        int ix = x + col;
                        int iy = y + row;
    
                        if (ix < 0 || ix >= grayScaleImage.width || iy < 0 || iy >= grayScaleImage.height) {
                            continue;
                        }
    
                        int matrixIndex = (row + matrixStartIndex) * matrixDimension + (col + matrixStartIndex);
                        double val = matrix[matrixIndex];
                        int gray = ImageProcessing.clamp8((int) (error * val) + grayScaleImage.getPixel(ix, iy));
                        grayScaleImage.setPixel(ix, iy, gray);
                    }
                }
            }
        }
    
        return grayScaleImage;
    }


    public static ImageData dither2(ImageData inData, int imageType) {
        var matrix = new double[] {
            8 / 42, 4 / 42,
            2 / 42, 4 / 42, 8 / 42, 4 / 42, 2 / 42,
            1 / 42, 2 / 42, 4 / 42, 2 / 42, 1 / 42
        };
        var matrixDimension = 3;
        var matrixStartIndex = matrixDimension / 2;
    
        var grayScaleImage = GrayScaleConverter.convert(inData, imageType);
    
        for (int y = 0; y < grayScaleImage.height; y++) {
            for (int x = 0; x < grayScaleImage.width; x++) {
    
                var pixelValue = grayScaleImage.getPixel(x, y);
                if (pixelValue < 0) {
                    pixelValue = 0;
                } else if (pixelValue > 255) {
                    pixelValue = 255;
                }
    
                var newPixel = pixelValue < 128 ? 0 : 255;
                grayScaleImage.setPixel(x, y, newPixel);
                var error = pixelValue - newPixel;
    
                for (int row = -matrixStartIndex; row <= matrixStartIndex; row++) {
                    for (int col = -matrixStartIndex; col <= matrixStartIndex; col++) {
                        int ix = x + col;
                        int iy = y + row;
    
                        if (ix < 0 || ix >= grayScaleImage.width || iy < 0 || iy >= grayScaleImage.height) {
                            continue;
                        }
    
                        int matrixIndex = (row + matrixStartIndex) * matrixDimension + (col + matrixStartIndex);
                        double val = matrix[matrixIndex];
                        int gray = ImageProcessing.clamp8((int) (error * val) + grayScaleImage.getPixel(ix, iy));
                        grayScaleImage.setPixel(ix, iy, gray);
                    }
                }
            }
        }
    
        return grayScaleImage;
    }


    public static ImageData ditherSelf(ImageData inData, int imageType) {
        var matrix = new double[] {
            8 / 42, 4 / 42,
            2 / 42, 4 / 42, 8 / 42, 4 / 42, 2 / 42,
            1 / 42, 2 / 42, 4 / 42, 2 / 42, 1 / 42
        };
        var matrixStartIndex = matrix.length;
        var matrixDimension = 5;

        var grayScaleImage = GrayScaleConverter.convert(inData, imageType);

        for (int x = 0; x < grayScaleImage.width; x++) {
            for (int y = 0; y < grayScaleImage.height; y++) {

                var pixelValue = grayScaleImage.getPixel(x, y);
                if (pixelValue < 0) {
                    pixelValue = 0;
                } else if (pixelValue > 255) {
                    pixelValue = 255;
                }

                var newPixel = pixelValue < THRESHOLD ? 0 : 255;
                grayScaleImage.setPixel(x, y, newPixel);
                var error = pixelValue - newPixel;
       
                for (var i = 0; i < matrix.length; i++) {
                    var val = matrix[i];
                    var index = matrixStartIndex + i;
                    var col = (index / matrixDimension) - 2;
                    var row = (index % matrixDimension) - 2;
                    
                    var ix = x + col;
                    var iy = y + row;
       
                    if (ix < 0 || ix >= grayScaleImage.width || iy < 0 || iy >= grayScaleImage.height) {
                        continue;
                    }

                    int gray = ImageProcessing.clamp8((error * val) + grayScaleImage.getPixel(ix, iy));
                    grayScaleImage.setPixel(ix, iy, gray);
                }
            }
        }

        return grayScaleImage;
    }
    
}
