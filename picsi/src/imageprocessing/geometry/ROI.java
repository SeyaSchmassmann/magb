package imageprocessing.geometry;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Region of Interest (ROI)
 * 
 * @author Christoph Stamm
 *
 */
public class ROI {
	public ImageData m_imageData;
	public Rectangle m_rect;
	
	/**
	 * Create new ROI
	 * @param imageData image data
	 * @param roi region of interest
	 */
	public ROI(ImageData imageData, Rectangle roi) {
        m_imageData = imageData;
        m_rect = roi;
	}
	
	/**
	 * @return width of ROI
	 */
	public int getWidth() {
		return m_rect.width;
	}
	
	/**
	 * @return height of ROI
	 */
	public int getHeight() {
		return m_rect.height;
	}
	
	/**
	 * Get pixel at position (x,y)
	 * @param x x-coordinate in ROI coordinate system
	 * @param y y-coordinate in ROI coordinate system
	 * @return
	 */
	public int getPixel(int x, int y) {
        return m_imageData.getPixel(m_rect.x + x, m_rect.y + y);
	}

	/**
	 * Set pixel at position (x,y)
	 * @param x x-coordinate in ROI coordinate system
	 * @param y y-coordinate in ROI coordinate system
	 * @param val
	 */
	public void setPixel(int x, int y, int val) {
        m_imageData.setPixel(m_rect.x + x, m_rect.y + y, val);
	}
	
	/**
	 * Returns true if this ROI overlaps with r
	 * @param r another ROI
	 * @return
	 */
	public boolean overlaps(ROI r) {
        return m_rect.intersects(r.m_rect);
	}

}
