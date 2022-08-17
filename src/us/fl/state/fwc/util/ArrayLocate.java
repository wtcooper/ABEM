package us.fl.state.fwc.util;

import java.util.Arrays;

public class ArrayLocate {

	

	public static int locate(double[] ja, double val){
		int idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// if -1, then is smaller than smallest value
			if (idx == -1) {
				return 0;
			}


			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= ja.length) {
				return ja.length-1;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;
			if (val < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}

	

	public static int locate(int[] ja, int val){
		int idx = Arrays.binarySearch(ja, val);

		if (idx < 0) {

			// if -1, then is smaller than smallest value
			if (idx == -1) {
				return 0;
			}


			// If not an exact match - determine which value we're closer to
			if (-(idx + 1) >= ja.length) {
				return ja.length-1;
			}

			double spval = (ja[-(idx + 2)] + ja[-(idx + 1)]) / 2d;
			if (val < spval) {
				return -(idx + 2);
			} else {
				return -(idx + 1);
			}
		}

		// Otherwise it's an exact match.
		return idx;
	}

}
