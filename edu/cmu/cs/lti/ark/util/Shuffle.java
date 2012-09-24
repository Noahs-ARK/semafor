/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Shuffle.java is part of SEMAFOR 2.0.
 * 
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.util;



public class Shuffle
{
    private static java.util.Random rand = new java.util.Random();
    // construct a random permutation of the ints 0 .. n-1
    // represented as an int array of length n
    //
    public static int[] permutation(int n){

        assert n > 0;
        //intitial element order is irrelevant so long as each int 1..n occurs exactly once
        //inorder initialization assures that is the case

        int[] sample = new int[n];
        for (int k = 0; k < sample.length; k++) {
            sample[k] = k;
        }
        //loop invariant: the tail of the sample array is randomized.
        //Intitally the tail is empty; at each step move a random
        //element from front of array into the tail, then decrement boundary of tail
        int last = sample.length-1;   //last is maximal index of elements not in the tail

        while (last > 0){
            // Select random index in range 0..last, and swap its contents with those at last
            // The null swap is allowed; it should be possible that sample[k] does not change
            swap(rand.nextInt(last+1), last, sample);
            last -= 1;
        }
       return sample;
    }

    // swap the elements at indices j and k
    // j and k need not be distinct, allowing for null swaps
    //
    private static void swap(int j, int k, int[] array){
          int temp = array[k];
          array[k] = array[j];
          array[j] = temp;
    }
}
