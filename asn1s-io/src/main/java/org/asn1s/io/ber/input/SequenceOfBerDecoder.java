////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010-2017. Lapinin "lastrix" Sergey.                          /
//                                                                             /
// Permission is hereby granted, free of charge, to any person                 /
// obtaining a copy of this software and associated documentation              /
// files (the "Software"), to deal in the Software without                     /
// restriction, including without limitation the rights to use,                /
// copy, modify, merge, publish, distribute, sublicense, and/or                /
// sell copies of the Software, and to permit persons to whom the              /
// Software is furnished to do so, subject to the following                    /
// conditions:                                                                 /
//                                                                             /
// The above copyright notice and this permission notice shall be              /
// included in all copies or substantial portions of the Software.             /
//                                                                             /
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,             /
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES             /
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND                    /
// NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT                /
// HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,                /
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING                /
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE                  /
// OR OTHER DEALINGS IN THE SOFTWARE.                                          /
////////////////////////////////////////////////////////////////////////////////

package org.asn1s.io.ber.input;

import org.asn1s.api.exception.Asn1Exception;
import org.asn1s.api.type.CollectionOfType;
import org.asn1s.api.type.ComponentType;
import org.asn1s.api.type.Type.Family;
import org.asn1s.api.value.Value;
import org.asn1s.api.value.ValueFactory;
import org.asn1s.api.value.x680.ValueCollection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

final class SequenceOfBerDecoder implements BerDecoder
{
	@Override
	public Value decode( @NotNull ReaderContext context ) throws IOException, Asn1Exception
	{
		assert context.getType().getFamily() == Family.SEQUENCE_OF;
		assert context.getTag().isConstructed();
		return readComponents( context );
	}

	static Value readComponents( @NotNull ReaderContext ctx ) throws IOException, Asn1Exception
	{
		int ctxLength = ctx.getLength();
		CollectionOfType type = (CollectionOfType)ctx.getType();
		ValueFactory valueFactory = ctx.getValueFactory();
		ComponentType componentType = type.getComponentType();

		boolean isDummy = !componentType.isDummy();
		if( ctxLength == 0 )
			return valueFactory.collection( isDummy );

		int start = ctx.position();
		ValueCollection collection = valueFactory.collection( isDummy );
		ctx.getScope().setValueLevel( collection );
		ctx = ctx.toSiblingContext( componentType );
		boolean indefinite = ctxLength == -1;
		while( indefinite || start + ctxLength > ctx.position() )
		{
			if( ctx.readTagInfoEocPossible( !indefinite ) )
				break;

			Value componentValue = ctx.readInternal( ctx.copy() );
			if( isDummy )
				componentValue = valueFactory.named( componentType.getComponentName(), componentValue );
			collection.add( componentValue );
		}

		ctx.ensureConstructedRead( start, ctxLength, ctx.getTag() );
		return collection;
	}
}
