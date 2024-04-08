package imageprocessing.transformations;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import utils.Matrix;

public class Rotator implements IImageProcessor {

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {        
        return rotate(inData, imageType);
    }

    public static ImageData rotate(ImageData inData, int imageType) {
        var rotationAngleDegrees = 45; 
        var rotationAngle = Math.toRadians(rotationAngleDegrees);

        var newWidth = inData.width * Math.cos(rotationAngle) + inData.height * Math.sin(rotationAngle);
        var newHeight = inData.width * Math.sin(rotationAngle) + inData.height * Math.cos(rotationAngle);

        var outData = ImageProcessing.createImage((int)newWidth, (int)newHeight, imageType);

        var yOrigin = Math.sqrt(Math.pow(inData.height * Math.cos(rotationAngle), 2) + Math.pow(inData.height * Math.sin(rotationAngle), 2)) / 2;
        var xOrigin = Math.sqrt(Math.pow(inData.width * Math.sin(rotationAngle), 2) + Math.pow(inData.width * Math.cos(rotationAngle), 2)) / 2;

        for (int yTrg = 0; yTrg < outData.height; yTrg++) {
            for (int xTrg = 0; xTrg < outData.width; xTrg++) {
                var xSrc = (int)(((xTrg - xOrigin) * Math.cos(rotationAngle)) - (yTrg - yOrigin) * Math.sin(rotationAngle) + xOrigin);
                var ySrc = (int)(((xTrg - xOrigin) * Math.sin(rotationAngle)) + (yTrg - yOrigin) * Math.cos(rotationAngle) + yOrigin);
                // var xSrc = (int)(((xTrg) * Math.cos(rotationAngle)) - (yTrg) * Math.sin(rotationAngle));
                // var ySrc = (int)(((xTrg) * Math.sin(rotationAngle)) + (yTrg) * Math.cos(rotationAngle));

                if (xSrc >= 0 && xSrc < inData.width && ySrc >= 0 && ySrc < inData.height) {
                    outData.setPixel(xTrg, yTrg, inData.getPixel(xSrc, ySrc));
                } else {
                    outData.setPixel(xTrg, yTrg, 0);
                }
            }
        }        

        return outData;        
    }

    public static ImageData rotate_backup(ImageData inData, int imageType) {
        var outData = (ImageData)inData.clone();
        
        var rotationAngleDegrees = 45; 
        var rotationAngle = Math.toRadians(rotationAngleDegrees);
        var rotationMatrix = new Matrix(new double[][] {
            new double[] { Math.cos(rotationAngle), Math.sin(rotationAngle), 0 },
            new double[] { -Math.sin(rotationAngle), Math.cos(rotationAngle), 0 },
            new double[] { 0, 0, 1 }
        });
        var rotationMatrixInverse = rotationMatrix.inverse();

        for (int yTrg = 0; yTrg < inData.height; yTrg++) {
            for (int xTrg = 0; xTrg < inData.width; xTrg++) {
                var newCoordinates = rotationMatrixInverse.multiply(new double[] { xTrg, yTrg, 1 });
                var xSrc = (int)newCoordinates[0];
                var ySrc = (int)newCoordinates[1];

                if (xSrc >= 0 && xSrc < inData.width && ySrc >= 0 && ySrc < inData.height) {
                    outData.setPixel(xTrg, yTrg, inData.getPixel(xSrc, ySrc));
                } else {
                    // Handle out-of-bounds case (e.g., set a default color or handle it as you see fit)
                    outData.setPixel(xTrg, yTrg, 0);
                }
            }
        }        

        return outData;        
    }
    
}
