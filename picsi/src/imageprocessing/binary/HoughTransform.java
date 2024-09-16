package imageprocessing.binary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import imageprocessing.colors.GrayScaleConverter;
import main.Picsi;
import utils.Parallel;

/**
 * Hough Transform
 * @author Christoph Stamm
 *
 */
public class HoughTransform implements IImageProcessor {
	@Override
	public boolean isEnabled(int imageType) {
		return true;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
        var binImg = (ImageData)inData.clone();
        if (imageType != Picsi.IMAGE_TYPE_GRAY){
            binImg = GrayScaleConverter.convert(binImg, imageType);
        }
        final int threshold = OtsuThresholdConverter.otsuThreshold(binImg).threshold();
        binImg = OtsuThresholdConverter.binarization(binImg, threshold, true, true);
        boolean[][] s_square4 = new boolean[][] {{ true, true, true, true},{true, true, true, true},{true, true, true, true},{true, true, true, true}};
        binImg = MorphologicFilter.contour(binImg, Picsi.IMAGE_TYPE_BINARY, s_square4, 1, 1, true);

		var paramSpace = rhoThetaTransform(binImg);

        var threshold1 = calculateThreshold(paramSpace);
        // get shape of lines in paramSpace
        // return showParamSpace(paramSpace);
	    return drawDetectedLines(binImg, inData, paramSpace, 100);
	}

    public static int calculateThreshold(int[][] paramSpace) {
        // Flatten the 2D paramSpace array into a 1D array
        int rows = paramSpace.length;
        int cols = paramSpace[0].length;
        int[] flattenedArray = new int[rows * cols];
        int index = 0;
        
        for (int[] row : paramSpace) {
            for (int value : row) {
                flattenedArray[index++] = value;
            }
        }

        // Sort the 1D array
        Arrays.sort(flattenedArray);

        var thresholdIndex = (int) Math.ceil(0.99 * flattenedArray.length);
        var threshold = 10;// flattenedArray[thresholdIndex - 1];

        System.out.println("thresholdIndex: " + thresholdIndex);
        System.out.println("Threshold: " + threshold);
        System.out.println("flattenedArray.length: " + flattenedArray.length);
        System.out.println("Max: " + flattenedArray[flattenedArray.length - 1]);

        // Return the value at the threshold index
        return flattenedArray[flattenedArray.length - 30];
    }

	private static int[][] rhoThetaTransform(ImageData inData) {
		final int tMax = 180; // Number of theta values
		final int rMax = 180; // Number of rho values
		final int rMaxD2 = rMax / 2; // Half of rho max
		final int bg = 0; // Background value (assumed to be 0)
		final int wD2 = inData.width / 2; // Half of image width
		final int hD2 = inData.height / 2; // Half of image height
		final double dTheta = Math.PI / tMax; // Theta step
		final double dRho = Math.ceil(Math.hypot(wD2, hD2)) / rMaxD2; // Rho step
		int[][] paramSpace = new int[rMax][tMax]; // Accumulator array

		// Iterate through each pixel in the input image
		for (int x = 0; x < inData.width; x++) {
			for (int y = 0; y < inData.height; y++) {
				// Check if the pixel is an edge
				if ((inData.getPixel(x, y) & 0xFF) != bg) {
					// Transform to parameter space
					for (int t = 0; t < tMax; t++) {
						double theta = t * dTheta;
						double rho = (x - wD2) * Math.cos(theta) + (y - hD2) * Math.sin(theta);
						int r = (int) Math.round(rho / dRho) + rMaxD2;
						if (r >= 0 && r < rMax) {
							paramSpace[r][t]++;
						}
					}
				}
			}
		}

		return paramSpace;
	}
	
	private static ImageData showParamSpace(int[][] paramSpace) {
		final int size = 3 * 180;
		final int h = paramSpace.length;
		final int w = paramSpace[0].length;
		
		var outData = ImageProcessing.createImage(size, size, Picsi.IMAGE_TYPE_GRAY);
		
		// compute maximum value in paramSpace
		int[] max = new int[] {0};
		Parallel.For(0, h, 
			// creator
			() -> new int[] {0},
			// loop body
			(v, m) -> {
				for(int u = 0; u < w; u++) {
					if (paramSpace[v][u] > m[0]) m[0] = paramSpace[v][u];
				}
			},
			// reducer
			m -> {
				if (m[0] > max[0]) max[0] = m[0];
			}
		);
		
		final int maxVal = max[0];
		
		Parallel.For(0, outData.height, v -> {
			for(int u = 0; u < outData.width; u++) {
				outData.setPixel(u, v, 255 - paramSpace[v*h/size][u*w/size]*255/maxVal);
			}
		});
		return outData;
	}

