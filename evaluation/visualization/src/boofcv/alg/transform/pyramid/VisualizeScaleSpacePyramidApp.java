/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.transform.pyramid;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.gui.image.ImagePyramidPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidFloat;

import java.awt.image.BufferedImage;

/**
 * Displays an image pyramid.
 *
 * @author Peter Abeles
 */
// TODO abstract and add integer
public class VisualizeScaleSpacePyramidApp {

	public static void main( String args[] ) {
		double scales[] = new double[]{1,1.2,2.4,3.6,4.8,6.0,10,15,20};

		BufferedImage buffered = UtilImageIO.loadImage("../data/evaluation/standard/boat.png");

		PyramidFloat<ImageFloat32> pyramid = FactoryPyramid.scaleSpacePyramid(scales, ImageFloat32.class);

		ImageFloat32 input = ConvertBufferedImage.convertFrom(buffered,(ImageFloat32)null);

		pyramid.process(input);

		ImagePyramidPanel<ImageFloat32> gui = new ImagePyramidPanel<ImageFloat32>(pyramid,true);
		gui.render();

		ShowImages.showWindow(gui,"Image Pyramid");
	}
}
