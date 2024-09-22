package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MenuItem;

import imageprocessing.Cropping;
import imageprocessing.ParticleAnalyzer;
import imageprocessing.bayerPattern.DebayeringConverter;
import imageprocessing.binary.HoughTransform;
import imageprocessing.binary.MorphologicFilter;
import imageprocessing.binary.OtsuThresholdConverter;
import imageprocessing.colors.ChannelRGBA;
import imageprocessing.colors.DitheringConverter;
import imageprocessing.colors.GrayScaleConverter;
import imageprocessing.colors.Inverter;
import imageprocessing.transformations.Rotator;

/**
 * Image processing menu
 * @author Christoph Stamm
 *
 */
public class ImageMenu extends UserMenu {    
    /**
     * Registration of image operations
     * @param item menu item
     * @param views twin view
     * @param mru MRU
     */
    public ImageMenu(MenuItem item, TwinView views, MRU mru) {
        super(item, views, mru);

        // add(menuText, shortcut, instanceOfIImageProcessor)
        add("C&ropping\tCtrl+R",                 SWT.CTRL | 'R', new Cropping());
        add("&Invert\tF1",                       SWT.F1,         new Inverter());
        add("Grayscale",                         SWT.F2,         new GrayScaleConverter());
        add("Dithering",                         SWT.F3,         new DitheringConverter());
        add("Rotator",                           SWT.F4,         new Rotator());

        add("Otsu Threshold",                    SWT.F5,         new OtsuThresholdConverter());
        add("Particle Analyzer",                 SWT.F6,         new ParticleAnalyzer());
        add("Morphologic Filter",                SWT.F7,         new MorphologicFilter());
        add("Hough Transform",                   SWT.F8,         new HoughTransform());

        var bayerPatternMenu = addMenu("Bayern Pattern");
        bayerPatternMenu.add("Debayering",       SWT.F9,         new DebayeringConverter());

        UserMenu channels = addMenu("Channel");
        channels.add("R\tCtrl+1",                SWT.CTRL | '1', new ChannelRGBA(0));
        channels.add("G\tCtrl+2",                SWT.CTRL | '2', new ChannelRGBA(1));
        channels.add("B\tCtrl+3",                SWT.CTRL | '3', new ChannelRGBA(2));
        channels.add("A\tCtrl+4",                SWT.CTRL | '4', new ChannelRGBA(3));

        // TODO add here further image processing entries (they are inserted into the Image menu)
    }
}
