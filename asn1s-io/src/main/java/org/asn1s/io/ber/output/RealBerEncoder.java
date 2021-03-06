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

package org.asn1s.io.ber.output;

import org.asn1s.api.UniversalType;
import org.asn1s.api.encoding.tag.Tag;
import org.asn1s.api.encoding.tag.TagClass;
import org.asn1s.api.type.Type.Family;
import org.asn1s.api.util.NRxUtils;
import org.asn1s.api.value.Value.Kind;
import org.asn1s.api.value.x680.IntegerValue;
import org.asn1s.api.value.x680.RealValue;
import org.asn1s.io.ber.BerUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

@SuppressWarnings( "NumericCastThatLosesPrecision" )
final class RealBerEncoder implements BerEncoder
{
	private static final Tag TAG = new Tag( TagClass.UNIVERSAL, false, UniversalType.REAL.tagNumber() );
	private static final long ZERO_DOUBLE_BITS = Double.doubleToLongBits( 0.0d );
	private static final long NEGATIVE_ZERO_DOUBLE_BITS = Double.doubleToLongBits( -0.0d );

	@Override
	public void encode( @NotNull WriterContext context ) throws IOException
	{
		assert context.getType().getFamily() == Family.REAL;
		assert context.getValue().getKind() == Kind.REAL || context.getValue().getKind() == Kind.INTEGER;
		if( context.getValue().getKind() == Kind.REAL )
			writeRealValue( context );
		else
			writeIntegerValue( context );
	}

	private static void writeRealValue( @NotNull WriterContext context ) throws IOException
	{
		RealValue value = context.getValue().toRealValue();
		if( value.isDouble() )
			writeDouble( context, value.asDouble() );
		else
			writeNR3( context, value.asBigDecimal() );
	}

	private static void writeIntegerValue( WriterContext context ) throws IOException
	{
		IntegerValue integerValue = context.getValue().toIntegerValue();
		if( integerValue.isDouble() )
			writeDouble( context, integerValue.asDouble() );
		else
			writeNR3( context, integerValue.asBigDecimal() );
	}

	private static void writeDouble( WriterContext ctx, double value ) throws IOException
	{
		// encode binary value
		long bits = Double.doubleToLongBits( value );
		if( Double.isInfinite( value ) )
		{
			ctx.writeHeader( TAG, 1 );
			ctx.write( value < 0 ? BerUtils.REAL_NEGATIVE_INF : BerUtils.REAL_POSITIVE_INF );
		}
		else if( Double.isNaN( value ) )
		{
			ctx.writeHeader( TAG, 1 );
			ctx.write( BerUtils.REAL_NAN );
		}
		else if( bits == ZERO_DOUBLE_BITS )
			ctx.writeHeader( TAG, 0 );
		else if( bits == NEGATIVE_ZERO_DOUBLE_BITS )
		{
			ctx.writeHeader( TAG, 1 );
			ctx.write( BerUtils.REAL_MINUS_ZERO );
		}
		else
			new BinaryDoubleEncoder( ctx, bits ).encode();
	}

	private static void writeNR3( WriterContext ctx, BigDecimal bigDecimal ) throws IOException
	{
		String content = NRxUtils.toCanonicalNR3( bigDecimal.toString() );
		byte[] bytes;
		try
		{
			bytes = content.getBytes( "UTF-8" );
		} catch( UnsupportedEncodingException e )
		{
			throw new IllegalStateException( e );
		}
		ctx.writeHeader( TAG, 1 + bytes.length );

		ctx.write( 3 );
		ctx.write( bytes );
	}

	@SuppressWarnings( "MagicNumber" )
	private static final class BinaryDoubleEncoder
	{
		private BinaryDoubleEncoder( WriterContext ctx, long bits )
		{
			this.ctx = ctx;
			sign = ( bits & 0x8000000000000000L ) == 0 ? (byte)0 : BerUtils.REAL_SIGN_MASK;
			mantissa = bits & 0X000FFFFFFFFFFFFFL;
			exponent = ( bits & 0X7FF0000000000000L ) >> 52;
		}

		private final WriterContext ctx;
		private final byte sign;
		private long mantissa;
		private long exponent;

		private void encode() throws IOException
		{
			normalizeMantissaAndExponent();
			byte[] exponentBytes = IntegerBerEncoder.toByteArray( exponent );
			byte[] mantisBytes = IntegerBerEncoder.toByteArray( mantissa );
			byte first = (byte)( BerUtils.REAL_BINARY_FLAG | sign | Math.min( 3, exponentBytes.length - 1 ) );

			ctx.writeHeader( TAG, 1 + exponentBytes.length + mantisBytes.length );
			ctx.write( first );
			if( exponentBytes.length >= 4 )
				ctx.write( exponentBytes.length );
			ctx.write( exponentBytes );
			ctx.write( mantisBytes );
		}

		private void normalizeMantissaAndExponent()
		{
			if( mantissa > 0L )
			{
				mantissa |= 0x0010000000000000L;
				exponent -= 1023L + 52L;
			}
			else
				exponent -= 1022L + 52L;

			if( mantissa > 0 )
				normalizeMantissa();
		}

		private void normalizeMantissa()
		{
			while( ( mantissa & 255L ) == 0L )
			{
				mantissa >>= 8;
				exponent += 8L;
			}

			while( ( mantissa & 1L ) == 0L )
			{
				mantissa >>= 1;
				++exponent;
			}
		}
	}
}
