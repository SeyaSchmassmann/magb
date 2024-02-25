package imageprocessing.colors;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import main.Picsi;
import utils.Parallel;

public class GrayScaleConverter implements IImageProcessor {

	@Override
	public boolean isEnabled(int imageType) {
        return imageType != Picsi.IMAGE_TYPE_GRAY && imageType != Picsi.IMAGE_TYPE_INDEXED;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
        return convert(inData, imageType);
	}
 
	/**
	 * Invert image data
	 * @param output image
	 * @param imageType
	 */
	public static ImageData convert(ImageData inData, int imageType) {
        var grayScaleImage = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_GRAY);

        Parallel.For(0, inData.height, y -> {
            for (int x = 0; x < inData.width; x++) {
                var rgb = inData.palette.getRGB(inData.getPixel(x, y));
                int gray = ImageProcessing.clamp8(0.299 * rgb.red + 0.587 * rgb.green + 0.114 * rgb.blue);
                grayScaleImage.setPixel(x, y, gray);
            }
        });

        return grayScaleImage;
    }
}