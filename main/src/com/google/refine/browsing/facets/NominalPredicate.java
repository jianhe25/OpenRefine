/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.browsing.facets;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.DecoratedPredicate;
import org.json.JSONException;
import org.json.JSONWriter;

import java.util.Properties;

/**
 * Store a facet choice that has a decorated value, a count of matched rows,
 * and a flag of whether it has been selected.
 */
public class NominalPredicate implements Jsonizable {
    final public DecoratedPredicate decoratedPredicate;
    public int                    count;
    public boolean                selected;

    public NominalPredicate(DecoratedPredicate decoratedPredict) {
        this.decoratedPredicate = decoratedPredict;
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("v"); decoratedPredicate.write(writer, options);
        writer.key("c"); writer.value(count);
        writer.key("s"); writer.value(selected);
        writer.endObject();
    }
}
