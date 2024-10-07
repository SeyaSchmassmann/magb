package imageprocessing.patternmatching;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import gui.OptionPane;
import gui.RectTracker;
import imageprocessing.IImageProcessor;
import imageprocessing.ImageProcessing;
import imageprocessing.geometry.ROI;
import main.Picsi;
import utils.BoundedPQ;
import utils.Parallel;

/**
 * Pattern matching based on correlation coefficient
 * @author Christoph Stamm
 *
 */
public class PatternMatching implements IImageProcessor {
	public static class PMResult implements Comparable<PMResult> {
		public ROI m_roi;
		public double m_cl;
		
		public PMResult(ROI roi, double cl) {
			m_roi = roi; m_cl = cl;
		}
		
		public int compareTo(PMResult pm) {
			if (m_cl < pm.m_cl) return -1;
			else if (m_cl > pm.m_cl) return 1;
			else return 0;
		}
	}
	
	@Override
	public boolean isEnabled(int imageType) {
		return imageType == Picsi.IMAGE_TYPE_GRAY;
	}

	@Override
	public ImageData run(ImageData inData, int imageType) {
		// let the user choose the operation
		Object[] operations = { "Pattern Matching", "PM with modified Pattern", "User defined Pattern" };
		int f = OptionPane.showOptionDialog("Pattern Matching Operation", SWT.ICON_INFORMATION, operations, 0);
		if (f < 0) return null;
		
		final int intensityOffset = -50;
		final int contrastFactor = 4;
		final boolean predefinedPattern = f < 2;
		
		// pattern region
		Rectangle pr = null;
		
		if (predefinedPattern) {
			pr = new Rectangle(200, 310, 70, 50);
		} else {
			// let the user choose the ROI using a tracker
			RectTracker rt = new RectTracker();
			pr = rt.start(70, 50); //inData.width/2, inData.height/2, 70, 50);
		}
		
		final int nResults = 10*10;	// search nResults best matches
		final ROI pattern = new ROI((f > 0) ? (ImageData)inData.clone() : inData, pr);
		final int pw = pattern.getWidth();
		final int ph = pattern.getHeight();
		
		// pre-processing
		if (f == 1) {
            // modify pattern
            // todo: check???
            Parallel.For(0, ph, v -> {
                for(int u = 0; u < pw; u++) {
                    int p = pattern.getPixel(u, v);
                    int q = p + intensityOffset;
                    if (q < 0) q = 0;
                    else if (q > 255) q = 255;
                    q = (q - 128)*contrastFactor + 128;
                    pattern.setPixel(u, v, q);
                }
            });
        }
		
		// pattern matching
		BoundedPQ<PMResult> results = pm(inData, pattern, nResults);
		
		// create output
		ImageData outData = ImageProcessing.createImage(inData.width, inData.height, Picsi.IMAGE_TYPE_RGB);
		
		// copy inData to outData
		Parallel.For(0, inData.height, v -> {
			for(int u = 0; u < inData.width; u++) {
				RGB rgb = inData.palette.getRGB(inData.getPixel(u,v));
				outData.setPixel(u, v, outData.palette.getPixel(rgb));
			}
		});
		
		return createOutput(outData, results, nResults);
	}

	/**
	 * Pattern matching based on correlation coefficient
	 * @param inData
	 * @param pattern
	 * @param nResults number of best results
	 * @return results
	 */
	public static BoundedPQ<PMResult> pm(ImageData inData, ROI pattern, int nResults) {
		final int pw = pattern.getWidth();
		final int ph = pattern.getHeight();
		final int k = ph*pw;
		BoundedPQ<PMResult> results = new BoundedPQ<>(nResults);
        
        
        double r_ = 0.0;
        for (int yRoi = 0; yRoi < ph; yRoi++) {
            for (int xRoi = 0; xRoi < pw; xRoi++) {
                int q = pattern.getPixel(xRoi, yRoi);
                r_ += q;
            }
        }
        r_ /= k;

        
        var standardDeviation = 0.0;
        for (int yRoi = 0; yRoi < ph; yRoi++) {
            for (int xRoi = 0; xRoi < pw; xRoi++) {
                var difference = pattern.getPixel(xRoi, yRoi) - r_;
                standardDeviation += difference * difference;
            }
        }
        standardDeviation = Math.sqrt(standardDeviation / k);



        for (int y = 0; y < inData.height - ph; y++) {
            for (int x = 0; x < inData.width - pw; x++) {
                double sumIR = 0.0;
                double i_ = 0.0;
                double i2 = 0.0;
                
                for (int yRoi = 0; yRoi < ph; yRoi++) {
                    for (int xRoi = 0; xRoi < pw; xRoi++) {
                        int p = inData.getPixel(x + xRoi, y + yRoi);
                        int q = pattern.getPixel(xRoi, yRoi);
                        sumIR += p*q;
                        i_ += p;
                        i2 += p*p;
                    }
                }

                i_ /= k;
                
                var cl = (sumIR - k*i_*r_) / ( Math.sqrt(i2 - k*i_*i_) * standardDeviation * Math.sqrt(k) );

                results.add(new PMResult(new ROI(inData, new Rectangle(x, y, pw, ph)), cl));
            }
        }

		return results;
	}

	/**
	 * Show best matching results as rectangles in the input image
	 * @param outData output image
	 * @param pq
	 * @param nResults
	 * @return
	 */
	private ImageData createOutput(ImageData outData, BoundedPQ<PMResult> pq, int nResults) {
		ArrayList<PMResult> results = new ArrayList<>();
		
		// create image and write text into image
		Display display = Picsi.s_shell.getDisplay();
		Image output = new Image(display, outData);
		GC gc = new GC(output);

		// set font
		gc.setForeground(new Color(display, 255, 0, 0)); // red
		gc.setBackground(new Color(display, 255, 255, 255)); // white
		gc.setFont(new Font(display, "Segoe UI", 8, 0));
		
		for (int i=0; i < nResults; i++) {
			final PMResult pm = pq.removeMax();
			
			if (pm != null) {
				int j = 0;
				while(j < results.size() && !pm.m_roi.overlaps(results.get(j).m_roi)) j++;
				if (j == results.size()) {
					final Rectangle r = pm.m_roi.m_rect;

					results.add(pm);

					gc.drawRectangle(r);
					gc.drawText(String.format("%.2f", pm.m_cl), r.x, r.y + r.height, true);
				}
			}
		}
		
		gc.dispose();
		
		outData = output.getImageData();
		output.dispose();
		return outData;
	}
	
}
