/** *****************************************************************************
  * Copyright (c) 2011 Dipanjan Das
  * Language Technologies Institute,
  * Carnegie Mellon University,
  * All Rights Reserved.
  *
  * SpanAndCorrespondingFeatures.java is part of SEMAFOR 2.0.
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
  * *****************************************************************************/
package edu.cmu.cs.lti.ark.fn.parsing

import java.util

import com.google.common.base.Objects
import java.io.Serializable

import edu.cmu.cs.lti.ark.util.ds.Range0Based

case class SpanAndFeatures(span: Range0Based, features: Array[Int])
    extends Serializable with Comparable[SpanAndFeatures] {
  def compareTo(o: SpanAndFeatures): Int = {
    val span1 = span.start + ":" + span.end
    val span2 = o.span.end + ":" + o.span.end
    span1.compareTo(span2)
  }

  override def equals(that: Any): Boolean = {
    if (that == null || !that.isInstanceOf[SpanAndFeatures]) return false
    val other: SpanAndFeatures = that.asInstanceOf[SpanAndFeatures]
    span.equals(other.span) && util.Arrays.equals(features, other.features)
  }

  override def toString: String = {
    Objects.toStringHelper(this)
        .add("span", span)
        .add("features", util.Arrays.toString(features)).toString
  }
}
