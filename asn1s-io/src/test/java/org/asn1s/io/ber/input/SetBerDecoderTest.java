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

import org.asn1s.api.Scope;
import org.asn1s.api.UniversalType;
import org.asn1s.api.type.ComponentType;
import org.asn1s.api.type.ComponentType.Kind;
import org.asn1s.api.value.Value;
import org.asn1s.api.value.x680.NamedValue;
import org.asn1s.api.value.x680.ValueCollection;
import org.asn1s.core.module.CoreModule;
import org.asn1s.core.type.x680.collection.SetType;
import org.asn1s.core.value.CoreValueFactory;
import org.asn1s.core.value.x680.IntegerValueInt;
import org.asn1s.core.value.x680.NamedValueImpl;
import org.asn1s.core.value.x680.ValueCollectionImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;

public class SetBerDecoderTest
{
	@Test
	public void testWriteSet_Buffered() throws Exception
	{
		Scope scope = CoreModule.getInstance().createScope();
		SetType type = new SetType( true );
		type.addComponent( Kind.PRIMARY, "a", UniversalType.INTEGER.ref() );
		type.addComponent( Kind.PRIMARY, "b", UniversalType.INTEGER.ref() ).setOptional( true );
		type.setNamespace( "A." );
		type.validate( scope );
		ComponentType componentA = type.getNamedType( "a" );
		Assert.assertNotNull( "No component a", componentA );
		ValueCollection expected = new ValueCollectionImpl( true );
		NamedValue namedValue = new NamedValueImpl( "a", new IntegerValueInt( 0 ) );
		expected.add( namedValue );

		byte[] result = InputUtils.writeValue( scope, type, expected );
		try( ByteArrayInputStream is = new ByteArrayInputStream( result );
		     AbstractBerReader reader = new DefaultBerReader( is, new CoreValueFactory() ) )
		{
			Value value = reader.read( scope, type );
			Assert.assertEquals( "Values are not equal", expected, value );
		}
	}
}
