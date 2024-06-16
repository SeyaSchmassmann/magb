package imageprocessing;

import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import main.Picsi;
import utils.Parallel;

public class ParticleAnalyzer implements IImageProcessor {

    private static final Point[] NEIGHBOR_MOVES_MATRIX = {
        new Point(1, 0),
        new Point(-1, 0),
        new Point(0, 1),
        new Point(0, -1)
    };
    
	private static final boolean[][] MORPHOLOGY_STRUCTURE = new boolean[][] {
        { false, true, false },
        { true,  true, true  },
        { false, true, false }
    };
    private static final int MORPHOLOGY_STRUCTURE_DIMENSION = MORPHOLOGY_STRUCTURE.length / 2;
    
    private static final int COLOR_GREEN = 0x00FF00;
    private static final int COLOR_BLUE = 0xFF0000;
    private static final int COLOR_RED = 0x0000FF;

    private static final int BINARY_COLOR_BACKGROUND = 0;
    private static final int BINARY_COLOR_FOREGROUND = 1;

    private static final int THRESHOLD_PARTICLE_AREA = 10;

    @Override
    public boolean isEnabled(int imageType) {
        return true;
    }

    @Override
    public ImageData run(ImageData sourceImage, int imageType) {
        var targetImage = sourceImage;
        
        if (imageType != Picsi.IMAGE_TYPE_GRAY && imageType != Picsi.IMAGE_TYPE_BINARY) {
            targetImage = convertToGrayscale(sourceImage);
        }
        
        if (imageType != Picsi.IMAGE_TYPE_BINARY) {
            final var otsuThreshold = otsuThreshold(targetImage);
            targetImage = convertToBinary(targetImage, otsuThreshold.threshold(), otsuThreshold.smallValuesAreForeground());
        }
        
        targetImage = closing(targetImage);
        fillHolesInsideBorder(targetImage);

        final var particles = floodFillAndParticleAnalyzation(targetImage);
        final var falseColorImage = falseColor(targetImage, particles.length + 1);

        drawParticles(falseColorImage, particles);
        printParticles(particles);

        return falseColorImage;
    }


    private static ImageData convertToGrayscale(ImageData image) {
        final var targetImage = ImageProcessing.createImage(image.width, image.height, Picsi.IMAGE_TYPE_GRAY);
        final var wr = 0.299;
        final var wg = 0.587;
        final var wb = 0.114;

        Parallel.For(0, image.height, y -> {
            for (var x = 0; x < image.width; x++) {
                var rgb = image.palette.getRGB(image.getPixel(x, y));
                var gray = ImageProcessing.clamp8(wr * rgb.red + wg * rgb.green + wb * rgb.blue);
                targetImage.setPixel(x, y, gray);
            }
        });

        return targetImage;
    }
    
    private static OtsuThreshold otsuThreshold(ImageData image) {
        var histogram = ImageProcessing.histogram(image, 256);
        
        var totalPixels = image.width * image.height;

        var sum = 0d;
        for (int i = 0; i < histogram.length; i++) {
            sum += i * histogram[i];
        }
        
        var sumB = 0d;
        var wB = 0;
        var wF = 0;
        var varMax = 0d;
        var threshold = 0;
        var darkPixels = 0;
        
        for (var i = 0; i < histogram.length; i++) {
            wB += histogram[i];
            if (wB == 0) {
                continue;
            }
            wF = totalPixels - wB;
            if (wF == 0) {
                break;
            }
            
            sumB += i * histogram[i];
            var mB = sumB / wB;
            var mF = (sum - sumB) / wF;
            
            var varBetween = wB * wF * (mB - mF) * (mB - mF);
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }

            if (i < histogram.length / 2) {
                darkPixels += histogram[i];
            }
        }

        // naive approach to determine if small values are foreground: check wheter the dark or light pixels dominate the image
        // if the number of dark pixels ([0 - 128]) is less than half of the total pixels, then small values are foreground
        var smallValuesAreForeground = darkPixels <= (totalPixels / 2);
        
