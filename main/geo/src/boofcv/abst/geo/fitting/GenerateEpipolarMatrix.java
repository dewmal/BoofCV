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

package boofcv.abst.geo.fitting;

import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimator1;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Wrapper around {@link boofcv.abst.geo.EpipolarMatrixEstimator} for {@link ModelGenerator}.  Used for robust model
 * fitting with outliers.
 * 
 * @author Peter Abeles
 */
public class GenerateEpipolarMatrix implements ModelGenerator<DenseMatrix64F,AssociatedPair> {

	GeoModelEstimator1<DenseMatrix64F,AssociatedPair> alg;

	public GenerateEpipolarMatrix(GeoModelEstimator1<DenseMatrix64F,AssociatedPair> alg) {
		this.alg = alg;
	}

	@Override
	public DenseMatrix64F createModelInstance() {
		return new DenseMatrix64F(3,3);
	}

	@Override
	public boolean generate(List<AssociatedPair> dataSet, DenseMatrix64F model ) {
		if( alg.process(dataSet,model) ) {
			return true;
		}
		return false;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
