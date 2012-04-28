/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity;

import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestSelectRectBasicWta_S32_U8 extends BasicDisparitySelectRectTests<ImageUInt8> {

	public TestSelectRectBasicWta_S32_U8() {
		super(ImageUInt8.class);
	}

	@Override
	public DisparitySelectRect_S32<ImageUInt8> createAlg() {
		return new SelectRectBasicWta_S32_U8();
	}
}
