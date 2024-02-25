package imageprocessing.colors;

import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;

public class GrayScaleConverter implements IImageProcessor {

	@Override
	public boolean isEnabled(int imageType) {
		return true;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		ImageData outData = (ImageData)inData.clone();
        
		return outData;
	}
    
}
