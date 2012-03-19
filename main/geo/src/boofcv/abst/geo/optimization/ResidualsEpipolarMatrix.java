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

package boofcv.abst.geo.optimization;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.ModelObservationResidual;
import boofcv.numerics.fitting.modelset.ModelCodec;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Computes residual errors for a set of observations from a 3x3 epipolar matrix.
 * Only a single output from the residual function.
 *
 * @author Peter Abeles
 */
public class ResidualsEpipolarMatrix implements FunctionNtoM {
	// converts parameters to and from the fundamental matrix
	protected ModelCodec<DenseMatrix64F> param;
	// list of observations
	protected List<AssociatedPair> obs;
	// error function
	protected ModelObservationResidual<DenseMatrix64F,AssociatedPair> residual;

	// pre-declare temporary storage
	protected DenseMatrix64F F = new DenseMatrix64F(3,3);

	/**
	 * Configures algorithm
	 *
	 * @param param Converts parameters into epipolar matrix
	 * @param residual Function for computing the residuals
	 */
	public ResidualsEpipolarMatrix(ModelCodec<DenseMatrix64F> param,
								   ModelObservationResidual<DenseMatrix64F,AssociatedPair> residual)
	{
		this.param = param == null ? new ModelCodecSwapData(9) : param;
		this.residual = residual;
	}

	public void setObservations( List<AssociatedPair> obs ) {
		this.obs = obs;
	}

	@Override
	public int getN() {
		return param.getParamLength();
	}

	@Override
	public int getM() {
		return obs.size();
	}

	@Override
	public void process(double[] input, double[] output) {
		param.decode(input, F);

		residual.setModel(F);
		for( int i = 0; i < obs.size(); i++ ) {
			AssociatedPair p = obs.get(i);
			output[i] = residual.computeResidual(p);
		}
	}
}
