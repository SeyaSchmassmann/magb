package imageprocessing.bayerPattern;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import utils.Parallel;

import main.Picsi;

public class DebayeringConverter implements IImageProcessor {

    private final static int COLOR_CHANNEL_RED = 0;
    private final static int COLOR_CHANNEL_GREEN = 1;
    private final static int COLOR_CHANNEL_BLUE = 2;

    private final static int[][] BAYER_PATTERN = new int[][] {
        { COLOR_CHANNEL_BLUE, COLOR_CHANNEL_GREEN, },
        { COLOR_CHANNEL_GREEN, COLOR_CHANNEL_RED, }
    };

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        return debayer(inData, imageType);
    }

    public static ImageData debayer(ImageData inData, int imageType) {
        var outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        Parallel.For(0, outData.height, y -> {
            for (int x = 0; x < outData.width; x++) {
                var rgb = interpolateRGB(inData, x, y);
                outData.setPixel(x, y, outData.palette.getPixel(rgb));
            }
        });

        return outData;
    }

    private static RGB interpolateRGB(ImageData imageData, int x, int y) {
        var rgb = new RGB(0, 0, 0);
        
        var mainColorChannel = BAYER_PATTERN[y % 2][x % 2];

        switch (mainColorChannel) {
            case COLOR_CHANNEL_RED:
                rgb.red = imageData.getPixel(x, y);
                rgb.green = interpolateSurroundingCross(imageData, x, y);
                rgb.blue = interpolateCorners(imageData, x, y);
                break;
            case COLOR_CHANNEL_GREEN:
                rgb.green = imageData.getPixel(x, y);

                var leftColorChannel = x > 0
                    ? BAYER_PATTERN[y % 2][(x - 1) % 2]
                    : COLOR_CHANNEL_RED; // if it is first column then left would be red due to bayer pattern

                if (leftColorChannel == COLOR_CHANNEL_RED) {
                    rgb.red = interpolateLeftAndRight(imageData, x, y);
                    rgb.blue = interpolateTopAndBottom(imageData, x, y);
                } else {
                    rgb.red = interpolateTopAndBottom(imageData, x, y);
                    rgb.blue = interpolateLeftAndRight(imageData, x, y);
                }

                break;
            case COLOR_CHANNEL_BLUE:
                rgb.red = interpolateCorners(imageData, x, y);
                rgb.green = interpolateSurroundingCross(imageData, x, y);
                rgb.blue = imageData.getPixel(x, y); 
                break;
        }

        return rgb;
    }

    private static int interpolateCorners(ImageData imageData, int x, int y) {
        var sum = 0;
        var divisor = 0;
        
        if (x > 0 && y > 0) {
            sum += imageData.getPixel(x - 1, y - 1);
            divisor++;
        }

        if (x < imageData.width - 1 && y > 0) {
            sum += imageData.getPixel(x + 1, y - 1);
            divisor++;
        }

        if (x > 0 && y < imageData.height - 1) {
            sum += imageData.getPixel(x - 1, y + 1);
            divisor++;
        }

        if (x < imageData.width - 1 && y < imageData.height - 1) {
            sum += imageData.getPixel(x + 1, y + 1);
            divisor++;
        }

        return sum / divisor;
    }

    private static int interpolateSurroundingCross(ImageData imageData, int x, int y) {
        var sum = 0;
        var divisor = 0;

        if (x > 0) {
            sum += imageData.getPixel(x - 1, y);
            divisor++;
        }

        if (x < imageData.width - 1) {
            sum += imageData.getPixel(x + 1, y);
            divisor++;
        }

        if (y > 0) {
            sum += imageData.getPixel(x, y - 1);
            divisor++;
        }

        if (y < imageData.height - 1) {
            sum += imageData.getPixel(x, y + 1);
            divisor++;
        }

        return sum / divisor;
    }

    private static int interpolateTopAndBottom(ImageData imageData, int x, int y) {
        var sum = 0;
        var divisor = 0;

        if (y > 0) {
            sum += imageData.getPixel(x, y - 1);
            divisor++;
        }

        if (y < imageData.height - 1) {
            sum += imageData.getPixel(x, y + 1);
            divisor++;
        }

        return sum / divisor;
    }

    private static int interpolateLeftAndRight(ImageData imageData, int x, int y) {
        var sum = 0;
        var divisor = 0;

        if (x > 0) {
            sum += imageData.getPixel(x - 1, y);
            divisor++;
        }

        if (x < imageData.width - 1) {
            sum += imageData.getPixel(x + 1, y);
            divisor++;
        }

        return sum / divisor;
    }
    
}
