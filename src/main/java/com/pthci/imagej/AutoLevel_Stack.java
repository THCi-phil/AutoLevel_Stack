/* MIT license, with no advertising clause added
 * Copyright (c) 2023 Prof Phil Threlfall-Holmes, TH Collaborative Innovation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * 
 * Except as contained in this notice, the name of Prof Phil Threlfall-Holmes TH Collaborative Innovation
 * either in whole or in part shall not be used in advertising or otherwise to promote the sale,
 * use or other dealings in this Software
 * without prior written authorization from Prof Phil Threlfall-Holmes TH Collaborative Innovation.
 * 
 */

package com.pthci.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

/**
 * Resets Display Min And Max from min and max pixels in the entire stack,
 * not just one slice as per built-in method
 * also here min is set default before loop as bit-level max, (c.f. in-built method set as 0)
 * so the true min pixel greyscale in the stack is found: in-built method will always find 0.
 *
 * @author Phil Threlfall-Holmes
 */
public class AutoLevel_Stack implements PlugInFilter {
	protected ImagePlus image;

	// image property members
	private int width   ;
	private int height  ;
	private int type    ;
	private int nSlices ;
	private int sizePixelArray ;
	
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	} //end public int setup(String arg, ImagePlus imp)
	//-----------------------------------------------------


	@Override
	public void run(ImageProcessor ip) {
		width   = ip.getWidth();    //in pixel units
		height  = ip.getHeight();
		type    = image.getType();
		nSlices = image.getStackSize();
		sizePixelArray = width*height;
		process(image);
		image.updateAndDraw();
	} //end public void run(ImageProcessor ip)
	//-----------------------------------------------------


	/**
	 * Process an image.
	 * <p>
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 * </p>
	 * <p>
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 * </p>
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public void process(ImagePlus image) {
		if      (type == ImagePlus.GRAY8    ) autoLevel_8bit();
		else if (type == ImagePlus.GRAY16   ) autoLevel_16bit();
		else if (type == ImagePlus.GRAY32   ) autoLevel_32bit();
		else if (type == ImagePlus.COLOR_RGB) autoLevel_RGB();
		else {
			throw new RuntimeException("not supported");
		}
	} //end public void process(ImagePlus image) 
	//-----------------------------------------------------


	public void autoLevel_8bit() {
		int minGreyscale = 255 ; //set as the max possible, update with each value lower
		int maxGreyscale =   0 ; //set as the min possible, update with each value larger
		int testedPixelValue ;
		
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= nSlices; i++) {
			byte[] pixels = (byte[]) image.getStack().getProcessor(i).getPixels();
			//pixels = ip.getPixels() is a 1-D array, not a 2D array as you would intuit, so pixels[x+y*width] instead of pixels[x,y]
			//here as per Invert_Image we can just pixelPos++ through the array for maximum speed
			//
			//Images are 8-bit (unsigned, i.e. values between 0 and 255).
			//Java has no data type for unsigned 8-bit integers: the byte type is signed, so we have to use the & 0xff dance
			//(a Boolean AND operation) to make sure that the value is treated as unsigned integer,
			for( int pixelPos=0; pixelPos<sizePixelArray; pixelPos++ ) {
				testedPixelValue = pixels[pixelPos] & 0xff ;
				if( testedPixelValue < minGreyscale ) minGreyscale = testedPixelValue ;
				if( testedPixelValue > maxGreyscale ) maxGreyscale = testedPixelValue ;
			}  //end for min-max scan
		} //end for slice
		image.setDisplayRange( minGreyscale, maxGreyscale );
	} //end public void autoLevel_8bit
	//-----------------------------------------------------


	public void autoLevel_16bit() {
		int minGreyscale = 65535 ; //set as the max possible, update with each value lower
		int maxGreyscale =     0 ; //set as the min possible, update with each value larger
		int testedPixelValue ;
		
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= nSlices; i++) {
			short[] pixels = (short[]) image.getStack().getProcessor(i).getPixels();
			//Java short is 16 bit signed, so -32,768 to 32,767
			//Java int is 32 bit, signed -2,147,483,648 to 2,147,483,647
			//so we can safely promote to int type for tested pixed value
			for( int pixelPos=0; pixelPos<sizePixelArray; pixelPos++ ) {
				testedPixelValue = pixels[pixelPos] & 0x00ffff ;
				if( testedPixelValue < minGreyscale ) minGreyscale = testedPixelValue ;
				if( testedPixelValue > maxGreyscale ) maxGreyscale = testedPixelValue ;
			}  //end for min-max scan
		} //end for slice
		image.setDisplayRange( minGreyscale, maxGreyscale );
	} //end public void autoLevel_16bit
	//-----------------------------------------------------
	
	
	public void autoLevel_32bit() {
		float minGreyscale = (float)1.0 ; //set as the max possible, update with each value lower
		float maxGreyscale = (float)0.0 ; //set as the min possible, update with each value larger
		float testedPixelValue ;
		
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= nSlices; i++) {
			float[] pixels = (float[]) image.getStack().getProcessor(i).getPixels();
			//Java short is 16 bit signed, so -32,768 to 32,767
			//Java int is 32 bit, signed -2,147,483,648 to 2,147,483,647
			//so we can safely promote to int type for tested pixed value
			for( int pixelPos=0; pixelPos<sizePixelArray; pixelPos++ ) {
				testedPixelValue = pixels[pixelPos] ;
				if( testedPixelValue < minGreyscale ) minGreyscale = testedPixelValue ;
				if( testedPixelValue > maxGreyscale ) maxGreyscale = testedPixelValue ;
			}  //end for min-max scan
		} //end for slice
		image.setDisplayRange( minGreyscale, maxGreyscale );
	} //end public void autoLevel_8bit
	//-----------------------------------------------------


	// processing of COLOR_RGB images
	public void autoLevel_RGB() {
		int minRed   = 255 ; //set as the max possible, update with each value lower
		int minGreen = 255 ; 
		int minBlue  = 255 ; 
		int maxRed   =   0 ; //set as the max possible, update with each value lower
		int maxGreen =   0 ; 
		int maxBlue  =   0 ; 
		int testedPixelValue ;
		
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= nSlices; i++) {
			int[] pixels = (int[]) image.getStack().getProcessor(i).getPixels();
			ColorProcessor cp = new ColorProcessor(width, height, pixels);
			byte[] R = new byte[ sizePixelArray ];
			byte[] G = new byte[ sizePixelArray ];
			byte[] B = new byte[ sizePixelArray ];
			cp.getRGB( R, G, B);
			for( int pixelPos=0; pixelPos<sizePixelArray; pixelPos++ ) {
				testedPixelValue = R[pixelPos] & 0xff ;
				if( testedPixelValue < minRed   ) minRed   = testedPixelValue ;
				if( testedPixelValue > maxRed   ) maxRed   = testedPixelValue ;
				testedPixelValue = G[pixelPos] & 0xff ;
				if( testedPixelValue < minGreen ) minGreen = testedPixelValue ;
				if( testedPixelValue > maxGreen ) maxGreen = testedPixelValue ;
				testedPixelValue = B[pixelPos] & 0xff ;
				if( testedPixelValue < minBlue  ) minBlue  = testedPixelValue ;
				if( testedPixelValue > maxBlue  ) maxBlue  = testedPixelValue ;
			}  //end for min-max scan
		} //end for slice
		image.setDisplayRange( minRed  , maxRed  , 4 );
		image.setDisplayRange( minGreen, maxGreen, 2 );
		image.setDisplayRange( minBlue , maxBlue , 1 );
	} //end public void autoLevel_RGB()
  //-----------------------------------------------------


	

/*=================================================================================*/


	public void showAbout() {
		IJ.showMessage("AutoLevel Stack",
			"setDisplayRange to min and max pixel value in whole stack"
		);
	} //end public void showAbout()
  //-----------------------------------------------------


/*=================================================================================*/

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = AutoLevel_Stack.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());

		// start ImageJ
		new ImageJ();

		ImagePlus image = IJ.openImage("d:/20221221 TP1501 phantom 2 - ruler slice1 16bit example.tif");
		//ImagePlus image = IJ.openImage("d:/vertical spray test_bkgndRmvd.tif");
		//ImagePlus image = IJ.openImage("d:/test16bitBandWinvert.tif");
		//ImagePlus image = IJ.openImage("d:/test32bitBandWinvert.tif");
		//magePlus image = IJ.openImage("d:/testRGB.tif");
		
		// open the Clown sample
		//ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}  //end public static void main(String[] args)
  
/*=================================================================================*/
  
}  //end public class AutoLevel_Stack
//========================================================================================
//                         end public class AutoLevel_Stack
//========================================================================================