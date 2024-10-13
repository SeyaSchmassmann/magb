package imageprocessing.filter;

import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;
import main.Picsi;

public class MedianFilter implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        var radius = 1;

        var outData = (ImageData)inData.clone();
        var width = inData.width;
        var height = inData.height;
        var pixels = new int[width * height];
        inData.getPixels(0, 0, pixels.length, pixels, 0);

        var grays = new int[(radius * 2 + 1) * (radius * 2 + 1)];

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var count = 0;
                for (var ky = -radius; ky <= radius; ky++) {
                    for (var kx = -radius; kx <= radius; kx++) {
                        var pixel = pixels[Math.min(Math.max(y + ky, 0), height - 1) * width + Math.min(Math.max(x + kx, 0), width - 1)];
                        grays[count++] = pixel;
                    }
                }
                java.util.Arrays.sort(grays, 0, count);
                outData.setPixel(x, y, grays[count / 2]);
            }
        }

        return outData;
    }
    
}