    private static ImageData drawDetectedLines(ImageData inData, ImageData outData, int[][] paramSpace, int threshold) {
        final int tMax = paramSpace[0].length;
        final int rMax = paramSpace.length;
        final int rMaxD2 = rMax / 2;
        final double dTheta = Math.PI / tMax;
        final double dRho = Math.ceil(Math.hypot(inData.width / 2, inData.height / 2)) / rMaxD2;
        final int h = inData.height;
        final int w = inData.width;
        final RGB red = new RGB(255, 0, 0); // Line color
        final RGB blue = new RGB(0, 0, 255); // Bounding box color

		//ImageData outData = ImageProcessing.createImage(w, h, Picsi.IMAGE_TYPE_RGB);

        // Threshold to determine whether a line is detected
        //threshold = 200;

        System.out.println("Threshold: " + threshold);

        List<Line> lines = new ArrayList<>();

        // Iterate through the parameter space
        for (int r = 0; r < rMax; r++) {
            for (int t = 0; t < tMax; t++) {
                if (paramSpace[r][t] > threshold) {
                    double theta = t * dTheta;
                    double rho = r * dRho - rMaxD2 * dRho;

                    lines.add(new Line(rho, theta));

                    // Convert (rho, theta) to (x, y) coordinates and draw the line
                    for (int x = 0; x < w; x++) {
                        int y = (int) ((rho - (x - w / 2) * Math.cos(theta)) / Math.sin(theta) + h / 2);
                        if (y >= 0 && y < h) {
                            outData.setPixel(x, y, outData.palette.getPixel(red));
                        }
                    }
                    for (int y = 0; y < h; y++) {
                        int x = (int) ((rho - (y - h / 2) * Math.sin(theta)) / Math.cos(theta) + w / 2);
                        if (x >= 0 && x < w) {
                            outData.setPixel(x, y, outData.palette.getPixel(red));
                        }
                    }
                }
            }
        }

        // Filter out lines that are too close to each other
        List<Line> lines2 = new ArrayList<>();
        for (Line l1 : lines) {
            boolean close = false;
            for (Line l2 : lines2) {
                if (Math.abs(l1.rho - l2.rho) < 10 && Math.abs(l1.theta - l2.theta) < 0.1) {
                    close = true;
                    break;
                }
            }
            if (!close) {
                lines2.add(l1);
            }
        }

        var points = new ArrayList<Point>();

        // Draw bounding boxes around detected lines
        /*for (Line l1 : lines2) {
            for (Line l2 : lines2) {
                if (l1 != l2) {
                    var point = intersectionPoint(l1, l2 , w, h);
                    if (point != null) {
                        points.add(point);
                        // draw point and a boder of 2 pixel in out
                        // outData.setPixel(point.x, point.y, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x-1, point.y, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x+1, point.y, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x, point.y-1, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x-1, point.y-1, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x+1, point.y-1, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x, point.y+1, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x-1, point.y+1, outData.palette.getPixel(blue));
                        // outData.setPixel(point.x+1, point.y+1, outData.palette.getPixel(blue));
                    }
                }
            }
        }

        var boundingBoxX1 = points.stream().mapToInt(p -> p.x).min().getAsInt();
        var boundingBoxX2 = points.stream().mapToInt(p -> p.x).max().getAsInt();
        var boundingBoxY1 = points.stream().mapToInt(p -> p.y).min().getAsInt();
        var boundingBoxY2 = points.stream().mapToInt(p -> p.y).max().getAsInt();

        // Draw bounding box
        for (int x = boundingBoxX1; x <= boundingBoxX2; x++) {
            outData.setPixel(x, boundingBoxY1, outData.palette.getPixel(blue));
            outData.setPixel(x, boundingBoxY2, outData.palette.getPixel(blue));
        }
        for (int y = boundingBoxY1; y <= boundingBoxY2; y++) {
            outData.setPixel(boundingBoxX1, y, outData.palette.getPixel(blue));
            outData.setPixel(boundingBoxX2, y, outData.palette.getPixel(blue));
        }
*/
        return outData;
    }



    private static Point intersectionPoint(Line l1, Line l2, int w, int h) {
        double a1 = Math.cos(l1.theta);
        double b1 = Math.sin(l1.theta);
        double c1 = l1.rho;
        double a2 = Math.cos(l2.theta);
        double b2 = Math.sin(l2.theta);
        double c2 = l2.rho;

        double det = a1 * b2 - a2 * b1;
        if (det == 0) {
            return null;
        }

        double x = (b2 * c1 - b1 * c2) / det;
        double y = (a1 * c2 - a2 * c1) / det;

        x = x + w / 2;
        y = y + h / 2;

        if (x < 0 || x >= w || y < 0 || y >= h) {
            return null;
        }

        return new Point((int)x, (int)y);
    }

    private record Line(double rho, double theta) {}

    private record Point(int x, int y) {}

}