        return new OtsuThreshold(threshold, smallValuesAreForeground);
    }

    private static record OtsuThreshold(int threshold, boolean smallValuesAreForeground) {}

    private static ImageData convertToBinary(ImageData image, int threshold, boolean smallValuesAreForeground) {        
        final var targetImage = ImageProcessing.createImage(image.width, image.height, Picsi.IMAGE_TYPE_GRAY);
        final var foregroundColor = (smallValuesAreForeground) ? BINARY_COLOR_FOREGROUND : BINARY_COLOR_BACKGROUND;
        final var backgroundColor = (smallValuesAreForeground) ? BINARY_COLOR_BACKGROUND : BINARY_COLOR_FOREGROUND;
        
        Parallel.For(0, image.height, y -> {
            for (var x = 0; x < image.width; x++) {
                targetImage.setPixel(x, y, (image.getPixel(x,y) <= threshold) ? foregroundColor : backgroundColor);
            }
        });

        return targetImage;
    }

    private static ImageData closing(ImageData image) {
        var targetImage = dilation(image);
        return erosion(targetImage);
    }

    private static ImageData erosion(ImageData image) {        
        var targetImage = (ImageData)image.clone();

        Parallel.For(0, image.height, y -> {
            for (int x=0; x < image.width; x++) {
                boolean set = true;
                
                for (int j=0; set && j < MORPHOLOGY_STRUCTURE.length; j++) {
                    final int v0 = y + j - MORPHOLOGY_STRUCTURE_DIMENSION;
                    
                    for (int i=0; set && i < MORPHOLOGY_STRUCTURE[j].length; i++) {
                        final int u0 = x + i - MORPHOLOGY_STRUCTURE_DIMENSION;
                        
                        if (MORPHOLOGY_STRUCTURE[j][i] && (v0 < 0 || v0 >= image.height || u0 < 0 || u0 >= image.width || image.getPixel(u0, v0) != BINARY_COLOR_FOREGROUND)) {
                            set = false;
                        }
                    }
                }
                if (set) {
                    targetImage.setPixel(x, y, BINARY_COLOR_FOREGROUND);
                } else {
                    targetImage.setPixel(x, y, BINARY_COLOR_BACKGROUND);
                }
            }
        });

        return targetImage;
    }

    private static ImageData dilation(ImageData image) {
        var targetImage = new ImageData(image.width, image.height, image.depth, image.palette);

        Parallel.For(0, targetImage.height, y -> {
            for (int x = 0; x < targetImage.width; x++) {
                boolean set = false;
                
                for (int j=0; !set && j < MORPHOLOGY_STRUCTURE.length; j++) {
                    final int v0 = y + j - MORPHOLOGY_STRUCTURE_DIMENSION;
                    
                    for (int i=0; !set && i < MORPHOLOGY_STRUCTURE[j].length; i++) {
                        final int u0 = x + i - MORPHOLOGY_STRUCTURE_DIMENSION;
                        
                        if (MORPHOLOGY_STRUCTURE[j][i] && v0 >= 0 && v0 < image.height && u0 >= 0 && u0 < image.width && image.getPixel(u0, v0) == BINARY_COLOR_FOREGROUND) {
                            set = true;
                        }
                    }
                }
                if (set) {
                    targetImage.setPixel(x, y, BINARY_COLOR_FOREGROUND);
                } else {
                    targetImage.setPixel(x, y, BINARY_COLOR_BACKGROUND);
                }
            }
        });
        
        return targetImage;
    }

     private static void fillHolesInsideBorder(ImageData imageData) {
        var tempColor = -1;
        int width = imageData.width;
        int height = imageData.height;

        // Flood fill the background from the borders with a temporary color
        for (int y = 0; y < height; y++) {
            floodFill(imageData, 0, y, BINARY_COLOR_BACKGROUND, tempColor);
            floodFill(imageData, width - 1, y, BINARY_COLOR_BACKGROUND, tempColor);
        }
        for (int x = 0; x < width; x++) {
            floodFill(imageData, x, 0, BINARY_COLOR_BACKGROUND, tempColor);
            floodFill(imageData, x, height - 1, BINARY_COLOR_BACKGROUND, tempColor);
        }

        // Identify and fill the holes with foreground color
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imageData.getPixel(x, y) == BINARY_COLOR_BACKGROUND) {
                    floodFill(imageData, x, y, BINARY_COLOR_BACKGROUND, BINARY_COLOR_FOREGROUND);
                }
            }
        }

        // Revert the temporary color back to the original background color
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (imageData.getPixel(x, y) == tempColor) {
                    imageData.setPixel(x, y, BINARY_COLOR_BACKGROUND);
                }
            }
        }
    }

    private static BoundingBox floodFill(ImageData imageData, int x, int y, int targetColor, int replacementColor) {
        var queue = new LinkedList<Point>();
        queue.add(new Point(x, y));
        imageData.setPixel(x, y, replacementColor);
        var box = new BoundingBox(x, y);

        while (!queue.isEmpty()) {
            var current = queue.poll();

            for (var delta : NEIGHBOR_MOVES_MATRIX) {
                var nx = current.x + delta.x;
                var ny = current.y + delta.y;
 
                if (nx >= 0 && nx < imageData.width && ny >= 0 && ny < imageData.height) {
                    if (imageData.getPixel(nx, ny) == targetColor) {
                        queue.add(new Point(nx, ny));
                        imageData.setPixel(nx, ny, replacementColor);
                        box.updateBoundingBox(nx, ny);
                    }
                }
            }
        }
        return box;
    }

    private static Particle[] floodFillAndParticleAnalyzation(ImageData imageData) {
        var nextLabel = 2;
        final var particle = new ArrayList<Particle>();
    
        for (int y = 0; y < imageData.height; y++) {
            for (int x = 0; x < imageData.width; x++) {
                if (imageData.getPixel(x, y) == BINARY_COLOR_FOREGROUND) {
                    var box = floodFill(imageData, x, y, BINARY_COLOR_FOREGROUND, nextLabel);

                    var perimeter = calculatePerimeter(imageData, x, y, nextLabel);
                    var moment = calculateMoments(box, nextLabel, imageData);
    
                    if (moment.area < THRESHOLD_PARTICLE_AREA) {
                        continue;
                    }

                    particle.add(new Particle(nextLabel - 1,
                                              box,
                                              moment.area,
                                              moment.centerOfMass,
                                              moment.orientation,
                                              moment.eccentricity,
                                              perimeter,
                                              perimeter * 0.95,
                                              calculateCircularity(moment.area, perimeter * 0.95),
                                              calculateCircularity(moment.area, perimeter)));
                    
                    nextLabel++;
                }
            }
        }
    
        return particle.toArray(new Particle[0]);
    }

    private static int calculatePerimeter(ImageData imageData, int x, int y, int label) {
        int perimeter = 0;
        int width = imageData.width;
        int height = imageData.height;
    
        boolean[][] visited = new boolean[width][height];
        var queue = new LinkedList<Point>();
        queue.add(new Point(x, y));
        visited[x][y] = true;
    
        while (!queue.isEmpty()) {
            var current = queue.poll();
            var isPerimeter = false;
    
            for (var delta : NEIGHBOR_MOVES_MATRIX) {
                var nx = current.x + delta.x;
                var ny = current.y + delta.y;
    
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (imageData.getPixel(nx, ny) != label) {
                        isPerimeter = true;
                    } else if (!visited[nx][ny]) {
                        queue.add(new Point(nx, ny));
                        visited[nx][ny] = true;
                    }
                } else {
                    isPerimeter = true;
                }
            }
    
            if (isPerimeter) {
                perimeter++;
            }
        }
    
        return perimeter;
    }

    private static class BoundingBox {

        private int x1;
        private int x2;
        private int y1;
        private int y2;

        private BoundingBox(int x, int y) {
            x1 = x;
            x2 = x;
            y1 = y;
            y2 = y;
        }

        private void updateBoundingBox(int x, int y) {
            x1 = Math.min(x1, x);
            x2 = Math.max(x2, x);
            y1 = Math.min(y1, y);
            y2 = Math.max(y2, y);
        }
    }
    
    private static record Particle(int label, BoundingBox boundingBox, int area, Point centerOfMass, double orientation, double eccentricity, int perimeter, double perimeterCorrected, double circularity, double circularityCorrected) { }

    private static double calculateCircularity(double area, double perimeter) {
        return (4 * Math.PI * area) / (perimeter * perimeter);
    }

    private static MomentResult calculateMoments(BoundingBox boundingBox, int label, ImageData imageData) {
        var sumX = 0l;
        var sumY = 0l;
        var sumX2 = 0l;
        var sumY2 = 0l;
        var sumXY = 0l;
        var count = 0;

        for (int y = boundingBox.y1; y <= boundingBox.y2; y++) {
            for (int x = boundingBox.x1; x <= boundingBox.x2; x++) {
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

        var centerX = (double) sumX / count;
        var centerY = (double) sumY / count;

        var mu20 = (double) sumX2 / count - centerX * centerX;
        var mu02 = (double) sumY2 / count - centerY * centerY;
        var mu11 = (double) sumXY / count - centerX * centerY;

        var orientation = 0.5 * Math.atan2(2 * mu11, (mu20 - mu02));

        var eccentricity = ((mu20 - mu02) * (mu20 - mu02) + 4 * mu11 * mu11) / (mu20 + mu02) / (mu20 + mu02);

        return new MomentResult(count, new Point((int) centerX, (int) centerY), orientation, eccentricity);
    }

    private static record MomentResult(int area, Point centerOfMass, double orientation, double eccentricity) {}

    private static ImageData falseColor(ImageData image, int particleCount) {
        var targetImage = ImageProcessing.createImage(image.width, image.height, Picsi.IMAGE_TYPE_RGB);

        for (var y = 0; y < image.height; y++) {
            for (var x = 0; x < image.width; x++) {
                var label = image.getPixel(x, y);
                var gray = (int)((float)label / (float)particleCount * 255);
                targetImage.setPixel(x, y, gray << 16 | gray << 8 | gray);
            }
        }

        return targetImage;
    }

    private static void printParticles(Particle[] particles) {
        System.out.println("| Label | Center of Mass (x,y) | Bounding Box (x1,y1),(x2,y2)    | Eccentricity | Orientation (deg) | Area         | Perimeter    | Perimeter (corrected) | Circularity  | Circularity (corrected) |");
        System.out.println("|-------|----------------------|---------------------------------|--------------|-------------------|--------------|--------------|-----------------------|--------------|-------------------------|");

        for (var particle : particles) {
            final var centerOfMass = particle.centerOfMass;
            final var box = particle.boundingBox;

            System.out.printf("| %-5d |       %6d, %6d | (%6d,%6d),(%6d,%6d) | %12.6f |      %12.6f | %12d | %12d |          %12.6f | %12.6f |            %12.6f |%n",
                    particle.label(),
                    centerOfMass.x, centerOfMass.y,
                    box.x1, box.y1, box.x2, box.y2,
                    particle.eccentricity,
                    Math.toDegrees(particle.orientation),
                    particle.area,
                    particle.perimeter,
                    particle.perimeterCorrected,
                    particle.circularity,
                    particle.circularityCorrected);
        }
        System.out.println("");
    }

    private static void drawParticles(ImageData image, Particle[] particles) {
        for (Particle particle : particles) {
            var box = particle.boundingBox;

            // Draw bounding box
            for (var x = box.x1; x <= box.x2; x++) {
                image.setPixel(x, box.y1, COLOR_BLUE);
                image.setPixel(x, box.y2, COLOR_BLUE);
            }
            for (var y = box.y1; y <= box.y2; y++) {
                image.setPixel(box.x1, y, COLOR_BLUE);
                image.setPixel(box.x2, y, COLOR_BLUE);
            }

            // Draw center of mass
            var centerOfMass = particle.centerOfMass;
            for (var x = centerOfMass.x - 1; x <= centerOfMass.x + 1; x++) {
                for (var y = centerOfMass.y - 1; y <= centerOfMass.y + 1; y++) {
                    image.setPixel(x, y, COLOR_RED);
                }
            }

            // Draw orientation line
            var orientation = particle.orientation;
            var lineLength = (Math.abs(box.x1 - box.x2) + Math.abs(box.y1 - box.y2)) / 4;
            
            for (var i = -lineLength; i <= lineLength; i++) {
                var dx = (int) (i * Math.cos(orientation));
                var dy = (int) (i * Math.sin(orientation));
                var x = centerOfMass.x + dx;
                var y = centerOfMass.y + dy;
                if (x >= 0 && x < image.width && y >= 0 && y < image.height) {
                    image.setPixel(x, y, COLOR_GREEN);
                }
            }
        }
    }

}
