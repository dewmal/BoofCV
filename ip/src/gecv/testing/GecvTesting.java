/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.testing;

import gecv.core.image.FactorySingleBandImage;
import gecv.core.image.SingleBandImage;
import gecv.struct.image.*;
import sun.awt.image.ByteInterleavedRaster;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Functions to aid in unit testing code for correctly handling sub-images
 *
 * @author Peter Abeles
 */
// todo remove all compare with border functions and use sub-images instead
@SuppressWarnings({"unchecked"})
public class GecvTesting {

	public static <T>T convertToGenericType( Class<?> type ) {
		if( type == ImageSInt8.class || type == ImageUInt8.class )
			return (T)ImageInt8.class;
		if( type == ImageSInt16.class || type == ImageUInt16.class )
			return (T)ImageInt16.class;
		return (T)type;
	}

	/**
	 * If an image is to be created then the generic type can't be used a specific one needs to be.  An arbitrary
	 * specific image type is returned here. 
	 */
	public static <T>T convertGenericToSpecificType( Class<?> type ) {
		if( type == ImageInt8.class )
			return (T)ImageUInt8.class;
		if( type == ImageInt16.class )
			return (T)ImageSInt16.class;
		return (T)type;
	}

	/**
	 * Returns an image which is a sub-image but contains the same values of the input image.  Use for
	 * testing compliance with sub-images.
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageBase> T createSubImageOf(T input) {
		T ret = (T) input._createNew(input.width + 10, input.height + 12);
		ret = (T) ret.subimage(5, 7, input.width + 5, input.height + 7);
		ret.setTo(input);

		return ret;
	}

	/**
	 * Searches for functions that accept only images and makes sure they only accept
	 * images which have he same width and height.
	 *
	 * @param testClass Instance of the class being tested
	 */
	public static void checkImageDimensionValidation(Object testClass, int numFunctions) {
		int count = 0;
		Method methods[] = testClass.getClass().getMethods();

		for (Method m : methods) {
			// see if the inputs are all images
			if (!areAllInputsImages(m))
				continue;

			// test a positive case
			Class<?> params[] = m.getParameterTypes();
			Object[] inputs = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				inputs[i] = createImage(params[i], 10, 20);
			}

			try {
				m.invoke(testClass, inputs);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			// test negative cases
			for (int target = 0; target < params.length; target++) {
				for (int i = 0; i < params.length; i++) {
					if (i != target)
						inputs[i] = createImage(params[i], 10, 20);
					else
						inputs[i] = createImage(params[i], 11, 22);
				}

				try {
					m.invoke(testClass, inputs);
					throw new RuntimeException("Expected an exception here");
				} catch (InvocationTargetException e) {
					if (e.getTargetException().getClass() != IllegalArgumentException.class)
						throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
			count++;
		}

		if (count != numFunctions)
			throw new RuntimeException("Unexpected number of functions");
	}

	private static boolean areAllInputsImages(Method m) {

		Class<?> params[] = m.getParameterTypes();

		if (params.length == 0)
			return false;

		for (Class<?> p : params) {
			if (!ImageBase.class.isAssignableFrom(p)) {
				return false;
			}
		}

		return true;
	}

	public static <T extends ImageBase> T createImage(Class<?> type, int width, int height) {
		type = GecvTesting.convertGenericToSpecificType(type);

		if (type == ImageUInt8.class) {
			return (T)new ImageUInt8(width, height);
		} else if (type == ImageSInt8.class) {
			return (T)new ImageSInt8(width, height);
		} else if (type == ImageSInt16.class) {
			return (T)new ImageSInt16(width, height);
		} else if (type == ImageUInt16.class) {
			return (T)new ImageUInt16(width, height);
		} else if (type == ImageSInt32.class) {
			return (T)new ImageSInt32(width, height);
		} else if (type == ImageFloat32.class) {
			return (T)new ImageFloat32(width, height);
		} else if (type == ImageFloat64.class) {
			return (T)new ImageFloat64(width, height);
		} else if (type == ImageInterleavedInt8.class) {
			return (T)new ImageInterleavedInt8(width, height, 1);
		} else if( type == ImageInteger.class ) {
			// ImageInteger is a generic type, so just create something
			return (T)new ImageSInt32(width,height);
		}
		throw new RuntimeException("Unknown type: "+type.getSimpleName());
	}

	/**
	 * Tests the specified function with the original image provided and with an equivalent
	 * sub-image.  The two results are then compared. The function being tested must only
	 * have one input parameter of type {@link gecv.struct.image.ImageUInt8}.
	 *
	 * @param testClass   Instance of the class that contains the function being tested.
	 * @param function	The name of the function being tested.
	 * @param checkEquals Checks to see if the two images have been modified the same way on output
	 * @param inputParam	The original input parameters
	 */
	// TODO make sure pixels outside are not modified of sub-matrix
	// todo have the submatrices be from different shaped inputs
	@SuppressWarnings({"unchecked"})
	public static void checkSubImage(Object testClass,
									 String function,
									 boolean checkEquals,
									 Object... inputParam) {
		try {
			ImageBase[] larger = new ImageBase[inputParam.length];
			ImageBase[] subImg = new ImageBase[inputParam.length];
			Class<?> paramDesc[] = new Class<?>[inputParam.length];
			Object[] inputModified = new Object[inputParam.length];

			for (int i = 0; i < inputParam.length; i++) {
				if( ImageBase.class.isAssignableFrom(inputParam[i].getClass())) {
					ImageBase<?> img = (ImageBase<?>)inputParam[i];

					// copy the original image inside of a larger image
					larger[i] = img._createNew(img.getWidth() + 10, img.getHeight() + 12);
					// extract a sub-image and make it equivalent to the original image.
					subImg[i] = larger[i].subimage(5, 6, 5 + img.getWidth(), 6 + img.getHeight());
					subImg[i].setTo(img);

				}
				
				// the first time it is called use the original inputs
				inputModified[i] = inputParam[i];
				paramDesc[i] = inputParam[i].getClass();
			}


			// first try it with the original image
			Method m = findMethod(testClass.getClass(), function, paramDesc);

			m.invoke(testClass, inputModified);

			// now try it with the sub-image
			for( int i = 0; i < inputModified.length; i++ ) {
				if( subImg[i] != null )
					inputModified[i] = subImg[i];
			}
			m.invoke(testClass, inputModified);

			// the result should be the identical
			if (checkEquals) {
				for (int i = 0; i < inputParam.length; i++) {
					if( subImg[i] == null )
						continue;
					if( ImageInteger.class.isAssignableFrom(inputParam[i].getClass()))
						assertEquals((ImageInteger) inputModified[i], (ImageInteger) subImg[i], 0);
					else if (inputParam[i] instanceof ImageInterleavedInt8)
						assertEquals((ImageInterleavedInt8) inputParam[i], (ImageInterleavedInt8) subImg[i]);
					else if (inputParam[i] instanceof ImageFloat32)
						assertEquals((ImageFloat32) inputParam[i], (ImageFloat32) subImg[i]);
					else if (inputParam[i] instanceof ImageFloat64)
						assertEquals((ImageFloat64) inputParam[i], (ImageFloat64) subImg[i]);
					else
						throw new RuntimeException("Unknown type " + inputParam[i].getClass().getSimpleName() + ".  Add it here");
				}
			}

		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for a function which is a perfect match.  if none it exists it checks
	 * to see if any matches that could accept an input of the specified type.  If there
	 * is only one such match that is returned.
	 */
	public static Method findMethod(Class<?> type, String name,  Class<?>... params ) {
		Method methods[] = type.getMethods();

		List<Method> found = new ArrayList<Method>();
		for (Method m : methods) {
			if (m.getName().compareTo(name) != 0)
				continue;

			Class<?> a[] = m.getParameterTypes();
			if (a.length != params.length)
				continue;

			boolean match = true;
			for (int i = 0; i < a.length; i++) {
				if (a[i] != params[i]) {
					match = false;
					break;
				}
			}

			if (match) {
				// its a perfect match
				return m;
			}

			// see if it could be called with these parameters
			match = true;
			for (int i = 0; i < a.length; i++) {
				if( params[i] == a[i] )
					continue;
				if ( !a[i].isAssignableFrom(params[i])) {
					match = false;
					break;
				}
			}

			if (match) {
				found.add(m);
			}
		}

		if (found.size() == 1) {
			return found.get(0);
		}

		throw new RuntimeException("Couldn't find matching *public* function to " + name);
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA An image.
	 * @param imgB An image.
	 */
	public static void assertEqualsGeneric(ImageBase imgA, ImageBase imgB, int tolInt, double tolFloat) {

		if( imgA.isInteger() && imgB.isInteger() ) {
			assertEquals((ImageInteger)imgA,(ImageInteger)imgB,tolInt);
		} else if( imgA.isInteger() || imgB.isInteger() ) {
			ImageInteger imgInt = (ImageInteger)(imgA.isInteger() ? imgA : imgB);
			ImageFloat imgFloat = (ImageFloat)(imgA.isInteger() ? imgB : imgA);

			assertEquals(imgInt,imgFloat,tolInt);
		} else {
			assertEquals((ImageFloat) imgA, (ImageFloat) imgB, 0, (float)tolFloat);
		}
	}

	public static void assertEqualsGeneric(ImageBase imgA, ImageBase imgB, int tolInt, double tolFloat,
										   int ignoreBorder ) {

		if (ImageInteger.class.isAssignableFrom(imgA.getClass())) {
			if( ImageInteger.class.isAssignableFrom(imgB.getClass()) ) {
				assertEquals((ImageInteger) imgA, (ImageInteger) imgB, ignoreBorder);
			} else {
				assertEquals((ImageInteger) imgA, (ImageFloat32) imgB, ignoreBorder);
			}
		} else if( ImageInteger.class.isAssignableFrom(imgB.getClass()) ) {
			assertEquals((ImageInteger) imgB, (ImageFloat32) imgA, ignoreBorder);
		} else {
			assertEquals((ImageFloat32) imgA, (ImageFloat32) imgB, ignoreBorder, (float)tolFloat);
		}
	}

	/**
	 * Checks to see if two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA		 An image.
	 * @param imgB		 An image.
	 * @param ignoreBorder
	 */
	public static void assertEquals(ImageInteger imgA, ImageInteger imgB, int ignoreBorder) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") vals " + imgA.get(x, y) + " " + imgB.get(x, y));
			}
		}
	}

	public static void assertEquals(ImageInteger imgA, ImageFloat32 imgB, int ignoreBorder) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (imgA.get(x, y) != (int)imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") vals " + imgA.get(x, y) + " " + (int)imgB.get(x, y));
			}
		}
	}

	public static void assertEquals(ImageInteger imgA, ImageFloat64 imgB, int ignoreBorder) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				if (imgA.get(x, y) != (int)imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") vals " + imgA.get(x, y) + " " + (int)imgB.get(x, y));
			}
		}
	}

	/**
	 * Checks to see if thw two images are equivalent.  Note that this is not the same
	 * as identical since they can be sub-images.
	 *
	 * @param imgA An image.
	 * @param imgB An image.
	 */
	public static void assertEquals(ImageFloat32 imgA, ImageFloat32 imgB) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") " + imgA.get(x, y) + "  " + imgB.get(x, y));
			}
		}
	}

	public static void assertEquals(ImageFloat64 imgA, ImageFloat64 imgB) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				if (imgA.get(x, y) != imgB.get(x, y))
					throw new RuntimeException("values not equal at (" + x + " " + y + ") " + imgA.get(x, y) + "  " + imgB.get(x, y));
			}
		}
	}

	public static void assertEquals( ImageInteger imgA, ImageFloat imgB, int tol) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		SingleBandImage a = FactorySingleBandImage.wrap(imgA);
		SingleBandImage b = FactorySingleBandImage.wrap(imgB);

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				if( Math.abs(a.get(x, y).intValue() - b.get(x, y).intValue()) > tol)
					throw new RuntimeException("values not equal at (" + x + " " + y + ") " + a.get(x, y) + "  " + b.get(x, y));
			}
		}
	}

	public static void assertEquals( ImageFloat imgA, ImageFloat imgB, int ignoreBorder, double tol) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		SingleBandImage a = FactorySingleBandImage.wrap(imgA);
		SingleBandImage b = FactorySingleBandImage.wrap(imgB);

		for (int y = ignoreBorder; y < imgA.getHeight() - ignoreBorder; y++) {
			for (int x = ignoreBorder; x < imgA.getWidth() - ignoreBorder; x++) {
				double normalizer = Math.abs(a.get(x, y).doubleValue()) + Math.abs(b.get(x, y).doubleValue());
				if( normalizer < 1.0 ) normalizer = 1.0;
				if (Math.abs(a.get(x, y).doubleValue() - b.get(x, y).doubleValue())/normalizer > tol)
					throw new RuntimeException("values not equal at (" + x + " " + y + ") " + a.get(x, y) + "  " + b.get(x, y));
			}
		}
	}

	public static void assertEquals(ImageInterleavedInt8 imgA, ImageInterleavedInt8 imgB) {
		if (imgA.getWidth() != imgB.getWidth())
			throw new RuntimeException("Widths are not equals");

		if (imgA.getHeight() != imgB.getHeight())
			throw new RuntimeException("Heights are not equals");

		if (imgA.numBands != imgB.numBands)
			throw new RuntimeException("Number of bands are not equal");

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				for (int k = 0; k < imgA.numBands; k++)
					if (imgA.getBand(x, y, k) != imgB.getBand(x, y, k))
						throw new RuntimeException("value not equal");
			}
		}
	}

	public static void checkEquals(BufferedImage imgA, ImageBase imgB , double tol ) {
		if( ImageUInt8.class == imgB.getClass()) {
			checkEquals(imgA,(ImageUInt8)imgB);
		} else if( ImageFloat32.class == imgB.getClass()) {
			checkEquals(imgA,(ImageFloat32)imgB,(float)tol);
		} else if( ImageInterleavedInt8.class == imgB.getClass()) {
			checkEquals(imgA,(ImageInterleavedInt8)imgB);
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the ImageUInt8
	 *
	 * @param imgA BufferedImage
	 * @param imgB ImageUInt8
	 */
	public static void checkEquals(BufferedImage imgA, ImageUInt8 imgB ) {

		if (imgA.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) imgA.getRaster();

			if (raster.getNumBands() == 1) {
				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						int valB = imgB.get(j, i);
						int valA = raster.getDataStorage()[i * imgA.getWidth() + j];
						if( !imgB.isSigned() )
							valA &= 0xFF;
						
						if (valA != valB)
							throw new RuntimeException("Images are not equal");
					}
				}
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				int gray = (byte) ((((rgb >>> 16) & 0xFF) + ((rgb >>> 8) & 0xFF) + (rgb & 0xFF)) / 3);
				int grayB = imgB.get(x, y);
				if( !imgB.isSigned() )
					gray &= 0xFF;

				if (Math.abs(gray - grayB) != 0) {
					throw new RuntimeException("images are not equal: ("+x+" , "+y+") A = "+gray+" B = "+grayB);
				}
			}
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the ImageUInt8
	 *
	 * @param imgA BufferedImage
	 * @param imgB ImageUInt8
	 */
	public static void checkEquals(BufferedImage imgA, ImageFloat32 imgB, float tol ) {

		if (imgA.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) imgA.getRaster();

			if (raster.getNumBands() == 1) {
				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						float valB = imgB.get(j, i);
						int valA = raster.getDataStorage()[i * imgA.getWidth() + j];
						valA &= 0xFF;

						if (Math.abs(valA - valB) > tol)
							throw new RuntimeException("Images are not equal: A = "+valA+" B = "+valB);
					}
				}
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				float gray = (((rgb >>> 16) & 0xFF) + ((rgb >>> 8) & 0xFF) + (rgb & 0xFF)) / 3.0f;
				float grayB = imgB.get(x, y);

				if (Math.abs(gray - grayB) > tol) {
					throw new RuntimeException("images are not equal: A = "+gray+" B = "+grayB);
				}
			}
		}
	}

	/**
	 * Checks to see if the BufferedImage has the same intensity values as the ImageUInt8
	 *
	 * @param imgA BufferedImage
	 * @param imgB ImageUInt8
	 */
	public static void checkEquals(BufferedImage imgA, ImageInterleavedInt8 imgB) {

		if (imgA.getRaster() instanceof ByteInterleavedRaster) {
			ByteInterleavedRaster raster = (ByteInterleavedRaster) imgA.getRaster();

			if (raster.getNumBands() == 1) {
				// handle a special case where the RGB conversion is screwed
				for (int i = 0; i < imgA.getHeight(); i++) {
					for (int j = 0; j < imgA.getWidth(); j++) {
						byte valB = imgB.getBand(j, i, 0);
						byte valA = raster.getDataStorage()[i * imgA.getWidth() + j];

						if (valA != valB)
							throw new RuntimeException("Images are not equal");
					}
				}
				return;
			}
		}

		for (int y = 0; y < imgA.getHeight(); y++) {
			for (int x = 0; x < imgA.getWidth(); x++) {
				int rgb = imgA.getRGB(x, y);

				int r = (rgb >>> 16) & 0xFF;
				int g = (rgb >>> 8) & 0xFF;
				int b = rgb & 0xFF;

				if (Math.abs(b - imgB.getBand(x, y, 0) & 0xFF) != 0)
					throw new RuntimeException("images are not equal: ");
				if (Math.abs(g - imgB.getBand(x, y, 1) & 0xFF) != 0)
					throw new RuntimeException("images are not equal: ");
				if (Math.abs(r - imgB.getBand(x, y, 2) & 0xFF) != 0)
					throw new RuntimeException("images are not equal: ");
			}
		}
	}

	public static void checkBorderZero(ImageBase outputImage, int border) {
		SingleBandImage img = FactorySingleBandImage.wrap(outputImage);

		for (int y = 0; y < img.getHeight(); y++) {
			if( y >= border && y < img.getHeight()-border )
				continue;

			for (int x = 0; x < img.getWidth(); x++) {
				if( x >= border && x < img.getWidth()-border )
					continue;
				if( img.get(x,y).intValue() != 0 )
					throw new RuntimeException("The border is not zero");
			}
		}
	}

	public static void printDiff( ImageBase imgA , ImageBase imgB ) {

		SingleBandImage a = FactorySingleBandImage.wrap(imgA);
		SingleBandImage b = FactorySingleBandImage.wrap(imgB);

		System.out.println("------- Difference -----------");
		for( int y = 0; y < imgA.getHeight(); y++ ) {
			for( int x = 0; x < imgA.getWidth(); x++ ) {
				double diff = Math.abs(a.get(x,y).doubleValue()-b.get(x,y).doubleValue());
				System.out.printf("%2d ",(int)diff);
			}
			System.out.println();
		}
	}
}