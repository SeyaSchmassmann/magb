package imageprocessing.filter;

import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;
import main.Picsi;

public class GaussFilter implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        var sigma = 1.0;
        var radius = 3;
        var kernel = new double[radius * 2 + 1][radius * 2 + 1];
        var sum = 0.0;
        for (var y = -radius; y <= radius; y++) {
            for (var x = -radius; x <= radius; x++) {
                var g = Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                kernel[y + radius][x + radius] = g;
                sum += g;
            }
        }

        for (var y = 0; y < kernel.length; y++) {
            for (var x = 0; x < kernel[y].length; x++) {
                kernel[y][x] /= sum;
            }
        }

        var outData = (ImageData)inData.clone();
        var width = inData.width;
        var height = inData.height;
        var pixels = new int[width * height];
        inData.getPixels(0, 0, pixels.length, pixels, 0);

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var sumGray = 0.0;
                for (var ky = -radius; ky <= radius; ky++) {
                    for (var kx = -radius; kx <= radius; kx++) {
                        var pixel = pixels[Math.min(Math.max(y + ky, 0), height - 1) * width + Math.min(Math.max(x + kx, 0), width - 1)];
                        sumGray += pixel * kernel[ky + radius][kx + radius];
                    }
                }
                outData.setPixel(x, y, (int) sumGray);
            }
        }

        return outData;
    }
    
}
