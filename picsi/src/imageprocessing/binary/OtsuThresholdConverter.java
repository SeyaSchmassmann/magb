package imageprocessing.binary;

import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import main.Picsi;
import utils.Parallel;

public class OtsuThresholdConverter implements IImageProcessor {

    public static int BACKGROUND_COLOR = 0;
    public static int FOREGROUND_COLOR = 1;

    @Override
    public boolean isEnabled(int imageType) {
        return imageType == Picsi.IMAGE_TYPE_GRAY;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        final int threshold = otsuThreshold(inData);
        
        return binarization(inData, threshold, false, true);
    }

    public static ImageData binarization(ImageData inData, int threshold, boolean smallValuesAreForeground, boolean binary) {
        // assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;
        
        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, binary ? Picsi.IMAGE_TYPE_BINARY : Picsi.IMAGE_TYPE_GRAY);
        final int fg = (smallValuesAreForeground) ? FOREGROUND_COLOR : BACKGROUND_COLOR;
        final int bg = (smallValuesAreForeground) ? BACKGROUND_COLOR : FOREGROUND_COLOR;
        
        Parallel.For(0, inData.height, v -> {
            for (int u=0; u < inData.width; u++) {
                outData.setPixel(u, v, (inData.getPixel(u,v) <= threshold) ? fg : bg);
            }
        });
        return outData;
    }
    
    public static int otsuThreshold(ImageData inData) {
        int[] histogram = new int[256];

        for (int y = 0; y < inData.height; y++) {
            for (int x = 0; x < inData.width; x++) {
                int gray = 0xFF & inData.getPixel(x, y);
                histogram[gray]++;
            }
        }
        
        int total = inData.width * inData.height;
        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }
        
        double sumB = 0;
        int wB = 0;
        int wF = 0;
        double varMax = 0;
        int threshold = 0;
        
        for (int i = 0; i < 256; i++) {
            wB += histogram[i];
            if (wB == 0) {
                continue;
            }
            wF = total - wB;
            if (wF == 0) {
                break;
            }
            
            sumB += i * histogram[i];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            
            double varBetween = wB * wF * (mB - mF) * (mB - mF);
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }
        
        return threshold;
    }

}
