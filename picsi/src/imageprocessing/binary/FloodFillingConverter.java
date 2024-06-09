package imageprocessing.binary;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import imageprocessing.colors.GrayScaleConverter;
import main.Picsi;

public class FloodFillingConverter implements IImageProcessor {

    private final static int BACKGROUND_COLOR = 0;
    private final static int FOREGROUND_COLOR = 1;

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData inData, int imageType) {
        var workerImage = (ImageData) inData.clone();
        if (imageType != Picsi.IMAGE_TYPE_GRAY){
            workerImage = GrayScaleConverter.convert(workerImage, imageType);
        }
        final int threshold = OtsuThresholdConverter.otsuThreshold(workerImage);
        
        var smallValuesAreForeground = determineForegroundFromHistogram(workerImage);
        
        workerImage = OtsuThresholdConverter.binarization(workerImage, threshold, smallValuesAreForeground, false);
        workerImage = MorphologicFilter.closing(workerImage, MorphologicFilter.s_circle5, 3, 3);

        var particles = floodFill(workerImage);
        var falseColorImage = falseColor(workerImage, particles.length + 1);

        drawParticles(falseColorImage, particles);

        printParticles(particles);

        return falseColorImage;
    }

    // Method to determine if small values are foreground
    private static boolean determineForegroundFromHistogram(ImageData imageData) {
        int[] histogram = new int[256];

        // Calculate histogram
        for (int y = 0; y < imageData.height; y++) {
            for (int x = 0; x < imageData.width; x++) {
                int pixel = imageData.getPixel(x, y);
                histogram[pixel]++;
            }
        }

        // Analyze histogram: If more than 50% of the pixels are dark, consider small values as foreground
        int totalPixels = imageData.width * imageData.height;
        int darkPixels = 0;
        for (int i = 0; i < 128; i++) {
            darkPixels += histogram[i];
        }

        return darkPixels <= (totalPixels / 2);
    }

    public static Particle[] floodFill(ImageData imageData) {
        int nextLabel = 2;
        Point[] deltas = {new Point(1, 0), new Point(-1, 0), new Point(0, 1), new Point(0, -1)};
        
        var particle = new ArrayList<Particle>();
        
        for (int y = 0; y < imageData.height; y++) {
            for (int x = 0; x < imageData.width; x++) {

                if (imageData.getPixel(x, y) == FOREGROUND_COLOR) {
                    var queue = new LinkedList<Point>();
                    queue.add(new Point(x, y));
                    imageData.setPixel(x, y, nextLabel);
                    var box = new BoundingBox(x, x, y, y);
                    
                    while (!queue.isEmpty()) {
                        Point current = queue.poll();

                        for (Point delta : deltas) {
                            int nx = current.x + delta.x;
                            int ny = current.y + delta.y;

                            if (nx >= 0 && nx < imageData.width && ny >= 0 && ny < imageData.height
                                && imageData.getPixel(nx, ny) == FOREGROUND_COLOR) {
                                queue.add(new Point(nx, ny));
                                imageData.setPixel(nx, ny, nextLabel);
                                box = updateBoundingBox(box, nx, ny);
                            }
                        }
                    }
                    var moment = calculateMoments(box, nextLabel, imageData);
                    particle.add(new Particle(nextLabel - 1, box, moment));
                    nextLabel++;
                }
            }
        }
        
        return particle.toArray(new Particle[0]);
    }

    public static ImageData falseColor(ImageData inData, int n) {
        // assert Picsi.determineImageType(inData) == Picsi.IMAGE_TYPE_GRAY;        
        // assert 0 < n && n <= 256;

        ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);

        for (int y = 0; y < inData.height; y++) {
            for (int x = 0; x < inData.width; x++) {
                int label = inData.getPixel(x, y);
                
                int gray = (int)((float)label / (float)n * 255);

                outData.setPixel(x, y, gray << 16 | gray << 8 | gray);
            }
        }

        return outData;
    }

    private static void drawParticles(ImageData inData, Particle[] particles) {
        for (Particle particle : particles) {
            BoundingBox box = particle.boundingBox();
            MomentResult moments = particle.momentResult();

            // Draw the bounding box
            for (int x = box.x1(); x <= box.x2(); x++) {
                inData.setPixel(x, box.y1(), 255 << 16); // Red color for top and bottom borders
                inData.setPixel(x, box.y2(), 255 << 16);
            }
            for (int y = box.y1(); y <= box.y2(); y++) {
                inData.setPixel(box.x1(), y, 255 << 16); // Red color for left and right borders
                inData.setPixel(box.x2(), y, 255 << 16);
            }

            // Draw the center of mass
            Point centerOfMass = moments.centerOfMass();
            for (int x = centerOfMass.x - 1; x <= centerOfMass.x + 1; x++) {
                for (int y = centerOfMass.y - 1; y <= centerOfMass.y + 1; y++) {
                    inData.setPixel(x, y, 255); // White color for the center of mass
                }
            }

            // Draw the eccentricity (as a simple visual representation)
            // For simplicity, let's draw a line along the orientation with a length proportional to eccentricity
            double orientation = moments.orientation();
            var lineLength = (int) Math.sqrt(moments.area() / Math.PI); // Scale factor for visualization
            // int lineLength = (int) (meanLength); // Scale factor for visualization

            for (int i = -lineLength; i <= lineLength; i++) {
                int dx = (int) (i * Math.cos(orientation));
                int dy = (int) (i * Math.sin(orientation));
                int x = centerOfMass.x + dx;
                int y = centerOfMass.y + dy;
                if (x >= 0 && x < inData.width && y >= 0 && y < inData.height) {
                    inData.setPixel(x, y, 255 << 8); // Green color for the orientation line
                }
            }
        }
    }

    private static void printParticles(Particle[] particles) {
        System.out.println("| Label | Center of Mass (x,y) | Bounding Box (x1,y1),(x2,y2)    | Eccentricity | Orientation | Area    |");
        System.out.println("|-------|----------------------|---------------------------------|--------------|-------------|---------|");

        for (Particle particle : particles) {
            Point centerOfMass = particle.momentResult.centerOfMass();
            double eccentricity = particle.momentResult.eccentricity();
            double orientation = particle.momentResult.orientation();
            var box = particle.boundingBox();
            int area = particle.momentResult.area();

            System.out.printf("| %-5d |       %6d, %6d | (%6d,%6d),(%6d,%6d) | %12.6f | %11.6f | %7d |%n",
                    particle.label(), centerOfMass.x, centerOfMass.y, box.x1, box.y1, box.x2, box.y2, eccentricity, orientation, area);
        }
        System.out.println("");
    }

    private static BoundingBox updateBoundingBox(BoundingBox box, int x, int y) {
        return new BoundingBox(
            Math.min(box.x1, x),
            Math.max(box.x2, x),
            Math.min(box.y1, y),
            Math.max(box.y2, y)
        );
    }

    public static MomentResult calculateMoments(BoundingBox boundingBox, int label, ImageData imageData) {
        long sumX = 0, sumY = 0;
        long sumX2 = 0, sumY2 = 0, sumXY = 0;
        int count = 0;

        for (int y = boundingBox.y1(); y <= boundingBox.y2(); y++) {
            for (int x = boundingBox.x1(); x <= boundingBox.x2(); x++) {
                if (imageData.getPixel(x, y) == label) {
                    sumX += x;
                    sumY += y;
                    sumX2 += x * x;
                    sumY2 += y * y;
                    sumXY += x * y;
                    count++;
                }
            }
        }

        if (count == 0) {
            return new MomentResult(0, new Point(0, 0), 0.0, 0.0);
        }

        double centerX = (double) sumX / count;
        double centerY = (double) sumY / count;

        double mu20 = (double) sumX2 / count - centerX * centerX;
        double mu02 = (double) sumY2 / count - centerY * centerY;
        double mu11 = (double) sumXY / count - centerX * centerY;

        double orientation = 0.5 * Math.atan2(2 * mu11, (mu20 - mu02));
        double commonTerm = Math.sqrt((mu20 - mu02) * (mu20 - mu02) + 4 * mu11 * mu11);
        double majorAxis = Math.sqrt(2 * (mu20 + mu02 + commonTerm));
        double minorAxis = Math.sqrt(2 * (mu20 + mu02 - commonTerm));

        double eccentricity = (majorAxis != 0) ? Math.sqrt(1 - (minorAxis * minorAxis) / (majorAxis * majorAxis)) : 0.0;

        return new MomentResult(count, new Point((int) centerX, (int) centerY), orientation, eccentricity);
    }

    public static record BoundingBox(int x1, int x2, int y1, int y2) { }
    
    public static record Particle(int label, BoundingBox boundingBox, MomentResult momentResult) { }

    private static record MomentResult(int area, Point centerOfMass, double orientation, double eccentricity) {}

}
